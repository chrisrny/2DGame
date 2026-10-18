package game.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import game.core.Main;

/**
 * Écran titre : Entrée lance une partie, O ouvre les options, Échap quitte le jeu.
 */
public final class TitleScreen extends MenuScreen {

    /**
     * Crée l'écran titre.
     *
     * @param main le jeu, pour changer d'écran
     */
    public TitleScreen(Main main) {
        super(main);
    }

    @Override
    protected boolean handleInput() {
        if (confirmPressed()) {
            main.newGame();
            return true;
        }
        if (pressed(Keys.O)) {
            main.push(new OptionsScreen(main));
            return true;
        }
        if (backPressed()) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }

    @Override
    protected void drawContent() {
        batch.begin();
        centered("Kagaribi", HEIGHT * 0.74f, TITLE_SCALE * 1.2f, SELECTED);
        centered("Le quartier des braises  ·  action-roguelite", HEIGHT * 0.74f - 46f, TEXT_SCALE, TEXT);
        centered("Entrée : jouer", HEIGHT * 0.40f, TEXT_SCALE, TEXT);
        centered("O : options", HEIGHT * 0.40f - 22f, TEXT_SCALE, TEXT);
        centered("Échap : quitter", HEIGHT * 0.40f - 44f, TEXT_SCALE, HINT);
        batch.end();
    }
}
