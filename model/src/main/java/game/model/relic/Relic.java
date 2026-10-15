package game.model.relic;

import game.model.commun.PlayerStats;
import game.model.entity.Player;

/**
 * Une relique : une amélioration que le joueur garde jusqu'à la fin de la partie.
 *
 * <p>Une relique ne modifie jamais les statistiques qu'on lui donne : {@link #apply(PlayerStats)}
 * en renvoie de nouvelles. Le joueur applique ses reliques une par une sur ses statistiques de
 * base pour obtenir ses statistiques réelles. Une relique peut aussi réagir quand le joueur
 * touche une cible ({@link #onHit(Player)}) ; par défaut, elle ne fait rien.</p>
 */
public interface Relic {

    /** @return le nom affiché au joueur */
    String name();

    /** @return la description courte de l'effet, affichée au joueur */
    String description();

    /**
     * Applique l'effet de la relique sur des statistiques.
     *
     * @param stats les statistiques avant la relique
     * @return de nouvelles statistiques ; celles reçues ne sont pas modifiées
     */
    PlayerStats apply(PlayerStats stats);

    /**
     * Appelé à chaque cible que le joueur touche. Par défaut, ne fait rien : seules les
     * reliques qui réagissent aux coups redéfinissent cette méthode.
     *
     * @param player le joueur qui vient de toucher une cible
     */
    default void onHit(Player player) {
        // Rien par défaut.
    }
}
