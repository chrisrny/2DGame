package game.core.screen;

import game.core.Main;
import game.core.input.Gamepad.Button;
import java.util.Objects;

/** Écran de pause, ouvert avec Échap (Start à la manette) pendant la partie. */
public final class PauseScreen extends MenuScreen {

    private static final String[] ITEMS = {"Reprendre", "Options", "Quitter"};
    private static final int RESUME = 0;
    private static final int OPTIONS = 1;
    private static final int QUIT = 2;

    /** Écart vertical entre deux entrées du menu, en unités. */
    private static final float ITEM_GAP = 26f;

    private final GameScreen game;
    private int selected = RESUME;

    /**
     * Crée l'écran de pause.
     *
     * @param main le jeu, pour changer d'écran
     * @param game la partie en pause, dessinée figée derrière le menu
     */
    public PauseScreen(Main main, GameScreen game) {
        super(main);
        this.game = Objects.requireNonNull(game, "game");
    }

    @Override
    protected boolean handleInput() {
        if (backPressed() || pad().justPressed(Button.START)) {
            main.pop();
            return true;
        }
        if (upPressed()) {
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
        }
        if (downPressed()) {
            selected = (selected + 1) % ITEMS.length;
        }
        if (!confirmPressed()) {
            return false;
        }
        switch (selected) {
            case OPTIONS -> main.push(new OptionsScreen(main));
            case QUIT -> main.showTitle();
            default -> main.pop();
        }
        return true;
    }

    @Override
    protected void drawBackground() {
        game.drawFrozen();
    }

    @Override
    protected void drawContent() {
        veil(0.7f);
        batch.begin();
        // Plus haut que l'annonce « Stage N » de la partie, qui peut être figée derrière.
        centered("PAUSE", HEIGHT * 0.84f, TITLE_SCALE * 0.8f, TEXT);
        float y = HEIGHT * 0.48f;
        for (int i = 0; i < ITEMS.length; i++) {
            boolean isSelected = i == selected;
            String label = isSelected ? "> " + ITEMS[i] + " <" : ITEMS[i];
            centered(label, y - i * ITEM_GAP, TEXT_SCALE * 1.2f, isSelected ? SELECTED : TEXT);
        }
        centered(hint("Flèches : choisir · Entrée : valider · Échap : reprendre",
                "Croix : choisir · A : valider · B ou Start : reprendre"), 30f, TEXT_SCALE * 0.7f, HINT);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // La partie est dessinée derrière : elle doit suivre la taille de la fenêtre elle aussi.
        game.resize(width, height);
    }
}
