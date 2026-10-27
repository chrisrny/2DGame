package game.model.world;

import java.util.Map;
import java.util.Objects;

/**
 * Une vague d'ennemis : combien de chaque type, et après combien de temps de répit.
 *
 * @param enemies nombre d'ennemis par type
 * @param delayBefore répit avant l'apparition de la vague, en secondes
 */
public record Wave(Map<EnemyType, Integer> enemies, float delayBefore) {

    /**
     * Vérifie la vague et en fait une copie non modifiable.
     *
     * @throws NullPointerException si {@code enemies} est {@code null}
     * @throws IllegalArgumentException si un nombre ou le délai est négatif
     */
    public Wave {
        enemies = Map.copyOf(Objects.requireNonNull(enemies, "enemies"));
        if (delayBefore < 0f || enemies.values().stream().anyMatch(count -> count < 0)) {
            throw new IllegalArgumentException("Une vague ne peut pas avoir de valeur négative");
        }
    }

    /** Nombre total d'ennemis de la vague. */
    public int size() {
        return enemies.values().stream().mapToInt(Integer::intValue).sum();
    }
}
