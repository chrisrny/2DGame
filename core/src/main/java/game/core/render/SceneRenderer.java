package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Boss;
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
    private final DeathPuffs puffs;
    private final List<Item> items = new ArrayList<>();

    /**
     * Charge les sprites de la scène.
     *
     * @param events le bus de la partie, pour les nuages de mort
     */
    public SceneRenderer(EventBus events) {
        fx = new Texture(Gdx.files.internal("sprites/fx.png"));
        fx.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        map = new MapRenderer(fx);
        puffs = new DeathPuffs(events);
    }

    /**
     * Fait avancer les animations. À appeler une fois par image tant que la partie avance.
     *
     * @param run   la partie
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Run run, float delta) {
        player.update(run.player().stateKind(), delta);
        enemies.update(run.map().enemies(), delta);
        puffs.update(delta);
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
        map.drawGround(batch, run.map().arena(), run.map().walls(), camera, viewW, viewH);
        batch.end();
    }

    /**
     * Dessine les ombres, les murs et les personnages triés par profondeur, puis les lueurs.
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
        drawShadows(world, hero);

        items.clear();
        for (Rect wall : world.walls()) {
            items.add(new Item(wall.y(), () -> map.drawWall(batch, wall)));
        }
        for (Enemy enemy : world.enemies()) {
            float feet = enemy.position().y() - enemy.hitbox().radius();
            items.add(new Item(feet, () -> enemies.draw(batch, enemy, hero.position())));
        }
        items.add(new Item(hero.position().y() - Player.RADIUS, () -> player.draw(batch, hero, playerAlpha)));
        for (Projectile projectile : world.projectiles()) {
            items.add(new Item(projectile.position().y() - 4f, () -> drawProjectile(projectile)));
        }
        for (DeathPuffs.Puff puff : puffs.puffs()) {
            items.add(new Item(puff.position().y() - 8f, () -> puffs.draw(batch, puff)));
        }
        // Du plus haut au plus bas : ce qui est plus bas à l'écran est dessiné après, donc devant.
        items.sort(Comparator.comparingDouble(Item::depth).reversed());
        for (Item item : items) {
            item.draw().run();
        }

        drawGlows(world, hero);
        batch.end();
    }

    private void drawShadows(GameMap world, Player hero) {
        shadow(hero.position(), Player.RADIUS, Player.RADIUS);
        for (Enemy enemy : world.enemies()) {
            float r = enemy.hitbox().radius();
            shadow(enemy.position(), r, r);
        }
        for (Projectile projectile : world.projectiles()) {
            shadow(projectile.position().add(new Vec2(0f, -6f)), 3f, 0f);
        }
    }

    // Ellipse sombre sous les pieds, un peu plus large que la zone de collision.
    private void shadow(Vec2 center, float radius, float feetOffset) {
        float w = radius * 2f + 4f;
        float h = Math.max(4f, radius * 0.9f);
        batch.draw(fx, center.x() - w / 2f, center.y() - feetOffset - h / 2f, w, h, FX_SHADOW, 4, FX, 8, false, false);
    }

    private void drawProjectile(Projectile projectile) {
        Vec2 p = projectile.position();
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
                glow(projectile.position(), 18f, 0.25f, 0.95f, 0.95f, 0.55f);
            } else {
                glow(projectile.position(), 12f, 0.5f, 1f, 0.7f, 0.3f);
            }
        }
        for (Circle zone : hero.fireZones()) {
            glow(zone.center(), zone.radius() * 3.2f, 1f, 0.42f, 0.12f, 0.45f);
        }
        for (Enemy enemy : world.enemies()) {
            if (enemy instanceof Boss b) {
                float strength = b.phaseNumber() >= 2 ? 0.55f : 0.3f;
                glow(b.position().add(new Vec2(0f, 22f)), 70f, 1f, 0.35f, 0.1f, strength);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
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
        puffs.dispose();
    }
}
