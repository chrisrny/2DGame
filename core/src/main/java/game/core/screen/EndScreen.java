package game.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import game.core.Main;
import game.model.world.Run;
import java.util.Objects;

/**
 * Base des écrans de fin de partie : un grand titre, le bilan de la partie, puis Entrée (A) pour
 * rejouer (nouvelle partie, nouvelle graine) ou Échap (B) pour revenir au titre.
 */
public abstract class EndScreen extends MenuScreen {

    /** Écart vertical entre deux lignes du bilan, en unités. */
    private static final float LINE_GAP = 20f;

    /** Durée des jingles de fin, en secondes : la musique des menus attend qu'ils soient finis. */
    private static final float JINGLE_LENGTH = 3f;

    private final Run run;
    private final String title;
    private final Color titleColor;
    private final Sound jingle;

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
        // Jingle de fin : koto qui monte et gong pour la victoire, koto qui descend pour la défaite.
        this.jingle = Gdx.audio.newSound(Gdx.files.internal(run.isWon() ? "sfx/victory.ogg" : "sfx/defeat.ogg"));
    }

    @Override
    public void show() {
        jingle.play(0.9f);
        // La musique des menus revient une fois le jingle fini.
        main.menuMusic().play(JINGLE_LENGTH);
    }

    @Override
    public void dispose() {
        super.dispose();
        jingle.dispose();
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
        centered("Étage atteint : " + run.stage(), y, TEXT_SCALE, TEXT);
        centered("Ennemis tués : " + run.kills(), y - LINE_GAP, TEXT_SCALE, TEXT);
        centered("Niveau : " + run.experience().level(), y - 2 * LINE_GAP, TEXT_SCALE, TEXT);
        centered("Temps : " + formatTime(run.elapsed()), y - 3 * LINE_GAP, TEXT_SCALE, TEXT);
        centered(hint("Entrée : rejouer", "A : rejouer"), HEIGHT * 0.18f, TEXT_SCALE, TEXT);
        centered(hint("Échap : menu titre", "B : menu titre"), HEIGHT * 0.18f - 22f, TEXT_SCALE, HINT);
        batch.end();
    }

    private static String formatTime(float seconds) {
        int total = (int) seconds;
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
