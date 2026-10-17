package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Boss;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.event.EventBus;
import game.model.world.GameMap;
import game.model.world.Run;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dessine la scène en fausse 3D (vue 3/4, façon SNES) : sol, ombres, murs et personnages triés
 * par profondeur, puis lueurs.
 *
 * <p><b>Tri par profondeur</b> : chaque élément a une profondeur, le y de ses pieds (le bas
 * de l'emprise pour un mur). On dessine du plus haut à l'écran au plus bas : ce qui est plus
 * bas passe devant. Le joueur disparaît donc derrière un mur quand il est au nord de lui.</p>
 *
 * <p>Tout se lit dans le modèle à chaque image ; la vue ne décide d'aucune règle.</p>
 */
public final class SceneRenderer implements Disposable {

    // Dans fx.png : ombre (0), lueur (16), flèche (32), lame (48), pixel blanc (62, 0).
    private static final int FX_SHADOW = 0;
    private static final int FX_GLOW = 16;
    private static final int FX_ARROW = 32;
    private static final int FX_BLADE = 48;
    private static final int FX = 16;

    /** Un élément à dessiner, avec sa profondeur. */
    private record Item(float depth, Runnable draw) {
    }

    private final SpriteBatch batch = new SpriteBatch();
    private final Texture fx;
    private final MapRenderer map;
    private final EnemySprites enemies = new EnemySprites();
    private final PlayerSprite player = new PlayerSprite();
    private final SlashEffect slash = new SlashEffect();
    private final BossSprite boss = new BossSprite();
    private final BossEffects bossEffects;
    private final MotionSmoothing smoothing = new MotionSmoothing();
    private final DeathPuffs puffs;
    private final LightingRenderer lighting;
    private final AmbientEffects ambient = new AmbientEffects();
    private final TextureRegion white;
    private final List<Item> items = new ArrayList<>();
    private final List<Light> frameLights = new ArrayList<>();
    private final float viewW;
    private final float viewH;
    private float time;

    /**
     * Charge les sprites de la scène.
     *
     * @param events le bus de la partie, pour les nuages de mort
     * @param viewW  largeur de la vue, en unités du monde
     * @param viewH  hauteur de la vue, en unités du monde
     */
    public SceneRenderer(EventBus events, float viewW, float viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
        fx = new Texture(Gdx.files.internal("sprites/fx.png"));
        fx.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new MapRenderer(fx);
        puffs = new DeathPuffs(events);
        bossEffects = new BossEffects(fx);
        lighting = new LightingRenderer(viewW, viewH);
        white = new TextureRegion(fx, 62, 0, 1, 1);
    }

    /**
     * Note les positions avant un pas du modèle, pour lisser les déplacements à l'écran. À appeler
     * juste avant chaque pas.
     *
     * @param run la partie
     */
    public void beforeStep(Run run) {
        smoothing.beforeStep(run);
    }

    /**
     * Règle l'avancement vers le prochain pas du modèle, une fois par image après les pas.
     *
     * @param progress temps en attente dans l'horloge divisé par la durée d'un pas, de 0 à 1
     */
    public void setStepProgress(float progress) {
        smoothing.setProgress(progress);
    }

    /**
     * Position lissée du joueur, celle où il est dessiné : la caméra la suit.
     *
     * @param run la partie
     * @return la position à l'écran du joueur
     */
    public Vec2 playerOnScreen(Run run) {
        return smoothing.of(run.player(), run.player().position());
    }

    /**
     * Fait avancer les animations. À appeler une fois par image tant que la partie avance.
     *
     * @param run   la partie
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Run run, float delta) {
        player.update(run.player(), delta);
        slash.update(run.player(), delta);
        enemies.update(run.map().enemies(), run.map().projectiles(), run.player().position(), delta);
        boss.update(run.map().enemies(), run.player().position(), delta);
        BossPoseTracker pose = boss.tracker();
        pose.impactNow().ifPresent(bossEffects::slamImpact);
        if (pose.phaseChangedNow()) {
            bossEffects.phaseChange(pose.position());
        }
        if (boss.diedNow()) {
            bossEffects.death(pose.position());
        }
        bossEffects.update(delta);
        puffs.update(delta);
        time += delta;
        ambient.update(smoothing.of(run.player(), run.player().position()), viewW, viewH, map.fireSpots(), delta);
    }

    /**
     * Dessine le sol et l'ombre des murs : tout ce qui est sous les personnages.
     *
     * @param camera la caméra du monde
     * @param run    la partie
     * @param viewW  largeur visible, en unités
     * @param viewH  hauteur visible, en unités
     */
    public void drawGround(OrthographicCamera camera, Run run, float viewW, float viewH) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.drawGround(batch, run.map().arena(), run.map().walls(), camera, viewW, viewH, time);
        bossEffects.drawGround(batch);
        batch.end();
    }

    /**
     * Dessine les ombres, les murs et les personnages triés par profondeur. La nuit, les lueurs et
     * les braises viennent ensuite, avec {@link #drawLight}.
     *
     * @param camera      la caméra du monde
     * @param run         la partie
     * @param playerAlpha opacité du joueur (clignotement d'invincibilité)
     */
    public void drawObjects(OrthographicCamera camera, Run run, float playerAlpha) {
        GameMap world = run.map();
        Player hero = run.player();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Vec2 heroPos = smoothing.of(hero, hero.position());
        drawShadows(world, heroPos);

        items.clear();
        for (Rect wall : world.walls()) {
            items.add(new Item(wall.y(), () -> map.drawWall(batch, wall, heroPos, time)));
        }
        for (Enemy enemy : world.enemies()) {
            Vec2 pos = smoothing.of(enemy, enemy.position());
            float feet = pos.y() - enemy.hitbox().radius();
            if (enemy instanceof Boss b) {
                items.add(new Item(feet, () -> boss.draw(batch, b, pos)));
            } else {
                items.add(new Item(feet, () -> enemies.draw(batch, enemy, pos)));
            }
        }
        float heroFeet = heroPos.y() - Player.RADIUS;
        items.add(new Item(heroFeet, () -> player.draw(batch, hero, heroPos, playerAlpha)));
        if (slash.visible()) {
            // Coup vers le haut : juste derrière le héros ; sinon juste devant.
            float slashDepth = slash.behindHero() ? heroFeet + 0.01f : heroFeet - 0.01f;
            items.add(new Item(slashDepth, () -> slash.draw(batch, heroPos)));
        }
        for (Projectile projectile : world.projectiles()) {
            Vec2 pos = smoothing.of(projectile, projectile.position());
            items.add(new Item(pos.y() - 4f, () -> drawProjectile(projectile, pos)));
        }
        boss.dying().ifPresent(position ->
                items.add(new Item(position.y() - Boss.RADIUS, () -> boss.drawDeath(batch))));
        for (DeathPuffs.Puff puff : puffs.puffs()) {
            items.add(new Item(puff.position().y() - 8f, () -> puffs.draw(batch, puff)));
        }
        // Du plus haut au plus bas : ce qui est plus bas à l'écran est dessiné après, donc devant.
        items.sort(Comparator.comparingDouble(Item::depth).reversed());
        for (Item item : items) {
            item.draw().run();
        }

        batch.end();
    }

    /**
     * Pose la nuit et ses lumières sur la scène, puis ce qui doit rester vif : fumée, lueurs des
     * projectiles et du boss, onde de choc, braises. À appeler juste après {@link #drawObjects}.
     *
     * @param camera   la caméra du monde
     * @param viewport la vue de l'écran de jeu
     * @param run      la partie
     */
    public void drawLight(OrthographicCamera camera, Viewport viewport, Run run) {
        GameMap world = run.map();
        Player hero = run.player();
        collectLights(world, hero);
        lighting.apply(batch, camera, viewport, frameLights, time);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        ambient.drawSmoke(batch, lighting.smoke());
        drawGlows(world, hero);
        ambient.drawEmbers(batch, white);
        batch.end();
    }

    // Les lumières de l'image : celles du décor, puis tout ce qui bouge et brille.
    private void collectLights(GameMap world, Player hero) {
        frameLights.clear();
        frameLights.addAll(map.lights());
        Vec2 h = smoothing.of(hero, hero.position());
        // Le héros porte une lumière froide et douce : on le voit toujours, même loin des feux.
        frameLights.add(Light.steady(h.x(), h.y() + 10f, 80f, 0.75f, 0.72f, 0.95f, 0.6f));
        if (slash.visible()) {
            frameLights.add(Light.steady(h.x(), h.y() + 6f, 46f, 1f, 0.85f, 0.55f, 0.6f));
        }
        for (Projectile projectile : world.projectiles()) {
            Vec2 p = smoothing.of(projectile, projectile.position());
            if (projectile.faction() == Faction.PLAYER) {
                frameLights.add(Light.steady(p.x(), p.y(), 26f, 0.45f, 1f, 0.95f, 0.6f));
            } else {
                frameLights.add(Light.flame(p.x(), p.y(), 30f, 0.8f));
            }
        }
        for (Enemy enemy : world.enemies()) {
            Vec2 p = smoothing.of(enemy, enemy.position());
            if (enemy instanceof Boss b) {
                frameLights.add(Light.flame(p.x(), p.y() + 30f, 120f, b.phaseNumber() >= 2 ? 1f : 0.7f));
            } else if (enemy instanceof Archer) {
                frameLights.add(Light.steady(p.x(), p.y() + 10f, 44f, 1f, 0.6f, 0.3f, 0.75f));
            } else if (enemy instanceof Charger) {
                frameLights.add(Light.flame(p.x(), p.y() + 8f, 50f, 0.9f));
            }
        }
        boss.dying().ifPresent(p -> frameLights.add(Light.flame(p.x(), p.y() + 30f, 120f, 1f)));
        for (Circle zone : hero.fireZones()) {
            frameLights.add(Light.flame(zone.center().x(), zone.center().y(), zone.radius() * 2.2f, 0.8f));
        }
    }

    private void drawShadows(GameMap world, Vec2 heroPos) {
        shadow(heroPos, Player.RADIUS, Player.RADIUS);
        for (Enemy enemy : world.enemies()) {
            float r = enemy.hitbox().radius();
            // La lanterne flotte : son ombre est plus bas qu'elle.
            float drop = enemy instanceof Archer ? EnemySprites.LANTERN_SHADOW_DROP : 0f;
            shadow(smoothing.of(enemy, enemy.position()), r, r + drop);
        }
        for (Projectile projectile : world.projectiles()) {
            shadow(smoothing.of(projectile, projectile.position()).add(new Vec2(0f, -6f)), 3f, 0f);
        }
    }

    // Ellipse sombre sous les pieds, un peu plus large que la zone de collision.
    private void shadow(Vec2 center, float radius, float feetOffset) {
        float w = radius * 2f + 4f;
        float h = Math.max(4f, radius * 0.9f);
        batch.draw(fx, center.x() - w / 2f, center.y() - feetOffset - h / 2f, w, h, FX_SHADOW, 4, FX, 8, false, false);
    }

    private void drawProjectile(Projectile projectile, Vec2 p) {
        Vec2 v = projectile.velocity();
        float angle = (float) Math.toDegrees(Math.atan2(v.y(), v.x()));
        int src = projectile.faction() == Faction.PLAYER ? FX_BLADE : FX_ARROW;
        batch.draw(fx, p.x() - FX / 2f, p.y() - FX / 2f, FX / 2f, FX / 2f, FX, FX, 1f, 1f, angle,
                src, 0, FX, FX, false, false);
    }

    // Lueurs additives : la lumière s'ajoute à ce qu'il y a dessous au lieu de le recouvrir.
    private void drawGlows(GameMap world, Player hero) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Projectile projectile : world.projectiles()) {
            if (projectile.faction() == Faction.PLAYER) {
                glow(smoothing.of(projectile, projectile.position()), 18f, 0.25f, 0.95f, 0.95f, 0.55f);
            } else {
                glow(smoothing.of(projectile, projectile.position()), 12f, 0.5f, 1f, 0.7f, 0.3f);
            }
        }
        for (Circle zone : hero.fireZones()) {
            glow(zone.center(), zone.radius() * 3.2f, 1f, 0.42f, 0.12f, 0.45f);
        }
        for (Enemy enemy : world.enemies()) {
            if (enemy instanceof Boss b) {
                float strength = b.phaseNumber() >= 2 ? 0.55f : 0.3f;
                glow(smoothing.of(b, b.position()).add(new Vec2(0f, 22f)), 70f, 1f, 0.35f, 0.1f, strength);
            }
        }
        bossEffects.drawGlows(batch);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Décalage de caméra du tremblement d'écran en cours (coup de massue du boss, passage de
     * phase, mort du boss).
     *
     * @return le décalage à ajouter à la position de la caméra, en unités
     */
    public Vec2 cameraShake() {
        return bossEffects.shakeOffset();
    }

    private void glow(Vec2 center, float size, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(fx, center.x() - size / 2f, center.y() - size / 2f, size, size, FX_GLOW, 0, FX, FX, false, false);
    }

    @Override
    public void dispose() {
        batch.dispose();
        fx.dispose();
        map.dispose();
        enemies.dispose();
        player.dispose();
        boss.dispose();
        bossEffects.dispose();
        lighting.dispose();
        slash.dispose();
        puffs.dispose();
    }
}
