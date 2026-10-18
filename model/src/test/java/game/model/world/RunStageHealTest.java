package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.GameConfig;
import game.model.commun.Vec2;
import game.model.event.GameEvent;
import org.junit.jupiter.api.Test;

/** Tests du répit entre deux étages : le joueur reprend une part de sa vie. */
class RunStageHealTest {

    @Test
    void finirUnEtageRendUnePartDeLaVie() {
        Run run = new Run(GameConfig.defaults(42L));
        int max = run.player().maxHp();
        run.player().receive(new Hit(50, Faction.ENEMY, false, Vec2.ZERO));
        assertEquals(max - 50, run.player().hp());

        run.events().publish(new GameEvent.StageCleared(1));
        run.events().flush();

        assertEquals(max - 50 + Math.round(max * Run.STAGE_HEAL), run.player().hp());
    }

    @Test
    void leSoinNeDepassePasLaVieMax() {
        Run run = new Run(GameConfig.defaults(42L));
        run.player().receive(new Hit(5, Faction.ENEMY, false, Vec2.ZERO));

        run.events().publish(new GameEvent.StageCleared(1));
        run.events().flush();

        assertEquals(run.player().maxHp(), run.player().hp());
    }
}
