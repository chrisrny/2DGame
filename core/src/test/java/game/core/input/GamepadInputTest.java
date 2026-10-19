package game.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.core.input.Gamepad.Axis;
import game.core.input.Gamepad.Button;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests des entrées manette : déplacement, visée, boutons d'action. */
class GamepadInputTest {

    private static final Vec2 JOUEUR = new Vec2(100f, 100f);

    private final FakeGamepad pad = new FakeGamepad();
    private final List<Vec2> ennemis = new ArrayList<>();
    private final GamepadInput entrees = new GamepadInput(pad, () -> ennemis);

    private PlayerIntent lire() {
        pad.update();
        return entrees.poll(JOUEUR);
    }

    @Test
    void leStickGaucheDeplaceDeFaconAnalogique() {
        pad.set(Axis.LEFT_X, (1f + Gamepad.DEAD_ZONE) / 2f);
        PlayerIntent intention = lire();
        assertEquals(0.5f, intention.move().length(), 1e-4f);
        assertTrue(intention.move().x() > 0f);
    }

    @Test
    void laCroixDeplaceAussi() {
        pad.press(Button.DPAD_UP);
        assertEquals(new Vec2(0f, 1f), lire().move());
    }

    @Test
    void leStickDroitVise() {
        pad.set(Axis.RIGHT_Y, -1f);
        assertEquals(-1f, lire().aim().y(), 1e-4f);
    }

    @Test
    void sansStickDroitOnViseLaOuLOnMarche() {
        pad.set(Axis.LEFT_X, -1f);
        assertEquals(-1f, lire().aim().x(), 1e-4f);
    }

    @Test
    void immobileOnGardeLaDerniereVisee() {
        pad.set(Axis.LEFT_Y, 1f);
        lire();
        pad.set(Axis.LEFT_Y, 0f);
        assertEquals(1f, lire().aim().y(), 1e-4f);
    }

    @Test
    void lesBoutonsDuDepartSontIgnoresJusquAuRelachement() {
        pad.press(Button.A);
        assertFalse(lire().dodge(), "le A qui a lancé la partie ne fait pas esquiver");
        pad.release(Button.A);
        lire();
        pad.press(Button.A);
        assertTrue(lire().dodge());
    }

    @Test
    void chaqueActionASesBoutons() {
        lire();
        pad.press(Button.X).press(Button.RB).press(Button.LB);
        PlayerIntent intention = lire();
        assertTrue(intention.attack());
        assertTrue(intention.skill1());
        assertTrue(intention.skill2());
        assertFalse(intention.dodge());
    }

    @Test
    void lesGachettesAttaquentEtEsquivent() {
        lire();
        pad.set(Axis.RIGHT_TRIGGER, 1f).set(Axis.LEFT_TRIGGER, 1f);
        PlayerIntent intention = lire();
        assertTrue(intention.attack());
        assertTrue(intention.dodge());
    }

    @Test
    void apresUnMenuLeBoutonDoitEtreRelacheAvantDeCompter() {
        lire();
        pad.press(Button.A);
        assertTrue(lire().dodge());
        entrees.ignoreUntilReleased();
        assertFalse(lire().dodge());
    }

    @Test
    void laViseeEstRameneeSurUnEnnemiProche() {
        ennemis.add(JOUEUR.add(new Vec2(100f, 20f)));
        pad.set(Axis.RIGHT_X, 1f);
        Vec2 visee = lire().aim();
        assertTrue(visee.y() > 0.15f, "la visée glisse vers l'ennemi, un peu au-dessus");
    }
}
