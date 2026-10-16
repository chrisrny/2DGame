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
 * <p>Deux choses décident de l'image affichée :</p>
 * <ul>
 *   <li><b>l'état du joueur</b> ({@link PlayerStateKind}, pattern State dans le modèle) choisit
 *   l'animation : repos, course, attaque, esquive, coup reçu, mort. Le temps de l'animation
 *   repart de zéro à chaque changement d'état. Les durées d'attaque et de coup reçu valent
 *   exactement celles du modèle ({@link Player#ATTACK_DURATION}, {@link Player#HURT_DURATION}) :
 *   l'animation finit pile quand l'état change ;</li>
 *   <li><b>la direction du regard</b> (vers la souris) choisit la vue : de côté (retournée quand
 *   il regarde à gauche), de face quand il regarde vers le bas, de dos vers le haut. Un petit
 *   seuil évite que la vue clignote quand on vise pile en diagonale.</li>
 * </ul>
 *
 * <p>La feuille est une grille de cases de {@link #CELL} pixels : 6 lignes par vue (repos,
 * course, attaque, esquive, coup reçu, lancer) pour les vues de côté, de face et de dos, puis
 * une ligne pour la mort.</p>
 */
public final class PlayerSprite implements Disposable {

    /** Taille d'une case de la feuille, en pixels (et en unités du monde). */
    public static final int CELL = 48;

    /** Durée pendant laquelle la pose de lancer s'affiche après une compétence, en secondes. */
    public static final float CAST_POSE_DURATION = 0.18f;

    /**
     * Rapport à dépasser entre les deux composantes du regard pour changer de vue : au-delà de
     * 45°, il faut viser un peu plus franchement avant de basculer.
     */
    private static final float VIEW_SWITCH_RATIO = 1.2f;

    // Dans une case, le corps est centré sur la colonne 24 et les pieds touchent la ligne 46
    // (en partant du haut) : il reste 1 pixel vide sous les pieds.
    private static final float BODY_CENTER_X = 24f;
    private static final float EMPTY_BELOW_FEET = 1f;

    /** Les trois vues du personnage. */
    private enum View {
        SIDE(0), FRONT(6), BACK(12);

        private final int firstRow;

        View(int firstRow) {
            this.firstRow = firstRow;
        }
    }

    // Lignes, à partir de la première ligne de la vue.
    private static final int ROW_IDLE = 0;
    private static final int ROW_RUN = 1;
    private static final int ROW_ATTACK = 2;
    private static final int ROW_DODGE = 3;
    private static final int ROW_HURT = 4;
    private static final int ROW_CAST = 5;
    private static final int ROW_DEATH = 18;

    private final Texture texture;
    private final Map<View, Map<PlayerStateKind, Animation<TextureRegion>>> animations = new EnumMap<>(View.class);
    private final Map<View, Animation<TextureRegion>> casts = new EnumMap<>(View.class);
    private final Animation<TextureRegion> death;
    private PlayerStateKind shownKind;
    private View view = View.SIDE;
    private float stateTime;

    /** Charge la feuille de sprites et prépare les animations des trois vues. */
    public PlayerSprite() {
        texture = new Texture(Gdx.files.internal("sprites/hero.png"));
        // Pixel art : pas de lissage, chaque pixel reste net à l'agrandissement.
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(texture, CELL, CELL);
        for (View v : View.values()) {
            int r = v.firstRow;
            Map<PlayerStateKind, Animation<TextureRegion>> byKind = new EnumMap<>(PlayerStateKind.class);
            byKind.put(PlayerStateKind.IDLE, animation(grid, r + ROW_IDLE, 6, 0.12f, Animation.PlayMode.LOOP));
            byKind.put(PlayerStateKind.MOVING, animation(grid, r + ROW_RUN, 6, 0.08f, Animation.PlayMode.LOOP));
            byKind.put(PlayerStateKind.ATTACKING,
                    animation(grid, r + ROW_ATTACK, 4, Player.ATTACK_DURATION / 4, Animation.PlayMode.NORMAL));
            byKind.put(PlayerStateKind.DODGING, animation(grid, r + ROW_DODGE, 3, 0.07f, Animation.PlayMode.NORMAL));
            byKind.put(PlayerStateKind.HURT,
                    animation(grid, r + ROW_HURT, 2, Player.HURT_DURATION / 2, Animation.PlayMode.NORMAL));
            animations.put(v, byKind);
            casts.put(v, animation(grid, r + ROW_CAST, 3, CAST_POSE_DURATION / 3, Animation.PlayMode.NORMAL));
        }
        death = animation(grid, ROW_DEATH, 5, 0.18f, Animation.PlayMode.NORMAL);
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
     * Fait avancer l'animation et choisit la vue d'après le regard. À appeler une fois par
     * image tant que la partie avance (pas pendant la pause ni le choix de relique).
     *
     * @param player le joueur
     * @param delta  temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Player player, float delta) {
        PlayerStateKind kind = player.stateKind();
        if (kind != shownKind) {
            shownKind = kind;
            stateTime = 0f;
        } else {
            stateTime += delta;
        }
        // Mort : il garde la vue où il est tombé.
        if (kind != PlayerStateKind.DEAD) {
            view = nextView(player.facing());
        }
    }

    // Vue de côté si le regard est plutôt horizontal, de face ou de dos s'il est plutôt vertical.
    // On ne bascule qu'au-delà d'un petit seuil, pour éviter le clignotement en diagonale.
    private View nextView(Vec2 facing) {
        float ax = Math.abs(facing.x());
        float ay = Math.abs(facing.y());
        boolean vertical;
        if (view == View.SIDE) {
            vertical = ay > ax * VIEW_SWITCH_RATIO;
        } else {
            vertical = ax <= ay * VIEW_SWITCH_RATIO;
        }
        if (!vertical) {
            return View.SIDE;
        }
        // Le monde a l'axe y vers le haut : regarder vers le haut, c'est montrer son dos.
        return facing.y() > 0f ? View.BACK : View.FRONT;
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
        boolean sideways = view == View.SIDE || player.stateKind() == PlayerStateKind.DEAD;
        batch.setColor(1f, 1f, 1f, alpha);
        if (sideways && player.facing().x() < 0f) {
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
        float time = shownKind == kind ? stateTime : 0f;
        if (kind == PlayerStateKind.DEAD) {
            return death.getKeyFrame(time);
        }
        if (kind == PlayerStateKind.IDLE || kind == PlayerStateKind.MOVING) {
            float sinceSkill = Math.min(player.skill1().timeSinceActivation(),
                    player.skill2().map(skill -> skill.timeSinceActivation()).orElse(Float.POSITIVE_INFINITY));
            if (sinceSkill < CAST_POSE_DURATION) {
                return casts.get(view).getKeyFrame(sinceSkill);
            }
        }
        return animations.get(view).get(kind).getKeyFrame(time);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
