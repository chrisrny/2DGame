package game.model.world;

import java.util.List;
import java.util.Map;

/** Les stages du jeu, décrits en code. */
public final class Stages {

    /** Multiplicateur des ennemis du stage 2. */
    public static final float STAGE_2_MULTIPLIER = 1.2f;

    private Stages() {
    }

    /** Les trois stages de la démo. */
    public static List<StageConfig> demo() {
        StageConfig stage1 = new StageConfig(1, List.of(
                wave(1.5f, 4, 0, 0),
                wave(2f, 3, 2, 0),
                wave(2f, 4, 3, 0)), 16, 1f);
        StageConfig stage2 = new StageConfig(2, List.of(
                wave(2f, 4, 1, 1),
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
