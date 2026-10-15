package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.event.GameEvent;
import game.model.relic.Relic;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de la montée de niveau dans la partie : choix de relique et partie figée. */
class RunLevelUpTest {

    private static final PlayerIntent A_DROITE =
            new PlayerIntent(new Vec2(1f, 0f), new Vec2(1f, 0f), false, false, false, false);

    private final Run run = new Run(GameConfig.defaults(7L));

    /** Simule des ennemis tués : la map publierait ces événements en retirant les morts. */
    private void tuer(int ennemis) {
        for (int i = 0; i < ennemis; i++) {
            run.events().publish(new GameEvent.EntityDied(Vec2.ZERO, Faction.ENEMY));
        }
        run.update(PlayerIntent.NONE, FixedStep.DT);
    }

    private List<Vec2> positionsDesEnnemis() {
        return run.map().enemies().stream().map(Enemy::position).toList();
    }

    @Test
    void aucunChoixAuDebutDeLaPartie() {
        run.update(PlayerIntent.NONE, FixedStep.DT);
        assertFalse(run.isChoosingRelic());
        assertTrue(run.pendingChoice().isEmpty());
    }

    @Test
    void unNiveauGagneProposeTroisReliques() {
        tuer(5);
        assertEquals(2, run.experience().level());
        assertTrue(run.isChoosingRelic());
        assertEquals(Run.RELIC_CHOICES, run.pendingChoice().size());
    }

    @Test
    void pendantLeChoixLaPartieEstFigee() {
        tuer(5);
        Vec2 joueur = run.player().position();
        List<Vec2> ennemis = positionsDesEnnemis();
        float temps = run.elapsed();
        for (int i = 0; i < 30; i++) {
            run.update(A_DROITE, FixedStep.DT);
        }
        assertEquals(joueur, run.player().position());
        assertEquals(ennemis, positionsDesEnnemis());
        assertEquals(temps, run.elapsed());
    }

    @Test
    void choisirDonneLaReliquePublieRelicChosenEtFaitReprendreLaPartie() {
        List<GameEvent> recus = new ArrayList<>();
        run.events().subscribe(recus::add);
        tuer(5);
        Relic choisie = run.pendingChoice().get(1);
        run.chooseRelic(1);
        assertEquals(List.of(choisie), run.player().relics());
        assertTrue(recus.contains(new GameEvent.RelicChosen(choisie.name())));
        assertFalse(run.isChoosingRelic());
        Vec2 avant = run.player().position();
        run.update(A_DROITE, FixedStep.DT);
        assertTrue(run.player().position().x() > avant.x());
    }

    @Test
    void deuxNiveauxDUnCoupDonnentDeuxChoixLUnApresLAutre() {
        tuer(13);
        assertTrue(run.isChoosingRelic());
        run.chooseRelic(0);
        assertTrue(run.isChoosingRelic());
        run.chooseRelic(0);
        assertFalse(run.isChoosingRelic());
        assertEquals(2, run.player().relics().size());
    }

    @Test
    void leSecondChoixNeReproposePasLaPremiereRelique() {
        tuer(13);
        Relic premiere = run.pendingChoice().get(0);
        run.chooseRelic(0);
        for (Relic proposee : run.pendingChoice()) {
            assertFalse(proposee.getClass().equals(premiere.getClass()));
        }
    }

    @Test
    void laMemeGraineProposeLesMemesReliques() {
        Run autre = new Run(GameConfig.defaults(7L));
        tuer(5);
        for (int i = 0; i < 5; i++) {
            autre.events().publish(new GameEvent.EntityDied(Vec2.ZERO, Faction.ENEMY));
        }
        autre.update(PlayerIntent.NONE, FixedStep.DT);
        assertEquals(run.pendingChoice().stream().map(Relic::name).toList(),
                autre.pendingChoice().stream().map(Relic::name).toList());
    }

    @Test
    void choisirSansChoixEnCoursEstRefuse() {
        assertThrows(IllegalStateException.class, () -> run.chooseRelic(0));
    }

    @Test
    void choisirUneReliqueQuiNExistePasEstRefuse() {
        tuer(5);
        assertThrows(IndexOutOfBoundsException.class, () -> run.chooseRelic(3));
        assertTrue(run.isChoosingRelic());
    }
}
