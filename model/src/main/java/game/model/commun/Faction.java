package game.model.commun;

/**
 * Camp d'une entité ou d'un coup. Empêche le tir ami : un coup n'est appliqué qu'à une cible de
 * l'autre faction.
 */
public enum Faction {
    /** Le joueur et ses attaques. */
    PLAYER,
    /** Les ennemis, le boss et leurs attaques. */
    ENEMY
}
