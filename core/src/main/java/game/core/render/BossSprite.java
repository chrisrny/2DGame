package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.core.render.BossPoseTracker.Pose;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import game.model.entity.Enemy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dessine l'Oni du quartier, le boss, avec sa feuille de sprites ({@code assets/sprites/boss.png}).
 *
 * <p>Comme le héros, il est dessiné de 3/4 et ne fait que se retourner gauche/droite. Sa pose est
 * choisie par {@link BossPoseTracker}. La massue levée suit l'avancement de l'annonce du coup :
 * le geste arrive en haut pile quand le coup part.</p>
 *
 * <p>La feuille a 8 lignes par phase (repos, marche, massue levée, impact, élan, charge, sonné,
 * touché), la phase 2 juste sous la phase 1, puis une ligne pour la mort. Le modèle retire le boss
 * mort de la liste des ennemis : la vue garde sa dernière position pour jouer la mort à sa place.</p>
 */
public final class BossSprite implements Disposable {

    /** Taille d'une case de la feuille, en pixels (et en unités du monde). */
    public static final int CELL = 128;

    /** Durée d'une image de l'animation de mort, en secondes. */
    public static final float DEATH_FRAME = 0.2f;
    private static final int DEATH_FRAMES = 6;

    // Dans une case, le corps est centré sur la colonne 64 et il reste 1 pixel vide sous les pieds.
    private static final float BODY_CENTER_X = 64f;
    private static final float EMPTY_BELOW_FEET = 1f;

    private static final int ROWS_PER_PHASE = 8;
    private static final int ROW_DEATH = 16;
    private static final int SLAM_RAISE_FRAMES = 4;

    /** Opacité du flash blanc d'un coup reçu, ajouté par-dessus l'image. */
    private static final float FLASH_ALPHA = 0.65f;

    private final Texture texture;
    private final List<Map<Pose, Animation<TextureRegion>>> phases;
    private final List<TextureRegion[]> slamRaise;
    private final Animation<TextureRegion> death;
    private final BossPoseTracker tracker = new BossPoseTracker();
    private boolean secondPhase;
    private boolean alive;
    private boolean diedNow;
    private Vec2 deathPosition;
    private float deathTime;

