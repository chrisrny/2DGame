package game.model.commun;

/** Niveau de difficulté. Il multiplie les points de vie et les dégâts des ennemis. */
public enum Difficulty {
    /** Ennemis à 75 %. */
    EASY(0.75f),
    /** Valeurs de référence. */
    NORMAL(1f),
    /** Ennemis à 125 %. */
    HARD(1.25f);

    private final float enemyMultiplier;

    Difficulty(float enemyMultiplier) {
        this.enemyMultiplier = enemyMultiplier;
    }

    /** Multiplicateur appliqué aux points de vie et aux dégâts des ennemis. */
    public float enemyMultiplier() {
        return enemyMultiplier;
    }
}
