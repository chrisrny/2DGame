package game.model.world;

import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Prowler;
import java.util.Objects;

/** Fabrique d'ennemis (pattern Factory) : crée le bon ennemi à partir de son type. */
public final class EnemyFactory {

    private EnemyFactory() {
    }

    /**
     * Crée un ennemi.
     *
     * @param type le type d'ennemi
     * @param position sa position d'apparition
     * @param multiplier multiplicateur de ses points de vie et de ses dégâts (1 aux valeurs de
     *     référence)
     * @return le nouvel ennemi
     * @throws NullPointerException si le type ou la position est {@code null}
     * @throws IllegalArgumentException si le multiplicateur n'est pas strictement positif
     */
    public static Enemy create(EnemyType type, Vec2 position, float multiplier) {
        Objects.requireNonNull(type, "type");
        return switch (type) {
            case PROWLER -> new Prowler(position, multiplier);
            case ARCHER -> new Archer(position, multiplier);
            case CHARGER -> new Charger(position, multiplier);
        };
    }
}
