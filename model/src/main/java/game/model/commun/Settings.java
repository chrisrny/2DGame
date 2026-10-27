package game.model.commun;

import java.util.Objects;

/**
 * Les réglages du joueur, modifiables à tout moment depuis l'écran des options, même en pleine
 * partie.
 */
public final class Settings {

    private boolean showDamageNumbers = true;
    private Difficulty difficulty;

    /**
     * Crée des réglages, avec les chiffres de dégâts affichés.
     *
     * @param difficulty la difficulté de départ
     * @throws NullPointerException si la difficulté est {@code null}
     */
    public Settings(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }

    /** @return {@code true} si les chiffres de dégâts sont affichés au-dessus des cibles touchées */
    public boolean showDamageNumbers() {
        return showDamageNumbers;
    }

    /**
     * Affiche ou masque les chiffres de dégâts.
     *
     * @param show {@code true} pour les afficher
     */
    public void setShowDamageNumbers(boolean show) {
        this.showDamageNumbers = show;
    }

    /** @return la difficulté choisie */
    public Difficulty difficulty() {
        return difficulty;
    }

    /**
     * Change la difficulté. Elle s'applique aux ennemis à partir du prochain stage.
     *
     * @param difficulty la nouvelle difficulté
     * @throws NullPointerException si la difficulté est {@code null}
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }
}
