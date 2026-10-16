package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;

/**
 * Dessine le croissant de lame de l'attaque de base ({@code assets/sprites/slash.png}).
 *
 * <p>Le corps du héros ne fait que se retourner gauche/droite : c'est ce croissant qui montre
 * la vraie direction du coup, à 360°. Il est tourné vers la visée du début de l'attaque et
 * couvre la zone touchée par le modèle ({@link Player#attackZone()}) : ce qu'on voit, c'est ce
 * qui frappe.</p>
 *
 * <p>Il apparaît une image après le début de l'attaque, quand le bras part (l'image d'avant est
 * l'élan), et il est dessiné en lumière additive.</p>
 */
public final class SlashEffect implements Disposable {

    /** Taille d'une image de l'effet, en pixels. */
    private static final int SIZE = 64;

    // Le centre du joueur est au pixel (8, 32) de chaque image (32 depuis le bas aussi).
    private static final float ORIGIN_X = 8f;
    private static final float ORIGIN_Y = 32f;

    // L'attaque du héros a 5 images ; le croissant suit les 4 dernières.
    private static final float FRAME_DURATION = Player.ATTACK_DURATION / 5;

    /** Au-delà de cette part verticale de la visée vers le haut, le coup passe derrière le corps. */
    private static final float BEHIND_THRESHOLD = 0.3f;

    private final Texture texture;
    private final Animation<TextureRegion> animation;
    private boolean active;
    private float time;
    private Vec2 aim = new Vec2(1f, 0f);

    /** Charge l'image de l'effet. */
    public SlashEffect() {
        texture = new Texture(Gdx.files.internal("sprites/slash.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        animation = new Animation<>(FRAME_DURATION, TextureRegion.split(texture, SIZE, SIZE)[0]);
        animation.setPlayMode(Animation.PlayMode.NORMAL);
    }

    /**
     * Démarre l'effet au début d'une attaque et le fait avancer. À appeler une fois par image.
     *
     * @param player le joueur
     * @param delta  temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Player player, float delta) {
        boolean attacking = player.stateKind() == PlayerStateKind.ATTACKING;
        if (attacking && !active) {
            // Visée figée au départ : le croissant ne tourne pas si la souris bouge pendant le coup.
            aim = player.facing();
            time = 0f;
        } else if (attacking) {
            time += delta;
        }
        active = attacking;
    }

    /**
     * Indique si l'effet est à l'écran en ce moment.
     *
     * @return {@code true} pendant la partie visible de l'attaque
     */
    public boolean visible() {
        return active && time >= FRAME_DURATION;
    }

    /**
     * Indique si le coup part vers le haut de l'écran : il faut alors le dessiner derrière le
     * corps du héros.
     *
     * @return {@code true} si le croissant passe derrière le héros
     */
    public boolean behindHero() {
        return aim.y() > BEHIND_THRESHOLD;
    }

    /**
     * Dessine le croissant autour du joueur. Le {@link SpriteBatch} doit déjà être ouvert ; le
     * mélange normal est remis à la fin.
     *
     * @param batch  le pinceau d'images, réglé sur la caméra du monde
     * @param center le centre du joueur
     */
    public void draw(SpriteBatch batch, Vec2 center) {
        if (!visible()) {
            return;
        }
        TextureRegion frame = animation.getKeyFrame(time - FRAME_DURATION);
        float angle = (float) Math.toDegrees(Math.atan2(aim.y(), aim.x()));
        // Vers la gauche, on retourne le croissant en hauteur : le geste balaie dans le même
        // sens que le bras du héros retourné.
        float scaleY = aim.x() < 0f ? -1f : 1f;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.draw(frame, center.x() - ORIGIN_X, center.y() - ORIGIN_Y, ORIGIN_X, ORIGIN_Y,
                SIZE, SIZE, 1f, scaleY, angle);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
