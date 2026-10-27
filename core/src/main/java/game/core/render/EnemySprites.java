package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dessine les yōkai ordinaires avec leurs feuilles de sprites animées : l'oni bleu (Rôdeur), la
 * lanterne hantée (Archer) et la roue de feu (Chargeur).
 */
public final class EnemySprites implements Disposable {

    private static final float IDLE_FRAME = 0.14f;
    private static final float MOVE_FRAME = 0.09f;
    /** Durée du flash blanc après un coup reçu, en secondes. */
    private static final float HURT_FLASH = 0.1f;
    /** Opacité du flash blanc, ajouté en lumière par-dessus l'image. */
    private static final float FLASH_ALPHA = 0.65f;
    /** Durée d'une phase de clignotement pendant l'élan du Chargeur, en secondes. */
    private static final float WIND_UP_BLINK = 0.08f;
    /** Déplacement minimal entre deux images pour considérer qu'un ennemi marche, en unités. */
    private static final float MOVE_EPSILON = 0.05f;
    /**
     * Temps pendant lequel un ennemi reste « en marche » après son dernier mouvement, en secondes.
     */
    private static final float WALK_GRACE = 0.12f;
    /** Écart horizontal minimal avec le joueur pour se retourner, en unités. */
    private static final float FACE_DEAD_ZONE = 4f;
    /** Écart entre les bords de l'oni et du joueur en dessous duquel l'oni griffe, en unités. */
    private static final float SWIPE_REACH = 6f;
    /** Durée d'un coup de griffe ou d'un crachat, en secondes. */
    private static final float ACTION_DURATION = 0.24f;
    /** Distance maximale entre une lanterne et un projectile neuf pour dire qu'elle l'a craché. */
    private static final float SPIT_DETECT = 20f;
    /**
     * Décalage de l'ombre de la lanterne sous sa position, en unités : la lanterne est dessinée la
     * bouche à hauteur de son centre (les braises sortent bien de sa bouche), et son ombre descend
     * d'autant pour qu'elle paraisse flotter.
     */
    public static final float LANTERN_SHADOW_DROP = 8f;
    /** Petit relèvement de la lanterne pour que sa bouche tombe sur son centre, en unités. */
    private static final float LANTERN_LIFT = 1f;

    /** Ce que la vue retient d'un ennemi d'une image à l'autre. */
    private static final class View {
        private Vec2 lastPosition;
        private int lastHp;
        private float flashLeft;
        private float time;
        private float walkLeft;
        private boolean facingLeft;
        private float actionLeft;
    }

    /** Les animations d'un type d'ennemi. */
    private record Sheet(int cell, float emptyBelow, float lift, Animation<TextureRegion> idle,
            Animation<TextureRegion> move, Animation<TextureRegion> action, TextureRegion hurt) {
    }

    private final Texture prowlerTexture = load("prowler");
    private final Texture archerTexture = load("archer");
    private final Texture chargerTexture = load("charger");
    private final Sheet prowler;
    private final Sheet archer;
    private final Sheet charger;
    private final Animation<TextureRegion> chargerWindUp;
    private final Animation<TextureRegion> chargerCharge;
    private final Animation<TextureRegion> chargerStunned;
    private final Map<Enemy, View> views = new IdentityHashMap<>();
    private final Set<Projectile> knownProjectiles = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Charge les feuilles et prépare les animations. */
    public EnemySprites() {
        // Oni bleu : repos, course, griffe, touché.
        TextureRegion[][] p = TextureRegion.split(prowlerTexture, 48, 48);
        prowler = new Sheet(48, 0f, 0f, loop(p[0], 0, 4, IDLE_FRAME), loop(p[1], 0, 6, MOVE_FRAME),
                once(p[2], 0, 4, ACTION_DURATION / 4), p[3][1]);
        // Lanterne : flotte, avance, crache (sans l'image d'élan : on ne voit le tir qu'au départ), touchée.
        TextureRegion[][] a = TextureRegion.split(archerTexture, 48, 48);
        archer = new Sheet(48, 12f, LANTERN_LIFT, loop(a[0], 0, 6, IDLE_FRAME), loop(a[1], 0, 6, IDLE_FRAME),
                once(a[2], 1, 3, ACTION_DURATION / 3), a[3][1]);
        // Roue de feu : repos, roule, élan, charge, sonnée, touchée.
        TextureRegion[][] c = TextureRegion.split(chargerTexture, 64, 64);
        charger = new Sheet(64, 8f, 0f, loop(c[0], 0, 4, IDLE_FRAME), loop(c[1], 0, 6, MOVE_FRAME), null, c[5][1]);
        chargerWindUp = loop(c[2], 0, 4, 0.06f);
        chargerCharge = loop(c[3], 0, 4, 0.05f);
        chargerStunned = loop(c[4], 0, 6, IDLE_FRAME);
    }

