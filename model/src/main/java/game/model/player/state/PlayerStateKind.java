package game.model.player.state;

/**
 * Nature de l'état du joueur, vue de l'extérieur du modèle.
 *
 * <p>La vue dessine le joueur selon cette valeur, sans connaître les classes d'état.
 * Chaque nouvel état ajoute sa valeur (coup reçu en P1-A5).</p>
 */
public enum PlayerStateKind {
    /** Immobile. */
    IDLE,
    /** En train de se déplacer. */
    MOVING,
    /** En train d'attaquer : immobile, le coup part au premier pas. */
    ATTACKING,
    /** En train d'esquiver : se déplace vite et ne peut pas être touché. */
    DODGING,
    /** Mort : n'agit plus. */
    DEAD
}
