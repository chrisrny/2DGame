package game.model.commun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests de l'horloge à pas fixe. */
class FixedStepTest {

    @Test
    void uneImageA60ImagesParSecondeFaitUnPas() {
        assertEquals(1, new FixedStep().advance(FixedStep.DT));
    }

    @Test
    void uneImageTropCourteNeFaitAucunPasMaisGardeLeTemps() {
        FixedStep clock = new FixedStep();
        assertEquals(0, clock.advance(0.010f));
        assertEquals(1, clock.advance(0.010f));
    }

    @Test
    void uneImageLenteFaitPlusieursPas() {
        assertEquals(3, new FixedStep().advance(0.06f));
    }

    @Test
    void unGrosRetardEstPlafonneEtAbandonne() {
        FixedStep clock = new FixedStep();
        assertEquals(FixedStep.MAX_STEPS, clock.advance(1f));
        assertEquals(0f, clock.pending());
        assertEquals(0, clock.advance(0f));
    }
}
