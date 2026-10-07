package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import org.junit.jupiter.api.Test;

/** Tests de la partie. */
class RunTest {

    @Test
    void soixantePasDeJeuFontUneSeconde() {
        Run run = new Run(GameConfig.defaults(42L));
        for (int i = 0; i < 60; i++) {
            run.update(PlayerIntent.NONE, FixedStep.DT);
        }
        assertEquals(1f, run.elapsed(), 1e-4f);
    }
}
