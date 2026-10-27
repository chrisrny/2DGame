package game.model.combat;

import game.model.commun.Circle;
import java.util.Objects;

/**
 * L'annonce d'une attaque ennemie, visible avant qu'elle frappe : toute attaque du boss en a une,
 * pour que le joueur ait le temps de l'esquiver.
 *
 * @param zone la zone qui va être frappée
 * @param progress avancement de l'annonce, de 0 (elle commence) à 1 (le coup part)
 */
public record Telegraph(Circle zone, float progress) {

    /**
     * Vérifie que l'annonce est valide.
     *
     * @throws NullPointerException si la zone est {@code null}
     * @throws IllegalArgumentException si l'avancement n'est pas entre 0 et 1
     */
    public Telegraph {
        Objects.requireNonNull(zone, "zone");
        if (progress < 0f || progress > 1f) {
            throw new IllegalArgumentException("L'avancement d'une annonce doit être entre 0 et 1");
        }
    }
}
