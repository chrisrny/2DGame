package game.model.world;

/** Les types d'ennemis qui peuvent apparaître dans une vague. */
public enum EnemyType {
    /** Le Rôdeur : fonce vers le joueur. */
    PROWLER,
    /** L'Archer : garde ses distances et tire. */
    ARCHER,
    /** Le Chargeur : prévient, charge en ligne droite, puis reste étourdi. */
    CHARGER
}