    /** Charge la feuille et prépare les animations des deux phases. */
    public BossSprite() {
        texture = new Texture(Gdx.files.internal("sprites/boss.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(texture, CELL, CELL);
        phases = List.of(animations(grid, 0), animations(grid, ROWS_PER_PHASE));
        slamRaise = List.of(frames(grid[2], SLAM_RAISE_FRAMES), frames(grid[ROWS_PER_PHASE + 2], SLAM_RAISE_FRAMES));
        death = new Animation<>(DEATH_FRAME, frames(grid[ROW_DEATH], DEATH_FRAMES));
        death.setPlayMode(Animation.PlayMode.NORMAL);
    }

    private static Map<Pose, Animation<TextureRegion>> animations(TextureRegion[][] grid, int first) {
        Map<Pose, Animation<TextureRegion>> map = new EnumMap<>(Pose.class);
        map.put(Pose.IDLE, animation(grid[first], 6, 0.14f, Animation.PlayMode.LOOP));
        map.put(Pose.WALK, animation(grid[first + 1], 8, 0.11f, Animation.PlayMode.LOOP));
        map.put(Pose.SLAM_IMPACT, animation(grid[first + 3], 4, BossPoseTracker.IMPACT_DURATION / 4,
                Animation.PlayMode.NORMAL));
        map.put(Pose.CHARGE_WIND_UP, animation(grid[first + 4], 4, 0.08f, Animation.PlayMode.LOOP));
        map.put(Pose.CHARGE, animation(grid[first + 5], 4, 0.07f, Animation.PlayMode.LOOP));
        map.put(Pose.STUNNED, animation(grid[first + 6], 6, 0.15f, Animation.PlayMode.LOOP));
        map.put(Pose.FLINCH, animation(grid[first + 7], 2, BossPoseTracker.FLINCH_DURATION / 2,
                Animation.PlayMode.NORMAL));
        return map;
    }

    private static TextureRegion[] frames(TextureRegion[] row, int count) {
        TextureRegion[] regions = new TextureRegion[count];
        System.arraycopy(row, 0, regions, 0, count);
        return regions;
    }

    private static Animation<TextureRegion> animation(TextureRegion[] row, int count, float duration,
            Animation.PlayMode mode) {
        Animation<TextureRegion> animation = new Animation<>(duration, frames(row, count));
        animation.setPlayMode(mode);
        return animation;
    }

    /**
     * Suit le boss s'il est là, et lance sa mort quand il disparaît de la liste des ennemis.
     * À appeler une fois par image tant que la partie avance.
     *
     * @param enemies les ennemis encore en vie
     * @param player  la position du joueur
     * @param delta   temps écoulé depuis l'image précédente, en secondes
     */
    public void update(List<Enemy> enemies, Vec2 player, float delta) {
        diedNow = false;
        Optional<Boss> boss = enemies.stream().filter(Boss.class::isInstance).map(Boss.class::cast).findFirst();
        if (boss.isPresent()) {
            tracker.update(boss.get(), player, delta);
            secondPhase = boss.get().phaseNumber() >= 2;
            alive = true;
            return;
        }
        if (alive) {
            // Il vient de mourir : on joue sa mort là où il était.
            alive = false;
            diedNow = true;
            deathPosition = tracker.position();
            deathTime = 0f;
        } else if (deathPosition != null) {
            deathTime += delta;
        }
    }

    /** @return le suivi de pose, pour les effets (impact, passage de phase) */
    public BossPoseTracker tracker() {
        return tracker;
    }

    /** @return {@code true} si le boss vient de mourir à cette image */
    public boolean diedNow() {
        return diedNow;
    }

    /** @return la position où jouer la mort, si elle est en cours */
    public Optional<Vec2> dying() {
        if (alive || deathPosition == null || deathTime >= DEATH_FRAME * DEATH_FRAMES) {
            return Optional.empty();
        }
        return Optional.of(deathPosition);
    }

    /**
     * Dessine le boss vivant. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param boss  le boss
     */
    public void draw(SpriteBatch batch, Boss boss) {
        TextureRegion frame = currentFrame();
        drawFrame(batch, frame, boss.position(), boss.hitbox().radius());
        if (tracker.flashing()) {
            // Flash blanc : la même image ajoutée en lumière, le geste reste lisible.
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 1f, 1f, FLASH_ALPHA);
            drawFrame(batch, frame, boss.position(), boss.hitbox().radius());
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * Dessine la mort en cours, si elle l'est. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     */
    public void drawDeath(SpriteBatch batch) {
        dying().ifPresent(position -> drawFrame(batch, death.getKeyFrame(deathTime), position, Boss.RADIUS));
    }

    private TextureRegion currentFrame() {
        int phase = secondPhase ? 1 : 0;
        Pose pose = tracker.pose();
        if (pose == Pose.SLAM_RAISE) {
            int index = Math.min(SLAM_RAISE_FRAMES - 1, (int) (tracker.slamProgress() * SLAM_RAISE_FRAMES));
            return slamRaise.get(phase)[index];
        }
        return phases.get(phase).get(pose).getKeyFrame(tracker.poseTime());
    }

    private void drawFrame(SpriteBatch batch, TextureRegion frame, Vec2 center, float radius) {
        float x = center.x() - BODY_CENTER_X;
        float y = center.y() - radius - EMPTY_BELOW_FEET;
        if (tracker.facingLeft()) {
            batch.draw(frame, x + CELL, y, -CELL, CELL);
        } else {
            batch.draw(frame, x, y, CELL, CELL);
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
