package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dessine le joueur avec sa feuille de sprites animée ({@code assets/sprites/hero.png}).
 *
 * <p>Chaque état du joueur ({@link PlayerStateKind}, pattern State dans le modèle) a son
 * animation : la vue lit l'état courant et joue l'animation qui lui correspond. Le temps de
 * l'animation repart de zéro à chaque changement d'état. Les durées des animations d'attaque et
 * de coup reçu valent exactement celles du modèle ({@link Player#ATTACK_DURATION},
 * {@link Player#HURT_DURATION}) : l'animation finit pile quand l'état change.</p>
 *
 * <p>La feuille est une grille de cases de {@link #CELL} pixels, une animation par ligne, le
 * personnage regardant vers la droite : il est retourné quand le joueur vise à gauche.</p>
 */
public final class PlayerSprite implements Disposable {

    /** Taille d'une case de la feuille, en pixels (et en unités du monde). */
    public static final int CELL = 32;

    /** Durée pendant laquelle la pose de lancer s'affiche après une compétence, en secondes. */
    public static final float CAST_POSE_DURATION = 0.18f;

    // Dans une case, le corps est centré sur la colonne 16 et les pieds touchent la ligne 29
    // (en partant du haut) : il reste 2 pixels vides sous les pieds.
    private static final float BODY_CENTER_X = 16f;
    private static final float EMPTY_BELOW_FEET = 2f;

    // Lignes de la feuille, dans l'ordre.
    private static final int ROW_IDLE = 0;
    private static final int ROW_RUN = 1;
    private static final int ROW_ATTACK = 2;
    private static final int ROW_DODGE = 3;
    private static final int ROW_HURT = 4;
    private static final int ROW_DEATH = 5;
    private static final int ROW_CAST = 6;

    private final Texture texture;
    private final Map<PlayerStateKind, Animation<TextureRegion>> animations = new EnumMap<>(PlayerStateKind.class);
    private final Animation<TextureRegion> cast;
    private PlayerStateKind shownKind;
    private float stateTime;

    /** Charge la feuille de sprites et prépare les animations. */
    public PlayerSprite() {
        texture = new Texture(Gdx.files.internal("sprites/hero.png"));
        // Pixel art : pas de lissage, chaque pixel reste net à l'agrandissement.
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(texture, CELL, CELL);
        animations.put(PlayerStateKind.IDLE, animation(grid, ROW_IDLE, 6, 0.12f, Animation.PlayMode.LOOP));
        animations.put(PlayerStateKind.MOVING, animation(grid, ROW_RUN, 6, 0.08f, Animation.PlayMode.LOOP));
        animations.put(PlayerStateKind.ATTACKING,
                animation(grid, ROW_ATTACK, 4, Player.ATTACK_DURATION / 4, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.DODGING, animation(grid, ROW_DODGE, 3, 0.07f, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.HURT,
                animation(grid, ROW_HURT, 2, Player.HURT_DURATION / 2, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.DEAD, animation(grid, ROW_DEATH, 4, 0.2f, Animation.PlayMode.NORMAL));
        cast = animation(grid, ROW_CAST, 3, CAST_POSE_DURATION / 3, Animation.PlayMode.NORMAL);
    }

    private static Animation<TextureRegion> animation(TextureRegion[][] grid, int row, int frames,
            float frameDuration, Animation.PlayMode mode) {
        TextureRegion[] regions = new TextureRegion[frames];
        System.arraycopy(grid[row], 0, regions, 0, frames);
        Animation<TextureRegion> animation = new Animation<>(frameDuration, regions);
        animation.setPlayMode(mode);
        return animation;
    }

    /**
     * Fait avancer l'animation. À appeler une fois par image tant que la partie avance (pas
     * pendant la pause ni le choix de relique, où tout reste figé).
     *
     * @param kind  l'état actuel du joueur
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(PlayerStateKind kind, float delta) {
        if (kind != shownKind) {
            shownKind = kind;
            stateTime = 0f;
        } else {
            stateTime += delta;
        }
    }

    /**
     * Dessine le joueur. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch  le pinceau d'images, réglé sur la caméra du monde
     * @param player le joueur à dessiner
     * @param alpha  opacité, de 0 à 1 (pour le clignotement d'invincibilité)
     */
    public void draw(SpriteBatch batch, Player player, float alpha) {
        TextureRegion frame = currentFrame(player);
        Vec2 center = player.position();
        float x = center.x() - BODY_CENTER_X;
        float y = center.y() - Player.RADIUS - EMPTY_BELOW_FEET;
        batch.setColor(1f, 1f, 1f, alpha);
        if (player.facing().x() < 0f) {
            // Largeur négative : l'image est retournée autour de son bord droit.
            batch.draw(frame, x + CELL, y, -CELL, CELL);
        } else {
            batch.draw(frame, x, y, CELL, CELL);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // Juste après une compétence, au repos ou en marchant, on montre la pose de lancer.
    private TextureRegion currentFrame(Player player) {
        PlayerStateKind kind = player.stateKind();
        if (kind == PlayerStateKind.IDLE || kind == PlayerStateKind.MOVING) {
            float sinceSkill = Math.min(player.skill1().timeSinceActivation(),
                    player.skill2().map(skill -> skill.timeSinceActivation()).orElse(Float.POSITIVE_INFINITY));
            if (sinceSkill < CAST_POSE_DURATION) {
                return cast.getKeyFrame(sinceSkill);
            }
        }
        return animations.get(kind).getKeyFrame(shownKind == kind ? stateTime : 0f);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
