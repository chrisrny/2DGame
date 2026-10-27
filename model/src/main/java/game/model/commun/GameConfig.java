package game.model.commun;

import java.util.Objects;

/**
 * Réglages d'une partie : la graine du hasard, la difficulté et les statistiques de départ.
 *
 * @param seed graine du hasard
 * @param difficulty niveau de difficulté
 * @param player statistiques de départ du joueur
 */
public record GameConfig(long seed, Difficulty difficulty, PlayerStats player) {

    /**
     * Vérifie que la configuration est complète.
     *
     * @throws NullPointerException si la difficulté ou les statistiques sont {@code null}
     */
    public GameConfig {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(player, "player");
    }

    /**
     * Configuration par défaut avec une graine choisie : à utiliser dans les tests.
     *
     * @param seed la graine du hasard
     * @return une configuration en difficulté normale
     */
    public static GameConfig defaults(long seed) {
        return new GameConfig(seed, Difficulty.NORMAL, PlayerStats.defaults());
    }

    /**
     * Configuration par défaut avec une graine différente à chaque appel : à utiliser pour lancer
     * une vraie partie.
     */
    public static GameConfig defaults() {
        return defaults(System.nanoTime());
    }
}
