package game.core.screen;

import game.core.Main;
import game.model.commun.Difficulty;
import game.model.commun.Settings;

/**
 * Écran des options, ouvert depuis le titre (O) ou la pause.
 *
 * <p>Il modifie directement les réglages partagés du jeu ({@link Main#settings()}) : en revenant
 * à la partie, les chiffres de dégâts sont tout de suite affichés ou masqués, sans relancer. La
 * difficulté s'applique à partir du stage suivant.</p>
 *
 * <p>On choisit une ligne avec les flèches (ou Z / S, la croix, le stick), on la règle avec gauche /
 * droite (ou Q / D) ou Entrée (A). Échap (B) revient à l'écran d'où on l'a ouvert.</p>
 */
public final class OptionsScreen extends MenuScreen {

    private static final int DAMAGE_NUMBERS = 0;
    private static final int DIFFICULTY = 1;
    private static final int BACK = 2;
    private static final int ITEM_COUNT = 3;

    /** Écart vertical entre deux lignes du menu, en unités. */
    private static final float ITEM_GAP = 26f;

    private final Settings settings;
    private int selected = DAMAGE_NUMBERS;

    /**
     * Crée l'écran des options.
     *
     * @param main le jeu, pour ses réglages et pour revenir à l'écran précédent
     */
    public OptionsScreen(Main main) {
        super(main);
        this.settings = main.settings();
    }

    @Override
    protected boolean handleInput() {
        if (backPressed() || (selected == BACK && confirmPressed())) {
            main.pop();
            return true;
        }
        if (upPressed()) {
            selected = (selected + ITEM_COUNT - 1) % ITEM_COUNT;
        }
        if (downPressed()) {
            selected = (selected + 1) % ITEM_COUNT;
        }
        if (leftPressed()) {
            change(-1);
        } else if (rightPressed() || confirmPressed()) {
            change(1);
        }
        return false;
    }

    // Règle la ligne choisie : bascule les chiffres de dégâts, ou passe à la difficulté
    // suivante (step = 1) ou précédente (step = -1), en boucle.
    private void change(int step) {
        if (selected == DAMAGE_NUMBERS) {
            settings.setShowDamageNumbers(!settings.showDamageNumbers());
        } else if (selected == DIFFICULTY) {
            Difficulty[] all = Difficulty.values();
            settings.setDifficulty(all[Math.floorMod(settings.difficulty().ordinal() + step, all.length)]);
        }
    }

    @Override
    protected void drawContent() {
        batch.begin();
        centered("OPTIONS", HEIGHT * 0.78f, TITLE_SCALE * 0.8f, TEXT);
        String[] items = {
            "Chiffres de dégâts : " + (settings.showDamageNumbers() ? "oui" : "non"),
            "Difficulté : " + label(settings.difficulty()),
            "Retour"
        };
        float y = HEIGHT * 0.55f;
        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean isSelected = i == selected;
            String text = isSelected ? "> " + items[i] + " <" : items[i];
            centered(text, y - i * ITEM_GAP, TEXT_SCALE * 1.2f, isSelected ? SELECTED : TEXT);
        }
        centered("La difficulté change à partir du prochain étage.", y - ITEM_COUNT * ITEM_GAP - 8f,
                TEXT_SCALE * 0.7f, HINT);
        centered(hint("Flèches : choisir et régler · Entrée : valider · Échap : retour",
                "Croix : choisir et régler · A : valider · B : retour"), 30f, TEXT_SCALE * 0.7f, HINT);
        batch.end();
    }

    private static String label(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "facile";
            case NORMAL -> "normale";
            case HARD -> "difficile";
        };
    }
}
