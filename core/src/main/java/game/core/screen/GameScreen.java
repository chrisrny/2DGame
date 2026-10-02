package game.core.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Écran de partie.
 *
 * <p>À chaque image, il fera trois choses dans cet ordre : lire les entrées,
 * faire avancer le modèle à pas fixe, dessiner. Pour l'instant, il efface seulement l'écran.</p>
 */
public class GameScreen extends ScreenAdapter {

    @Override
    public void render(float delta) {
        // Fond sombre du donjon.
        ScreenUtils.clear(0.07f, 0.06f, 0.09f, 1f);
    }
}
