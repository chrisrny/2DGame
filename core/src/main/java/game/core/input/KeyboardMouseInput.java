package game.core.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import java.util.Objects;

/**
 * Entrées clavier et souris.
 *
 * <ul>
 *   <li>Déplacement : Z / Q / S / D sur un clavier AZERTY (W / A / S / D en QWERTY) et les flèches.</li>
 *   <li>Visée : la souris.</li>
 *   <li>Attaque : clic gauche. Compétence 1 : clic droit. Compétence 2 : E. Esquive : Espace.</li>
 * </ul>
 *
 * <p>libGDX nomme les touches d'après leur position sur un clavier QWERTY : {@code Keys.W}
 * est la touche en haut à gauche des lettres, c'est-à-dire le Z d'un clavier AZERTY.
 * Utiliser W / A / S / D donne donc Z / Q / S / D en AZERTY, sans rien configurer.</p>
 *
 * <p>Les entrées sont lues par interrogation ({@code Gdx.input.isKeyPressed}) une fois
 * par image : on veut savoir si une touche est enfoncée maintenant, pas réagir à un événement.</p>
 */
public final class KeyboardMouseInput implements InputSource {

    private final Viewport viewport;
    // Réutilisé à chaque image pour ne pas créer un objet par image.
    private final Vector2 mouse = new Vector2();

    /**
     * Crée la source d'entrées.
     *
     * @param viewport le viewport du jeu, pour convertir la position de la souris
     *                 (en pixels de la fenêtre) en coordonnées du monde
     */
    public KeyboardMouseInput(Viewport viewport) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    @Override
    public PlayerIntent poll(Vec2 playerPosition) {
        Vec2 move = Vec2.fromAxes(horizontalAxis(), verticalAxis());
        Vec2 aim = mouseInWorld().sub(playerPosition);
        return new PlayerIntent(
                move,
                aim,
                Gdx.input.isButtonPressed(Buttons.LEFT),
                Gdx.input.isKeyPressed(Keys.SPACE),
                Gdx.input.isButtonPressed(Buttons.RIGHT),
                Gdx.input.isKeyPressed(Keys.E));
    }

    private int horizontalAxis() {
        int axis = 0;
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
            axis++;
        }
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
            axis--;
        }
        return axis;
    }

    private int verticalAxis() {
        int axis = 0;
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) {
            axis++;
        }
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
            axis--;
        }
        return axis;
    }

    /**
     * Position de la souris dans le monde. La fenêtre a son origine en haut à gauche avec
     * y vers le bas, le monde en bas à gauche avec y vers le haut : {@code unproject} fait
     * la conversion, en tenant compte des bandes noires du viewport.
     */
    private Vec2 mouseInWorld() {
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);
        return new Vec2(mouse.x, mouse.y);
    }
}
