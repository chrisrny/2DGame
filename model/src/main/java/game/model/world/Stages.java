package game.model.world;

import java.util.List;
import java.util.Map;

/**
 * Les stages du jeu, décrits en code.
 *
 * <p>Une partie enchaîne trois stages sur la même map : le stage 1 pour apprendre, le stage 2
 * plus dur, et le stage 3 pour le boss. Les valeurs seront revues pendant l'équilibrage (P2-X1).</p>
 */
public final class Stages {

    /** Multiplicateur des ennemis du stage 2. */
    public static final float STAGE_2_MULTIPLIER = 1.3f;

    private Stages() {
    }

    /**
     * Les trois stages de la démo :
     * <ul>
     *   <li>stage 1 : des Rôdeurs, puis des Rôdeurs et des Archers ;</li>
     *   <li>stage 2 : des vagues plus nombreuses, avec des Chargeurs, ennemis renforcés à 130 % ;</li>
     *   <li>stage 3 : le boss seul. Il n'a pas de vague : c'est la partie qui le fait apparaître
     *       (P1-B7, puis le vrai boss en P2-B1).</li>
     * </ul>
     *
     * @return les stages, dans l'ordre
     */
    public static List<StageConfig> demo() {
        StageConfig stage1 = new StageConfig(1, List.of(
                wave(1.5f, 4, 0, 0),
                wave(2f, 3, 2, 0),
                wave(2f, 4, 3, 0)), 16, 1f);
        StageConfig stage2 = new StageConfig(2, List.of(
                wave(2f, 4, 0, 2),
                wave(2f, 4, 3, 2),
                wave(2f, 5, 3, 3)), 26, STAGE_2_MULTIPLIER);
        StageConfig stage3 = new StageConfig(3, List.of(), 1, 1f);
        return List.of(stage1, stage2, stage3);
    }

    private static Wave wave(float delayBefore, int prowlers, int archers, int chargers) {
        return new Wave(Map.of(
                EnemyType.PROWLER, prowlers,
                EnemyType.ARCHER, archers,
                EnemyType.CHARGER, chargers), delayBefore);
    }
}
