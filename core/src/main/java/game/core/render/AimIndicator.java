package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;

/**
 * Petite flèche qui tourne autour du héros, dans la direction de sa visée ({@code
 * assets/sprites/aim.png}).
 */
public final class AimIndicator implements Disposable {

    /** Taille de l'image de la flèche, en pixels. */
    private static final int SIZE = 16;

    // Le milieu du corps est 14 unités au-dessus du centre du joueur (le sprite part de ses pieds).
    private static final float BODY_OFFSET_Y = 14f;

    // Distance entre le milieu du corps et la flèche : juste au-delà de la pointe de la lance.
    private static final float ORBIT = 30f;

    // Un peu transparente : elle informe sans cacher ce qu'il y a derrière.
    private static final float ALPHA = 0.8f;

    private final Texture texture;
    private final TextureRegion arrow;

    /** Charge l'image de la flèche. */
    public AimIndicator() {
        texture = new Texture(Gdx.files.internal("sprites/aim.png"));
        // Pixel art : pas de lissage, même tournée la flèche reste nette.
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        arrow = new TextureRegion(texture);
    }

    /**
     * Dessine la flèche autour du joueur, sauf s'il est mort. Le {@link SpriteBatch} doit déjà être
     * ouvert, en mélange normal.
     *
     * @param batch le pinceau d'images, réglé sur la caméra du monde
     * @param player le joueur
     * @param center où il est dessiné (sa position lissée, voir {@link MotionSmoothing})
     */
    public void draw(SpriteBatch batch, Player player, Vec2 center) {
        if (player.stateKind() == PlayerStateKind.DEAD) {
            return;
        }
        Vec2 aim = player.facing();
        float angle = (float) Math.toDegrees(Math.atan2(aim.y(), aim.x()));
        float x = center.x() + aim.x() * ORBIT;
        float y = center.y() + BODY_OFFSET_Y + aim.y() * ORBIT;
        batch.setColor(1f, 1f, 1f, ALPHA);
        batch.draw(arrow, x - SIZE / 2f, y - SIZE / 2f, SIZE / 2f, SIZE / 2f, SIZE, SIZE, 1f, 1f, angle);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
