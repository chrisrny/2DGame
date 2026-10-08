package game.model.player.state;

/**
 * Nature de l'état du joueur, vue de l'extérieur du modèle.
 *
 * <p>La vue dessine le joueur selon cette valeur, sans connaître les classes d'état.
 * Chaque nouvel état ajoute sa valeur (attaque en P1-A3, esquive en P1-A4, coup reçu en P1-A5).</p>
 */
public enum PlayerStateKind {
    /** Immobile. */
    IDLE,
    /** En train de se déplacer. */
    MOVING,
    /** Mort : n'agit plus. */
    DEAD
}
