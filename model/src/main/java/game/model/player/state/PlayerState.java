package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/**
 * Un état du joueur (pattern State) : repos, déplacement, mort, puis attaque, esquive et coup reçu
 * dans les cartes suivantes.
 */
public interface PlayerState {

    /**
     * Décide de l'état du joueur pour ce pas.
     *
     * @param player le joueur
     * @param intent ce que le joueur veut faire pendant ce pas
     * @return l'état suivant, ou {@code this} pour rester dans cet état
     */
    PlayerState next(Player player, PlayerIntent intent);

    /**
     * Applique les règles de cet état pendant un pas.
     *
     * @param player le joueur
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param world la salle où se trouve le joueur
     * @param dt durée du pas, en secondes
     */
    void act(Player player, PlayerIntent intent, World world, float dt);

    /**
     * Indique si le joueur ignore les coups dans cet état. C'est le joueur qui décide si un coup
     * s'applique (contrat {@code Damageable}) : il demande à son état.
     */
    default boolean invulnerable() {
        return false;
    }

    /** @return la nature de cet état, que la vue utilise pour dessiner le joueur */
    PlayerStateKind kind();
}
