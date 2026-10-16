package game.core.screen;

import com.badlogic.gdx.graphics.Color;
import game.core.Main;
import game.model.world.Run;
import java.util.Objects;

/**
 * Base des écrans de fin de partie : un grand titre, le bilan de la partie, puis Entrée pour
 * rejouer (nouvelle partie, nouvelle graine) ou Échap pour revenir au titre.
 */
public abstract class EndScreen extends MenuScreen {

    /** Écart vertical entre deux lignes du bilan, en unités. */
    private static final float LINE_GAP = 20f;

    private final Run run;
    private final String title;
    private final Color titleColor;

    /**
     * Crée l'écran de fin.
     *
     * @param main       le jeu, pour changer d'écran
     * @param run        la partie terminée, dont on affiche le bilan
     * @param title      le grand titre
     * @param titleColor la couleur du titre
     */
    protected EndScreen(Main main, Run run, String title, Color titleColor) {
        super(main);
        this.run = Objects.requireNonNull(run, "run");
        this.title = Objects.requireNonNull(title, "title");
        this.titleColor = Objects.requireNonNull(titleColor, "titleColor");
    }

    @Override
    protected boolean handleInput() {
        if (confirmPressed()) {
            main.newGame();
            return true;
        }
        if (backPressed()) {
            main.showTitle();
            return true;
        }
        return false;
    }

    @Override
    protected void drawContent() {
        batch.begin();
        centered(title, HEIGHT * 0.78f, TITLE_SCALE, titleColor);
        float y = HEIGHT * 0.52f;
        centered("Stage atteint : " + run.stage(), y, TEXT_SCALE, TEXT);
        centered("Ennemis tués : " + run.kills(), y - LINE_GAP, TEXT_SCALE, TEXT);
        centered("Niveau : " + run.experience().level(), y - 2 * LINE_GAP, TEXT_SCALE, TEXT);
        centered("Temps : " + formatTime(run.elapsed()), y - 3 * LINE_GAP, TEXT_SCALE, TEXT);
        centered("Entrée : rejouer", HEIGHT * 0.18f, TEXT_SCALE, TEXT);
        centered("Échap : menu titre", HEIGHT * 0.18f - 22f, TEXT_SCALE, HINT);
        batch.end();
    }

    private static String formatTime(float seconds) {
        int total = (int) seconds;
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
