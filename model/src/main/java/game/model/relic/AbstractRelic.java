package game.model.relic;

import game.model.commun.PlayerStats;
import java.util.Objects;

/**
 * Base commune des reliques : elle garde le nom et la description, pour que chaque relique
 * n'ait plus qu'à décrire son effet.
 *
 * <p>Par défaut, une relique ne change aucune statistique ; celles qui en changent
 * redéfinissent {@link #apply(PlayerStats)}.</p>
 *
 * <p>Le nom et la description sont affichés avec la police par défaut de libGDX, qui ne
 * contient que les caractères latins courants : on écrit « Coeur » et « Oeil » plutôt
 * qu'avec la ligature, et un tiret simple plutôt que le signe moins.</p>
 */
public abstract class AbstractRelic implements Relic {

    private final String name;
    private final String description;

    /**
     * Crée une relique.
     *
     * @param name        nom affiché au joueur
     * @param description description courte de l'effet
     */
    protected AbstractRelic(String name, String description) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final String description() {
        return description;
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats;
    }

    @Override
    public String toString() {
        return name;
    }
}
