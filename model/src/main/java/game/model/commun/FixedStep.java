package game.model.commun;

/**
 * Horloge à pas fixe : transforme le temps réel d'une image en un nombre de pas de jeu de durée
 * constante.
 */
public final class FixedStep {

    /** Durée d'un pas de jeu : 1/60 de seconde. */
    public static final float DT = 1f / 60f;

    /** Nombre maximum de pas de jeu exécutés pour une seule image. */
    public static final int MAX_STEPS = 5;

    private float accumulator;

    /**
     * Ajoute le temps écoulé depuis la dernière image et indique combien de pas de jeu faire.
     *
     * @param delta temps écoulé depuis la dernière image, en secondes
     * @return le nombre de pas de {@link #DT} à exécuter maintenant, entre 0 et {@link #MAX_STEPS}
     */
    public int advance(float delta) {
        accumulator += Math.max(0f, delta);
        int steps = 0;
        while (accumulator >= DT && steps < MAX_STEPS) {
            accumulator -= DT;
            steps++;
        }
        if (steps == MAX_STEPS) {
            // Trop de retard : on l'abandonne au lieu d'essayer de le rattraper.
            accumulator = 0f;
        }
        return steps;
    }

    /** Temps restant dans le réservoir, pas encore consommé par un pas de jeu. */
    public float pending() {
        return accumulator;
    }
}
