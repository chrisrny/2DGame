package game.model.commun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests du vecteur 2D. */
class Vec2Test {

    private static final float EPSILON = 1e-5f;

    @Test
    void addAdditionneLesComposantes() {
        assertEquals(new Vec2(4f, 6f), new Vec2(1f, 2f).add(new Vec2(3f, 4f)));
    }

    @Test
    void lengthDonneLaNorme() {
        assertEquals(5f, new Vec2(3f, 4f).length(), EPSILON);
    }

    @Test
    void normalizedDonneUneLongueurDeUn() {
        Vec2 direction = new Vec2(3f, 4f).normalized();
        assertEquals(1f, direction.length(), EPSILON);
        assertEquals(0.6f, direction.x(), EPSILON);
        assertEquals(0.8f, direction.y(), EPSILON);
    }

    @Test
    void normalizedDuVecteurNulRenvoieZero() {
        assertEquals(Vec2.ZERO, Vec2.ZERO.normalized());
    }

    @Test
    void fromAxesEnDiagonaleNeVaPasPlusVite() {
        assertEquals(1f, Vec2.fromAxes(1, 1).length(), EPSILON);
    }

    @Test
    void fromAxesSansToucheRenvoieZero() {
        assertEquals(Vec2.ZERO, Vec2.fromAxes(0, 0));
    }

    @Test
    void distanceToMesureLaDistanceEntreDeuxPoints() {
        assertEquals(5f, new Vec2(1f, 1f).distanceTo(new Vec2(4f, 5f)), EPSILON);
    }

    @Test
    void dotDeDeuxDirectionsPerpendiculairesVautZero() {
        assertEquals(0f, new Vec2(1f, 0f).dot(new Vec2(0f, 1f)), EPSILON);
    }
}
