package game.model.player.state;

/** Nature de l'état du joueur, vue de l'extérieur du modèle. */
public enum PlayerStateKind {
    /** Immobile. */
    IDLE,
    /** En train de se déplacer. */
    MOVING,
    /** En train d'attaquer : immobile, le coup part au premier pas. */
    ATTACKING,
    /** En train d'esquiver : se déplace vite et ne peut pas être touché. */
    DODGING,
    /** Vient d'être touché : repoussé et sans contrôle pendant un court instant. */
    HURT,
    /** Mort : n'agit plus. */
    DEAD
}
