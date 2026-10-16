package game.core.screen;

import com.badlogic.gdx.graphics.Color;
import game.core.Main;
import game.model.world.Run;

/**
 * Écran de défaite, affiché quand le joueur est mort.
 */
public final class DefeatScreen extends EndScreen {

    /**
     * Crée l'écran de défaite.
     *
     * @param main le jeu, pour changer d'écran
     * @param run  la partie perdue
     */
    public DefeatScreen(Main main, Run run) {
        super(main, run, "DÉFAITE", new Color(0.9f, 0.3f, 0.3f, 1f));
    }
}
