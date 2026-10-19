package game.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import game.model.commun.Vec2;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de l'aide à la visée. */
class AimAssistTest {

    private static final Vec2 ORIGINE = Vec2.ZERO;
    private static final Vec2 DROITE = new Vec2(1f, 0f);

    @Test
    void sansEnnemiLaViseeNeChangePas() {
        assertEquals(DROITE, AimAssist.adjust(DROITE, ORIGINE, List.of()));
    }

    @Test
    void unEnnemiDansLeConeAttireLaVisee() {
        Vec2 ennemi = new Vec2(100f, 30f);
        Vec2 visee = AimAssist.adjust(DROITE, ORIGINE, List.of(ennemi));
        assertEquals(ennemi.normalized().x(), visee.x(), 1e-5f);
        assertEquals(ennemi.normalized().y(), visee.y(), 1e-5f);
    }

    @Test
    void unEnnemiHorsDuConeNeChangeRien() {
        assertEquals(DROITE, AimAssist.adjust(DROITE, ORIGINE, List.of(new Vec2(50f, 80f))));
    }

    @Test
    void unEnnemiTropLoinNeChangeRien() {
        assertEquals(DROITE, AimAssist.adjust(DROITE, ORIGINE, List.of(new Vec2(AimAssist.RANGE + 10f, 0f))));
    }

    @Test
    void leMieuxAligneLEmporte() {
        Vec2 proche = new Vec2(60f, 25f);
        Vec2 aligne = new Vec2(200f, 5f);
        Vec2 visee = AimAssist.adjust(DROITE, ORIGINE, List.of(proche, aligne));
        assertEquals(aligne.normalized().y(), visee.y(), 1e-5f);
    }
}
