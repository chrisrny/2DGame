package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import game.model.combat.Hit;
import game.model.commun.Difficulty;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Settings;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.entity.Prowler;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de la difficulté choisie dans les options : elle renforce ou affaiblit les ennemis créés. */
class RunDifficultyTest {

    // Écart toléré sur un multiplicateur calculé en float.
    private static final float PRECISION = 1e-5f;

    private final Settings reglages = new Settings(Difficulty.NORMAL);
    private final Run run = new Run(GameConfig.defaults(42L), reglages);

    // Un pas de jeu, en acceptant tout de suite les montées de niveau (sinon la partie reste figée).
    private void pas() {
        while (run.isChoosingRelic()) {
            run.chooseRelic(0);
        }
        run.update(PlayerIntent.NONE, FixedStep.DT);
    }

    // Avance jusqu'à ce que des ennemis soient sur la map (10 s au plus).
    private List<Enemy> attendreDesEnnemis() {
        for (int i = 0; i < 600 && run.map().enemies().isEmpty(); i++) {
            pas();
        }
        assertFalse(run.map().enemies().isEmpty(), "une vague est arrivée");
        return run.map().enemies();
    }

    private void jouerJusquAuStage(int stage) {
        for (int i = 0; i < 3600 && run.stage() < stage; i++) {
            pas();
            for (Enemy ennemi : List.copyOf(run.map().enemies())) {
                ennemi.receive(new Hit(100_000, Faction.PLAYER, false, Vec2.ZERO));
            }
        }
        assertEquals(stage, run.stage());
    }

    @Test
    void chaqueDifficulteMultipliePvEtDegatsDesEnnemisCrees() {
        for (Difficulty difficulte : Difficulty.values()) {
            Run partie = new Run(GameConfig.defaults(42L), new Settings(difficulte));
            for (int i = 0; i < 600 && partie.map().enemies().isEmpty(); i++) {
                partie.update(PlayerIntent.NONE, FixedStep.DT);
            }
            float multiplicateur = difficulte.enemyMultiplier();
            for (Enemy ennemi : partie.map().enemies()) {
                assertEquals(multiplicateur, ennemi.multiplier(), PRECISION, difficulte.name());
                if (ennemi instanceof Prowler) {
                    assertEquals(Math.round(Prowler.MAX_HP * multiplicateur), ennemi.maxHp(), difficulte.name());
                    assertEquals(Math.round(Prowler.CONTACT_DAMAGE * multiplicateur), ennemi.contactDamage(),
                            difficulte.name());
                }
            }
        }
    }

    @Test
    void uneDifficulteChangeeEnPleinStageNeVautQuAuStageSuivant() {
        attendreDesEnnemis();
        reglages.setDifficulty(Difficulty.HARD);

        // Le stage 1 a commencé en normal : ses ennemis le restent.
        pas();
        for (Enemy ennemi : run.map().enemies()) {
            assertEquals(1f, ennemi.multiplier(), PRECISION);
        }

        jouerJusquAuStage(2);
        float attendu = Stages.demo().get(1).multiplier() * Difficulty.HARD.enemyMultiplier();
        for (Enemy ennemi : attendreDesEnnemis()) {
            assertEquals(attendu, ennemi.multiplier(), PRECISION);
        }
    }

    @Test
    void leBossSuitLaDifficulteDuStage3() {
        reglages.setDifficulty(Difficulty.EASY);
        jouerJusquAuStage(3);
        for (int i = 0; i < 600 && run.boss().isEmpty(); i++) {
            pas();
        }

        assertEquals(Difficulty.EASY.enemyMultiplier(), run.boss().orElseThrow().multiplier(), PRECISION);
    }
}
