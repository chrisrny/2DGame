package game.core.screen;

import com.badlogic.gdx.graphics.Color;
import game.core.Main;
import game.model.world.Run;

/** Écran de victoire, affiché quand le boss est vaincu. */
public final class VictoryScreen extends EndScreen {

    /**
     * Crée l'écran de victoire.
     *
     * @param main le jeu, pour changer d'écran
     * @param run la partie gagnée
     */
    public VictoryScreen(Main main, Run run) {
        super(main, run, "VICTOIRE", new Color(1f, 0.85f, 0.35f, 1f));
    }
}
