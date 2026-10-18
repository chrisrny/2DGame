package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests des stages de la démo. */
class StagesTest {

    private final List<StageConfig> stages = Stages.demo();

    private static int nombre(Wave vague, EnemyType type) {
        return vague.enemies().getOrDefault(type, 0);
    }

    @Test
    void laDemoATroisStagesNumerotesDansLOrdre() {
        assertEquals(3, stages.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, stages.get(i).number());
        }
    }

    @Test
    void leStage1CommenceParDesRodeursPuisAjouteDesArchersSansChargeur() {
        List<Wave> vagues = stages.get(0).waves();
        assertEquals(0, nombre(vagues.get(0), EnemyType.ARCHER));
        assertTrue(nombre(vagues.get(0), EnemyType.PROWLER) > 0);
        assertTrue(vagues.stream().anyMatch(v -> nombre(v, EnemyType.ARCHER) > 0));
        assertTrue(vagues.stream().allMatch(v -> nombre(v, EnemyType.CHARGER) == 0));
        assertEquals(1f, stages.get(0).multiplier(), 0f);
    }

    @Test
    void leStage2EstPlusDurAvecDesChargeurs() {
        StageConfig stage1 = stages.get(0);
        StageConfig stage2 = stages.get(1);
        assertEquals(Stages.STAGE_2_MULTIPLIER, stage2.multiplier(), 1e-6f);
        assertTrue(stage2.multiplier() > 1f, "le stage 2 renforce les ennemis");
        assertTrue(stage2.waves().stream().anyMatch(v -> nombre(v, EnemyType.CHARGER) > 0));
        assertTrue(stage2.killsToClear() > stage1.killsToClear());
    }

    @Test
    void leStage3EstCeluiDuBossSansVague() {
        StageConfig stage3 = stages.get(2);
        assertTrue(stage3.waves().isEmpty());
        assertEquals(1, stage3.killsToClear());
    }

    @Test
    void lesEnnemisATuerNeDepassentPasCeuxDesVagues() {
        for (StageConfig stage : stages) {
            int total = stage.waves().stream().mapToInt(Wave::size).sum();
            assertFalse(!stage.waves().isEmpty() && stage.killsToClear() > total);
        }
    }

    @Test
    void scaledMultiplieLeMultiplicateur() {
        assertEquals(Stages.STAGE_2_MULTIPLIER * 1.25f, stages.get(1).scaled(1.25f).multiplier(), 1e-6f);
        assertEquals(stages.get(1).waves(), stages.get(1).scaled(1.25f).waves());
    }

    @Test
    void unStageInvalideEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new StageConfig(0, List.of(), 1, 1f));
        assertThrows(IllegalArgumentException.class, () -> new StageConfig(1, List.of(), 0, 1f));
        assertThrows(IllegalArgumentException.class, () -> new StageConfig(1, List.of(), 1, 0f));
    }
}