    private static Texture load(String name) {
        Texture texture = new Texture(Gdx.files.internal("sprites/" + name + ".png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static Animation<TextureRegion> loop(TextureRegion[] row, int first, int frames, float duration) {
        Animation<TextureRegion> animation = new Animation<>(duration, slice(row, first, frames));
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    private static Animation<TextureRegion> once(TextureRegion[] row, int first, int frames, float duration) {
        Animation<TextureRegion> animation = new Animation<>(duration, slice(row, first, frames));
        animation.setPlayMode(Animation.PlayMode.NORMAL);
        return animation;
    }

    private static TextureRegion[] slice(TextureRegion[] row, int first, int frames) {
        TextureRegion[] regions = new TextureRegion[frames];
        System.arraycopy(row, first, regions, 0, frames);
        return regions;
    }

    /**
     * Met à jour ce que la vue sait de chaque ennemi : s'il marche, s'il vient d'être touché, s'il
     * griffe ou crache, de quel côté il regarde.
     *
     * @param enemies les ennemis encore en vie
     * @param projectiles les projectiles en vol, pour voir quelle lanterne vient de cracher
     * @param player la position du joueur
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(List<Enemy> enemies, List<Projectile> projectiles, Vec2 player, float delta) {
        Set<Enemy> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Enemy enemy : enemies) {
            alive.add(enemy);
            View view = views.computeIfAbsent(enemy, e -> newView(e));
            Vec2 p = enemy.position();
            float dx = p.x() - view.lastPosition.x();
            boolean moved = p.distanceTo(view.lastPosition) > MOVE_EPSILON;
            view.walkLeft = moved ? WALK_GRACE : Math.max(0f, view.walkLeft - delta);
            if (enemy.hp() < view.lastHp) {
                view.flashLeft = HURT_FLASH;
            } else {
                view.flashLeft = Math.max(0f, view.flashLeft - delta);
            }
            view.actionLeft = Math.max(0f, view.actionLeft - delta);
            if (enemy instanceof Charger c && c.isCharging()) {
                if (Math.abs(dx) > MOVE_EPSILON) {
                    view.facingLeft = dx < 0f;
                }
            } else if (!(enemy instanceof Charger c2 && c2.isStunned())
                    && Math.abs(player.x() - p.x()) > FACE_DEAD_ZONE) {
                view.facingLeft = player.x() < p.x();
            }
            if (!(enemy instanceof Archer) && !(enemy instanceof Charger) && view.actionLeft <= 0f) {
                float gap = p.distanceTo(player) - enemy.hitbox().radius() - Player.RADIUS;
                if (gap < SWIPE_REACH) {
                    view.actionLeft = ACTION_DURATION;
                }
            }
            view.lastPosition = p;
            view.lastHp = enemy.hp();
            view.time += delta;
        }
        views.keySet().retainAll(alive);
        detectSpits(projectiles);
    }

    private static View newView(Enemy enemy) {
        View view = new View();
        view.lastPosition = enemy.position();
        view.lastHp = enemy.hp();
        return view;
    }

    // Un projectile ennemi qui n'existait pas à l'image d'avant, juste à côté d'une lanterne :
    // c'est elle qui vient de le cracher.
    private void detectSpits(List<Projectile> projectiles) {
        for (Projectile projectile : projectiles) {
            if (projectile.faction() != Faction.ENEMY || knownProjectiles.contains(projectile)) {
                continue;
            }
            for (Map.Entry<Enemy, View> entry : views.entrySet()) {
                if (entry.getKey() instanceof Archer
                        && entry.getKey().position().distanceTo(projectile.position()) < SPIT_DETECT) {
                    entry.getValue().actionLeft = ACTION_DURATION;
                    break;
                }
            }
        }
        knownProjectiles.clear();
        knownProjectiles.addAll(projectiles);
    }

    /**
     * Dessine un ennemi. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param enemy l'ennemi
     * @param position où le dessiner (sa position lissée, voir {@link MotionSmoothing})
     */
    public void draw(SpriteBatch batch, Enemy enemy, Vec2 position) {
        View view = views.get(enemy);
        float time = view == null ? 0f : view.time;
        Sheet sheet = sheetOf(enemy);
        boolean flashing = view != null && view.flashLeft > 0f;
        boolean blink = false;
        TextureRegion frame;
        if (enemy instanceof Charger c && c.isWindingUp()) {
            // L'élan clignote : c'est l'annonce de la charge.
            blink = (int) (c.phaseTime() / WIND_UP_BLINK) % 2 == 0;
            frame = chargerWindUp.getKeyFrame(time);
        } else if (enemy instanceof Charger c && c.isCharging()) {
            frame = chargerCharge.getKeyFrame(time);
        } else if (enemy instanceof Charger c && c.isStunned()) {
            frame = chargerStunned.getKeyFrame(time);
        } else if (view != null && view.actionLeft > 0f && sheet.action() != null) {
            frame = sheet.action().getKeyFrame(ACTION_DURATION - view.actionLeft);
        } else if (flashing) {
            frame = sheet.hurt();
        } else {
            boolean moving = view != null && view.walkLeft > 0f;
            frame = (moving ? sheet.move() : sheet.idle()).getKeyFrame(time);
        }
        boolean left = view != null && view.facingLeft;
        drawFrame(batch, frame, enemy, position, sheet, left);
        if (flashing || blink) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 1f, 1f, flashing ? FLASH_ALPHA : FLASH_ALPHA * 0.6f);
            drawFrame(batch, frame, enemy, position, sheet, left);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static void drawFrame(SpriteBatch batch, TextureRegion frame, Enemy enemy, Vec2 p, Sheet sheet,
            boolean left) {
        float size = sheet.cell();
        float x = p.x() - size / 2f;
        float y = p.y() - enemy.hitbox().radius() - sheet.emptyBelow() + sheet.lift();
        if (left) {
            batch.draw(frame, x + size, y, -size, size);
        } else {
            batch.draw(frame, x, y, size, size);
        }
    }

    private Sheet sheetOf(Enemy enemy) {
        if (enemy instanceof Charger) {
            return charger;
        }
        if (enemy instanceof Archer) {
            return archer;
        }
        return prowler;
    }

    @Override
    public void dispose() {
        prowlerTexture.dispose();
        archerTexture.dispose();
        chargerTexture.dispose();
    }
}
