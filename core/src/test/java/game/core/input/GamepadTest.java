package game.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.core.input.Gamepad.Axis;
import game.core.input.Gamepad.Button;
import game.core.input.Gamepad.Direction;
import game.model.commun.Vec2;
import org.junit.jupiter.api.Test;

/** Tests de la mise en forme commune des manettes : appuis, zone morte, directions de menu. */
class GamepadTest {

    private final FakeGamepad pad = new FakeGamepad();

    @Test
    void unAppuiNeCompteQuALImageOuIlCommence() {
        pad.press(Button.A).update();
        assertTrue(pad.justPressed(Button.A));
        assertTrue(pad.held(Button.A));
        pad.update();
        assertFalse(pad.justPressed(Button.A), "maintenu, ce n'est plus un nouvel appui");
        assertTrue(pad.held(Button.A));
        pad.release(Button.A).update();
        assertFalse(pad.held(Button.A));
    }

    @Test
    void unStickDansLaZoneMorteVautZero() {
        pad.set(Axis.LEFT_X, 0.15f).set(Axis.LEFT_Y, 0.1f).update();
        assertEquals(Vec2.ZERO, pad.stick(true));
        assertFalse(pad.active(), "un stick qui dérive ne réveille pas la manette");
    }

    @Test
    void laCourseApresLaZoneMorteEstEtaleeDeZeroAUn() {
        pad.set(Axis.LEFT_X, 1f).update();
        assertEquals(1f, pad.stick(true).length(), 1e-5f);
        float milieu = (1f + Gamepad.DEAD_ZONE) / 2f;
        pad.set(Axis.LEFT_X, milieu).update();
        assertEquals(0.5f, pad.stick(true).length(), 1e-5f);
    }

    @Test
    void uneDiagonalePousseeAFondNeDepassePasUn() {
        pad.set(Axis.RIGHT_X, 1f).set(Axis.RIGHT_Y, 1f).update();
        assertEquals(1f, pad.stick(false).length(), 1e-5f);
    }

    @Test
    void laGachetteCompteAuDelaDuSeuil() {
        pad.set(Axis.RIGHT_TRIGGER, 0.3f).update();
        assertFalse(pad.trigger(Axis.RIGHT_TRIGGER));
        pad.set(Axis.RIGHT_TRIGGER, 0.8f).update();
        assertTrue(pad.trigger(Axis.RIGHT_TRIGGER));
    }

    @Test
    void leStickFaitUnSeulPasDansLesMenusTantQuIlNEstPasRelache() {
        pad.set(Axis.LEFT_Y, -0.9f).update();
        assertTrue(pad.justPressed(Direction.DOWN));
        pad.set(Axis.LEFT_Y, -0.5f).update();
        assertFalse(pad.justPressed(Direction.DOWN), "encore poussé : pas de second pas");
        pad.set(Axis.LEFT_Y, -0.2f).update();
        pad.set(Axis.LEFT_Y, -0.9f).update();
        assertTrue(pad.justPressed(Direction.DOWN), "relâché puis repoussé : un nouveau pas");
    }

    @Test
    void laCroixDonneAussiLesDirections() {
        pad.press(Button.DPAD_LEFT).update();
        assertTrue(pad.justPressed(Direction.LEFT));
        assertFalse(pad.justPressed(Direction.RIGHT));
    }

    @Test
    void uneManetteDebrancheeNeRenvoieRien() {
        pad.press(Button.A).set(Axis.LEFT_X, 1f);
        pad.plugged = false;
        pad.update();
        assertFalse(pad.connected());
        assertFalse(pad.held(Button.A));
        assertEquals(Vec2.ZERO, pad.stick(true));
        assertFalse(pad.active());
    }

    @Test
    void sansManetteRienNEstJamaisEnfonce() {
        Gamepad.NONE.update();
        assertFalse(Gamepad.NONE.connected());
        assertFalse(Gamepad.NONE.justPressed(Button.START));
    }
}
