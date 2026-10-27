package game.model.relic;

import game.model.commun.Circle;
import game.model.commun.PlayerStats;
import game.model.entity.Player;
import game.model.world.World;
import java.util.List;

/** Une relique : une amélioration que le joueur garde jusqu'à la fin de la partie. */
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
     * Appelé à chaque cible que le joueur touche. Par défaut, ne fait rien : seules les reliques
     * qui réagissent aux coups redéfinissent cette méthode.
     *
     * @param player le joueur qui vient de toucher une cible
     */
    default void onHit(Player player) {
        // Rien par défaut.
    }

    /**
     * Appelé à chaque attaque du joueur, qu'elle touche ou non. Par défaut, ne fait rien.
     *
     * @param player le joueur qui vient d'attaquer
     */
    default void onAttack(Player player) {
        // Rien par défaut.
    }

    /**
     * Appelé au début de chaque esquive du joueur. Par défaut, ne fait rien.
     *
     * @param player le joueur qui commence à esquiver
     */
    default void onDodge(Player player) {
        // Rien par défaut.
    }

    /**
     * Appelé à chaque pas de jeu, après l'action du joueur, pour les effets qui durent. Par défaut,
     * ne fait rien.
     *
     * @param player le joueur qui possède la relique
     * @param world le monde, pour ses cibles et son bus d'événements
     * @param dt durée du pas, en secondes
     */
    default void update(Player player, World world, float dt) {
        // Rien par défaut.
    }

    /** Zones de feu posées au sol par la relique, que la vue dessine. Par défaut, aucune. */
    default List<Circle> fireZones() {
        return List.of();
    }
}
