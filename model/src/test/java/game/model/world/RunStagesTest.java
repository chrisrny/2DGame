package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.entity.Boss;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de l'enchaînement des stages dans une partie. */
class RunStagesTest {

    private final Run run = new Run(GameConfig.defaults(42L));

    private static Hit coupMortel(Faction source) {
        return new Hit(100_000, source, false, Vec2.ZERO);
    }

    // Un pas de jeu. Les montées de niveau sont acceptées tout de suite, sinon la partie
    // resterait figée sur le choix de relique.
    private void pas() {
        while (run.isChoosingRelic()) {
            run.chooseRelic(0);
        }
        run.update(PlayerIntent.NONE, FixedStep.DT);
    }

    private void avancer(float secondes) {
        int nombre = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < nombre; i++) {
            pas();
        }
    }

    private void tuerLesEnnemis() {
        for (Enemy ennemi : List.copyOf(run.map().enemies())) {
            if (run.boss().orElse(null) != ennemi) {
                ennemi.receive(coupMortel(Faction.PLAYER));
            }
        }
    }

    // Joue la partie en tuant chaque ennemi dès qu'il apparaît, jusqu'au stage voulu (60 s au plus).
    private void jouerJusquAuStage(int stage) {
        for (int i = 0; i < 3600 && run.stage() < stage; i++) {
            pas();
            tuerLesEnnemis();
        }
        assertEquals(stage, run.stage());
    }

    @Test
    void laPartieCommenceAuStage1AvecSonAnnonce() {
        assertEquals(1, run.stage());
        assertEquals(Run.STAGE_INTRO, run.stageIntroLeft(), 0f);
        assertFalse(run.isOver());
        assertTrue(run.boss().isEmpty());
    }

    @Test
    void stageClearedFaitPasserAuStageSuivantEtLeJoueurGardeSesPv() {
        run.player().receive(new Hit(25, Faction.ENEMY, false, Vec2.ZERO));
        for (int i = 0; i < 3600 && run.stage() == 1; i++) {
            while (run.isChoosingRelic()) {
                run.chooseRelic(0);
            }
            // Une relique peut changer les PV : on compare juste avant et juste après le passage.
            int pv = run.player().hp();
            Vec2 position = run.player().position();
            run.update(PlayerIntent.NONE, FixedStep.DT);
            tuerLesEnnemis();
            if (run.stage() == 2) {
                assertEquals(pv, run.player().hp());
                assertEquals(position, run.player().position());
            }
        }
        assertEquals(2, run.stage());
        assertTrue(run.player().hp() < run.player().maxHp(), "pas de soin entre deux stages");
        assertEquals(Stages.demo().get(0).killsToClear(), run.kills());
        assertEquals(0, run.stageKills());
        assertTrue(run.stageIntroLeft() > 0f);
    }

    @Test
    void aucuneVagueNArrivePendantLAnnonceDuStageSuivant() {
        jouerJusquAuStage(2);
        avancer(Run.STAGE_INTRO - 0.1f);
        assertTrue(run.map().enemies().isEmpty());
        assertTrue(run.stageIntroLeft() > 0f);
        // Puis le stage 2 commence : sa première vague arrive après son propre délai.
        avancer(0.2f + Stages.demo().get(1).waves().get(0).delayBefore());
        assertFalse(run.map().enemies().isEmpty());
    }

    @Test
    void auStage3LeBossApparaitLoinDuJoueur() {
        jouerJusquAuStage(3);
        assertTrue(run.isBossStage());
        avancer(Run.STAGE_INTRO + 0.1f);
        Enemy boss = run.boss().orElseThrow();
        assertInstanceOf(Boss.class, boss);
        assertTrue(run.map().enemies().contains(boss));
        assertFalse(run.isOver());
    }

    @Test
    void laMortDuJoueurFinitLaPartieSansVictoire() {
        run.player().receive(coupMortel(Faction.ENEMY));
        pas();
        assertTrue(run.isOver());
        assertFalse(run.isWon());
    }

    @Test
    void laMortDuBossGagneLaPartie() {
        jouerJusquAuStage(3);
        avancer(Run.STAGE_INTRO + 0.1f);
        run.boss().orElseThrow().receive(coupMortel(Faction.PLAYER));
        pas();
        assertTrue(run.isWon());
        assertTrue(run.isOver());
        assertEquals(3, run.stage());
    }
}
