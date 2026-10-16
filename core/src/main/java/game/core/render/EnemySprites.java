package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Boss;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dessine les ennemis avec leurs feuilles de sprites animées.
 *
 * <p>Le modèle ne dit pas si un ennemi marche ou vient d'être touché : la vue le déduit en
 * comparant chaque image à la précédente (la position a bougé, les points de vie ont baissé).
 * Ces petits états de vue sont gardés ici, par ennemi, et oubliés quand l'ennemi disparaît.
 * Les ennemis regardent toujours vers le joueur.</p>
 */
public final class EnemySprites implements Disposable {

    private static final float IDLE_FRAME = 0.15f;
    private static final float MOVE_FRAME = 0.1f;
    /** Durée du flash blanc après un coup reçu, en secondes. */
    private static final float HURT_FLASH = 0.1f;
    /** Durée d'une phase de clignotement pendant l'annonce du Chargeur, en secondes. */
    private static final float WIND_UP_BLINK = 0.08f;
    /** Déplacement minimal entre deux images pour considérer qu'un ennemi marche, en unités. */
    private static final float MOVE_EPSILON = 0.05f;

    /** Ce que la vue retient d'un ennemi d'une image à l'autre. */
    private static final class View {
        private Vec2 lastPosition;
        private int lastHp;
        private float flashLeft;
        private float time;
        private boolean moving;
    }

    /** Les animations d'un type d'ennemi. */
    private record Sheet(int cell, float feetPad, Animation<TextureRegion> idle, Animation<TextureRegion> move,
            TextureRegion hurt) {
    }

    private final Texture prowlerTexture = load("prowler");
    private final Texture archerTexture = load("archer");
    private final Texture chargerTexture = load("charger");
    private final Texture bossTexture = load("boss");
    private final Sheet prowler;
    private final Sheet archer;
    private final Sheet charger;
    private final Sheet boss;
    private final Sheet bossSecondPhase;
    private final Animation<TextureRegion> chargerWindUp;
    private final Animation<TextureRegion> chargerCharge;
    private final Animation<TextureRegion> chargerStunned;
    private final Map<Enemy, View> views = new IdentityHashMap<>();

    /** Charge les feuilles et prépare les animations. */
    public EnemySprites() {
        prowler = sheet(prowlerTexture, 32, 2f, 0, 1, 2);
        archer = sheet(archerTexture, 32, 1f, 0, 1, 2);
        charger = sheet(chargerTexture, 32, 1f, 0, 1, 2);
        boss = sheet(bossTexture, 64, 1f, 0, 1, 2);
        bossSecondPhase = sheet(bossTexture, 64, 1f, 3, 4, 2);
        TextureRegion[][] grid = TextureRegion.split(chargerTexture, 32, 32);
        chargerWindUp = loop(grid[3], 2, IDLE_FRAME);
        chargerCharge = loop(grid[4], 2, MOVE_FRAME);
        chargerStunned = loop(grid[5], 4, IDLE_FRAME);
    }

    private static Texture load(String name) {
        Texture texture = new Texture(Gdx.files.internal("sprites/" + name + ".png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static Sheet sheet(Texture texture, int cell, float feetPad, int idleRow, int moveRow, int hurtRow) {
        TextureRegion[][] grid = TextureRegion.split(texture, cell, cell);
        return new Sheet(cell, feetPad, loop(grid[idleRow], 4, IDLE_FRAME), loop(grid[moveRow], 4, MOVE_FRAME),
                grid[hurtRow][0]);
    }

    private static Animation<TextureRegion> loop(TextureRegion[] row, int frames, float duration) {
        TextureRegion[] regions = new TextureRegion[frames];
        System.arraycopy(row, 0, regions, 0, frames);
        Animation<TextureRegion> animation = new Animation<>(duration, regions);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    /**
     * Met à jour ce que la vue sait de chaque ennemi : s'il marche, s'il vient d'être touché.
     * À appeler une fois par image tant que la partie avance.
     *
     * @param enemies les ennemis encore en vie
     * @param delta   temps écoulé depuis l'image précédente, en secondes
     */
    public void update(List<Enemy> enemies, float delta) {
        Set<Enemy> alive = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Enemy enemy : enemies) {
            alive.add(enemy);
            View view = views.get(enemy);
            if (view == null) {
                view = new View();
                view.lastPosition = enemy.position();
                view.lastHp = enemy.hp();
                views.put(enemy, view);
            }
            view.moving = enemy.position().distanceTo(view.lastPosition) > MOVE_EPSILON;
            if (enemy.hp() < view.lastHp) {
                view.flashLeft = HURT_FLASH;
            } else {
                view.flashLeft = Math.max(0f, view.flashLeft - delta);
            }
            view.lastPosition = enemy.position();
            view.lastHp = enemy.hp();
            view.time += delta;
        }
        views.keySet().retainAll(alive);
    }

    /**
     * Dessine un ennemi, tourné vers le joueur. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch  le pinceau d'images
     * @param enemy  l'ennemi
     * @param target la position du joueur, vers qui l'ennemi regarde
     */
    public void draw(SpriteBatch batch, Enemy enemy, Vec2 target) {
        View view = views.get(enemy);
        float time = view == null ? 0f : view.time;
        Sheet sheet = sheetOf(enemy);
        TextureRegion frame;
        if (view != null && view.flashLeft > 0f) {
            frame = sheet.hurt();
        } else if (enemy instanceof Charger c && c.isWindingUp()) {
            boolean blink = (int) (c.phaseTime() / WIND_UP_BLINK) % 2 == 0;
            frame = blink ? sheet.hurt() : chargerWindUp.getKeyFrame(time);
        } else if (enemy instanceof Charger c && c.isCharging()) {
            frame = chargerCharge.getKeyFrame(time);
        } else if (enemy instanceof Charger c && c.isStunned()) {
            frame = chargerStunned.getKeyFrame(time);
        } else {
            boolean moving = view != null && view.moving;
            frame = (moving ? sheet.move() : sheet.idle()).getKeyFrame(time);
        }
        Vec2 p = enemy.position();
        float size = sheet.cell();
        float x = p.x() - size / 2f;
        float y = p.y() - enemy.hitbox().radius() - sheet.feetPad();
        if (target.x() < p.x()) {
            batch.draw(frame, x + size, y, -size, size);
        } else {
            batch.draw(frame, x, y, size, size);
        }
    }

    private Sheet sheetOf(Enemy enemy) {
        if (enemy instanceof Boss b) {
            return b.phaseNumber() >= 2 ? bossSecondPhase : boss;
        }
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
        bossTexture.dispose();
    }
}
