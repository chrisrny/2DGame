package game.core.screen;

import game.core.Main;

/**
 * Écran des options, ouvert depuis le titre (O) ou la pause.
 *
 * <p>Provisoire : il ne propose encore aucun réglage. Les réglages à effet immédiat arrivent
 * avec P2-B5. Échap ou Entrée revient à l'écran d'où on l'a ouvert.</p>
 */
public final class OptionsScreen extends MenuScreen {

    /**
     * Crée l'écran des options.
     *
     * @param main le jeu, pour revenir à l'écran précédent
     */
    public OptionsScreen(Main main) {
        super(main);
    }

    @Override
    protected boolean handleInput() {
        if (backPressed() || confirmPressed()) {
            main.pop();
            return true;
        }
        return false;
    }

    @Override
    protected void drawContent() {
        batch.begin();
        centered("OPTIONS", HEIGHT * 0.72f, TITLE_SCALE * 0.8f, TEXT);
        centered("Aucun réglage pour l'instant.", HEIGHT * 0.45f, TEXT_SCALE, HINT);
        centered("Échap : retour", 30f, TEXT_SCALE * 0.7f, HINT);
        batch.end();
    }
}
