package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.FixedStep;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import game.model.world.World;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests du Rôdeur. */
class ProwlerTest {

    private static final Vec2 JOUEUR = new Vec2(300f, 100f);

    private static FakeWorld monde(Rect... murs) {
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(murs));
        monde.setPlayerPosition(JOUEUR);
        return monde;
    }

    private static void avancer(Enemy ennemi, World monde, float secondes) {
        int pas = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < pas; i++) {
            ennemi.update(monde, FixedStep.DT);
        }
    }

    @Test
    void unRodeurNeufA30PvEtFrappeA10() {
        Prowler rodeur = new Prowler(new Vec2(100f, 100f));
        assertEquals(30, rodeur.hp());
        assertEquals(10, rodeur.contactDamage());
        assertEquals(8f, rodeur.hitbox().radius(), 0f);
    }

    @Test
    void apresUneSecondeLeRodeurSEstRapprocheDe70() {
        Prowler rodeur = new Prowler(new Vec2(100f, 100f));
        avancer(rodeur, monde(), 1f);
        assertEquals(130f, rodeur.position().distanceTo(JOUEUR), 0.01f);
    }

    @Test
    void leRodeurNeTraversePasUnMur() {
        Prowler rodeur = new Prowler(new Vec2(100f, 100f));
        avancer(rodeur, monde(new Rect(120f, 0f, 10f, 360f)), 2f);
        assertTrue(rodeur.position().x() < 120f);
    }
}
