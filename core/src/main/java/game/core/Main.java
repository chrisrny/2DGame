package game.core;

import com.badlogic.gdx.Game;
import game.core.screen.GameScreen;

/**
 * Classe principale du jeu côté libGDX : gère le passage d'un écran à l'autre
 * (titre, partie, pause, options, fin).
 */
public class Main extends Game {

    @Override
    public void create() {
        setScreen(new GameScreen());
    }

    @Override
    public void dispose() {
        // Libère l'écran courant : chaque écran possède et libère ses propres ressources.
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
