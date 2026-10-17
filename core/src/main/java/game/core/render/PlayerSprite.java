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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dessine le joueur avec sa feuille de sprites animée ({@code assets/sprites/hero.png}).
 *
 * <p>Le héros est toujours dessiné de 3/4, comme dans les jeux d'action en « quarter-view » :
 * il se retourne seulement vers la gauche ou la droite selon la visée. La direction exacte du
 * coup est montrée à part, par {@link SlashEffect}.</p>
 *
 * <ul>
 *   <li><b>L'état du joueur</b> ({@link PlayerStateKind}, pattern State dans le modèle) choisit
 *   l'animation. Le temps de l'animation repart de zéro à chaque changement d'état. Les durées
 *   d'attaque et de coup reçu valent celles du modèle ({@link Player#ATTACK_DURATION},
 *   {@link Player#HURT_DURATION}) : l'animation finit pile quand l'état change.</li>
 *   <li><b>Le sens</b> suit la visée, avec une zone morte : quand on vise presque à la
 *   verticale, il garde son sens au lieu de clignoter.</li>
 *   <li><b>Pendant l'esquive</b>, deux images fantômes suivent le joueur.</li>
 * </ul>
 *
 * <p>La feuille est une grille de cases de {@link #CELL} pixels, une animation par ligne :
 * repos, course, attaque, esquive, coup reçu, lancer, mort.</p>
 */
public final class PlayerSprite implements Disposable {

    /** Taille d'une case de la feuille, en pixels (et en unités du monde). */
    public static final int CELL = 48;

    /** Durée pendant laquelle la pose de lancer s'affiche après une compétence, en secondes. */
    public static final float CAST_POSE_DURATION = 0.24f;

    /** En dessous de cette part horizontale de la visée, le héros garde son sens. */
    private static final float FLIP_DEAD_ZONE = 0.2f;

    /** Écart entre deux images fantômes de l'esquive, en secondes. */
    private static final float GHOST_INTERVAL = 0.04f;

    /** Opacité des images fantômes, de la plus ancienne à la plus récente. */
    private static final float[] GHOST_ALPHAS = {0.18f, 0.35f};

    // Dans une case, le corps est centré sur la colonne 24 et les pieds touchent la ligne 46
    // (en partant du haut) : il reste 1 pixel vide sous les pieds.
    private static final float BODY_CENTER_X = 24f;
    private static final float EMPTY_BELOW_FEET = 1f;

    private static final int ROW_IDLE = 0;
    private static final int ROW_RUN = 1;
    private static final int ROW_ATTACK = 2;
    private static final int ROW_DODGE = 3;
    private static final int ROW_HURT = 4;
    private static final int ROW_CAST = 5;
    private static final int ROW_DEATH = 6;

    /** Une image fantôme : où était le joueur, quelle image il montrait et dans quel sens. */
    private record Ghost(Vec2 position, TextureRegion frame, boolean left) {
    }

    private final Texture texture;
    private final Map<PlayerStateKind, Animation<TextureRegion>> animations = new EnumMap<>(PlayerStateKind.class);
    private final Animation<TextureRegion> cast;
    private final Deque<Ghost> ghosts = new ArrayDeque<>();
    private PlayerStateKind shownKind;
    private float stateTime;
    private float ghostTimer;
    private boolean facingLeft;

    /** Charge la feuille de sprites et prépare les animations. */
    public PlayerSprite() {
        texture = new Texture(Gdx.files.internal("sprites/hero.png"));
        // Pixel art : pas de lissage, chaque pixel reste net à l'agrandissement.
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(texture, CELL, CELL);
        animations.put(PlayerStateKind.IDLE, animation(grid, ROW_IDLE, 6, 0.12f, Animation.PlayMode.LOOP));
        animations.put(PlayerStateKind.MOVING, animation(grid, ROW_RUN, 8, 0.075f, Animation.PlayMode.LOOP));
        animations.put(PlayerStateKind.ATTACKING,
                animation(grid, ROW_ATTACK, 5, Player.ATTACK_DURATION / 5, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.DODGING, animation(grid, ROW_DODGE, 4, 0.05f, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.HURT,
                animation(grid, ROW_HURT, 2, Player.HURT_DURATION / 2, Animation.PlayMode.NORMAL));
        animations.put(PlayerStateKind.DEAD, animation(grid, ROW_DEATH, 6, 0.16f, Animation.PlayMode.NORMAL));
        cast = animation(grid, ROW_CAST, 4, CAST_POSE_DURATION / 4, Animation.PlayMode.NORMAL);
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
     * Fait avancer l'animation, choisit le sens et note les images fantômes de l'esquive.
     * À appeler une fois par image tant que la partie avance.
     *
     * @param player le joueur
     * @param delta  temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Player player, float delta) {
        PlayerStateKind kind = player.stateKind();
        if (kind != shownKind) {
            shownKind = kind;
            stateTime = 0f;
            ghostTimer = 0f;
            ghosts.clear();
        } else {
            stateTime += delta;
        }
        // Mort : il reste tourné du côté où il est tombé.
        float x = player.facing().x();
        if (kind != PlayerStateKind.DEAD && Math.abs(x) > FLIP_DEAD_ZONE) {
            facingLeft = x < 0f;
        }
        if (kind == PlayerStateKind.DODGING) {
            ghostTimer += delta;
            if (ghostTimer >= GHOST_INTERVAL) {
                ghostTimer = 0f;
                ghosts.addLast(new Ghost(player.position(), currentFrame(player), facingLeft));
                if (ghosts.size() > GHOST_ALPHAS.length) {
                    ghosts.removeFirst();
                }
            }
        }
    }

    /**
     * Dessine le joueur, et ses images fantômes s'il esquive. Le {@link SpriteBatch} doit déjà
     * être ouvert.
     *
     * @param batch    le pinceau d'images, réglé sur la caméra du monde
     * @param player   le joueur à dessiner
     * @param position où le dessiner (sa position lissée, voir {@link MotionSmoothing})
     * @param alpha    opacité, de 0 à 1 (pour le clignotement d'invincibilité)
     */
    public void draw(SpriteBatch batch, Player player, Vec2 position, float alpha) {
        int i = 0;
        for (Ghost ghost : ghosts) {
            // Teinte turquoise, de plus en plus visible vers le joueur.
            batch.setColor(0.45f, 0.95f, 0.9f, GHOST_ALPHAS[i] * alpha);
            drawFrame(batch, ghost.frame(), ghost.position(), ghost.left());
            i++;
        }
        batch.setColor(1f, 1f, 1f, alpha);
        drawFrame(batch, currentFrame(player), position, facingLeft);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawFrame(SpriteBatch batch, TextureRegion frame, Vec2 center, boolean left) {
        float x = center.x() - BODY_CENTER_X;
        float y = center.y() - Player.RADIUS - EMPTY_BELOW_FEET;
        if (left) {
            // Largeur négative : l'image est retournée autour de son bord droit.
            batch.draw(frame, x + CELL, y, -CELL, CELL);
        } else {
            batch.draw(frame, x, y, CELL, CELL);
        }
    }

    // Juste après une compétence, au repos ou en marchant, on montre la pose de lancer.
    private TextureRegion currentFrame(Player player) {
        PlayerStateKind kind = player.stateKind();
        float time = shownKind == kind ? stateTime : 0f;
        if (kind == PlayerStateKind.IDLE || kind == PlayerStateKind.MOVING) {
            float sinceSkill = Math.min(player.skill1().timeSinceActivation(),
                    player.skill2().map(skill -> skill.timeSinceActivation()).orElse(Float.POSITIVE_INFINITY));
            if (sinceSkill < CAST_POSE_DURATION) {
                return cast.getKeyFrame(sinceSkill);
            }
        }
        return animations.get(kind).getKeyFrame(time);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
