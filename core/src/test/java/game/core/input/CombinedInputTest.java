package game.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import org.junit.jupiter.api.Test;

/** Tests de la combinaison clavier + manette. */
class CombinedInputTest {

    private static final Vec2 HAUT = new Vec2(0f, 1f);
    private static final Vec2 GAUCHE = new Vec2(-1f, 0f);

    private boolean manette;

    private CombinedInput combiner(PlayerIntent clavier, PlayerIntent pad) {
        return new CombinedInput(p -> clavier, p -> pad, () -> manette);
    }

    @Test
    void lesBoutonsDesDeuxSourcesSAdditionnent() {
        PlayerIntent clavier = new PlayerIntent(Vec2.ZERO, HAUT, true, false, false, false);
        PlayerIntent pad = new PlayerIntent(Vec2.ZERO, GAUCHE, false, false, false, true);
        PlayerIntent intention = combiner(clavier, pad).poll(Vec2.ZERO);
        assertTrue(intention.attack());
        assertTrue(intention.skill2());
    }

    @Test
    void leClavierDeplaceEnPrioriteSinonLaManette() {
        PlayerIntent pad = new PlayerIntent(GAUCHE, GAUCHE, false, false, false, false);
        assertEquals(GAUCHE, combiner(PlayerIntent.NONE, pad).poll(Vec2.ZERO).move());
        PlayerIntent clavier = new PlayerIntent(HAUT, HAUT, false, false, false, false);
        assertEquals(HAUT, combiner(clavier, pad).poll(Vec2.ZERO).move());
    }

    @Test
    void laViseeVientDeLaDerniereSourceUtilisee() {
        PlayerIntent clavier = new PlayerIntent(Vec2.ZERO, HAUT, false, false, false, false);
        PlayerIntent pad = new PlayerIntent(Vec2.ZERO, GAUCHE, false, false, false, false);
        CombinedInput entrees = combiner(clavier, pad);
        assertEquals(HAUT, entrees.poll(Vec2.ZERO).aim());
        manette = true;
        assertEquals(GAUCHE, entrees.poll(Vec2.ZERO).aim());
    }
}
