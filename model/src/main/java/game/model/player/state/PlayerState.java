package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/**
 * Un état du joueur (pattern State) : repos, déplacement, mort, puis attaque, esquive et
 * coup reçu dans les cartes suivantes.
 *
 * <p>À chaque pas de jeu, le joueur demande d'abord à son état courant quel est l'état suivant
 * ({@link #next}), puis fait agir cet état ({@link #act}). Décider avant d'agir permet qu'un
 * appui soit pris en compte dès le pas où il arrive.</p>
 *
 * <p>Chaque entrée dans un état crée un nouvel objet : les états temporisés (attaque, esquive)
 * gardent ainsi leur propre chronomètre.</p>
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
     * @param world  la salle où se trouve le joueur
     * @param dt     durée du pas, en secondes
     */
    void act(Player player, PlayerIntent intent, World world, float dt);

    /** @return la nature de cet état, que la vue utilise pour dessiner le joueur */
    PlayerStateKind kind();
}
