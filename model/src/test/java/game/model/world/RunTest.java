package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
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

    @Test
    void leJoueurCommenceAuCentreDeLaMap() {
        Run run = new Run(GameConfig.defaults(42L));
        assertEquals(MapLayouts.main().center(), run.player().position());
    }

    @Test
    void leJoueurAvanceAvecLIntention() {
        Run run = new Run(GameConfig.defaults(42L));
        PlayerIntent droite = new PlayerIntent(new Vec2(1f, 0f), new Vec2(1f, 0f), false, false, false, false);
        for (int i = 0; i < 60; i++) {
            run.update(droite, FixedStep.DT);
        }
        float vitesse = run.config().player().speed();
        assertEquals(MapLayouts.main().center().x() + vitesse, run.player().position().x(), 1e-2f);
    }
}
