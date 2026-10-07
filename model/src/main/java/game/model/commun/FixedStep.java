package game.model.commun;

/**
 * Horloge à pas fixe : transforme le temps réel d'une image en un nombre de pas de jeu
 * de durée constante.
 *
 * <p>libGDX donne à chaque image un {@code delta} qui varie selon la machine et sa charge.
 * Si le modèle avançait de {@code delta}, le jeu ne se comporterait pas pareil d'une machine
 * à l'autre, et un test ne pourrait jamais rejouer exactement la même partie. On accumule
 * donc le temps reçu dans un « réservoir », et on en retire des tranches de {@link #DT}
 * tant qu'il y en a assez : une image peut ainsi faire 0, 1 ou plusieurs pas.</p>
 *
 * <p>Le nombre de pas par image est plafonné à {@link #MAX_STEPS} : si le jeu rame, on
 * préfère ralentir que de rattraper un retard qui ferait encore plus ramer.</p>
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

    /**
     * Temps restant dans le réservoir, pas encore consommé par un pas de jeu.
     *
     * @return le temps en attente, en secondes (toujours inférieur à {@link #DT} après un appel normal)
     */
    public float pending() {
        return accumulator;
    }
}
