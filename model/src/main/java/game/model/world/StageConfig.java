package game.model.world;

import java.util.List;
import java.util.Objects;

/**
 * La description d'un stage : ses vagues, ce qu'il faut tuer pour le finir et sa difficulté.
 *
 * @param number numéro du stage, à partir de 1
 * @param waves les vagues, dans l'ordre d'apparition
 * @param killsToClear nombre d'ennemis à tuer pour finir le stage
 * @param multiplier multiplicateur des points de vie et des dégâts des ennemis du stage
 */
public record StageConfig(int number, List<Wave> waves, int killsToClear, float multiplier) {

    /**
     * Vérifie le stage et fait une copie non modifiable des vagues.
     *
     * @throws NullPointerException si {@code waves} est {@code null}
     * @throws IllegalArgumentException si le numéro, le nombre d'ennemis à tuer ou le
     *     multiplicateur ne sont pas strictement positifs
     */
    public StageConfig {
        waves = List.copyOf(Objects.requireNonNull(waves, "waves"));
        if (number < 1 || killsToClear < 1 || !(multiplier > 0f)) {
            throw new IllegalArgumentException("Le numéro, les ennemis à tuer et le multiplicateur doivent être positifs");
        }
    }

    /**
     * Le même stage, avec un multiplicateur supplémentaire : par exemple celui de la difficulté.
     *
     * @param factor le facteur à appliquer au multiplicateur
     * @return un nouveau stage dont le multiplicateur vaut {@code multiplier * factor}
     */
    public StageConfig scaled(float factor) {
        return new StageConfig(number, waves, killsToClear, multiplier * factor);
    }
}
