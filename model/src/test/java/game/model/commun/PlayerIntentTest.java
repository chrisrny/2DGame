package game.model.commun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests des règles de l'intention du joueur. */
class PlayerIntentTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void unDeplacementTropLongEstRameneAUn() {
        PlayerIntent intent = new PlayerIntent(new Vec2(3f, 4f), new Vec2(1f, 0f), false, false, false, false);
        assertEquals(1f, intent.move().length(), EPSILON);
    }

    @Test
    void unDeplacementPartielEstConserve() {
        PlayerIntent intent = new PlayerIntent(new Vec2(0.5f, 0f), new Vec2(1f, 0f), false, false, false, false);
        assertEquals(0.5f, intent.move().length(), EPSILON);
    }

    @Test
    void laViseeEstNormalisee() {
        PlayerIntent intent = new PlayerIntent(Vec2.ZERO, new Vec2(0f, 10f), false, false, false, false);
        assertEquals(new Vec2(0f, 1f), intent.aim());
    }

    @Test
    void uneViseeNulleRegardeADroite() {
        PlayerIntent intent = new PlayerIntent(Vec2.ZERO, Vec2.ZERO, false, false, false, false);
        assertEquals(new Vec2(1f, 0f), intent.aim());
    }
}
