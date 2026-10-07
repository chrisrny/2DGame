package game.model.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests des collisions. */
class CollisionsTest {

    private static final Rect MUR = new Rect(0f, 0f, 10f, 10f);
    private static final Rect MUR_VERTICAL = new Rect(20f, 0f, 10f, 100f);
    private static final Rect MUR_HORIZONTAL = new Rect(0f, 60f, 100f, 10f);
    private static final Circle CORPS = cercle(10f, 50f, 8f);

    private static Circle cercle(float x, float y, float rayon) {
        return new Circle(new Vec2(x, y), rayon);
    }

    @Test
    void unCercleDansLeRectangleLeChevauche() {
        assertTrue(Collisions.overlaps(cercle(5f, 5f, 2f), MUR));
    }

    @Test
    void unCercleQuiDepasseSurLeBordLeChevauche() {
        assertTrue(Collisions.overlaps(cercle(11f, 5f, 2f), MUR));
    }

    @Test
    void unCercleQuiToucheJusteLeBordNeLeChevauchePas() {
        assertFalse(Collisions.overlaps(cercle(12f, 5f, 2f), MUR));
    }

    @Test
    void unCercleLoinNeLeChevauchePas() {
        assertFalse(Collisions.overlaps(cercle(50f, 50f, 2f), MUR));
    }

    @Test
    void unCercleEnFaceDuCoinSansLeToucherNeLeChevauchePas() {
        assertFalse(Collisions.overlaps(cercle(12f, 12f, 2f), MUR));
    }

    @Test
    void unCercleQuiMordSurLeCoinLeChevauche() {
        assertTrue(Collisions.overlaps(cercle(11f, 11f, 2f), MUR));
    }

    @Test
    void deuxCerclesQuiSeChevauchent() {
        assertTrue(Collisions.overlaps(cercle(0f, 0f, 5f), cercle(8f, 0f, 5f)));
    }

    @Test
    void deuxCerclesQuiSeTouchentJusteNeSeChevauchentPas() {
        assertFalse(Collisions.overlaps(cercle(0f, 0f, 5f), cercle(10f, 0f, 5f)));
    }

    @Test
    void deuxCerclesEloignesNeSeChevauchentPas() {
        assertFalse(Collisions.overlaps(cercle(0f, 0f, 5f), cercle(11f, 0f, 5f)));
    }

    @Test
    void unPetitCercleDansUnGrandLeChevauche() {
        assertTrue(Collisions.overlaps(cercle(0f, 0f, 5f), cercle(1f, 1f, 1f)));
    }

    @Test
    void sansMurLeDeplacementEstComplet() {
        assertEquals(new Vec2(15f, 55f), Collisions.move(CORPS, new Vec2(5f, 5f), List.of()));
    }

    @Test
    void enDiagonaleContreUnMurVerticalSeulYAvance() {
        assertEquals(new Vec2(10f, 55f), Collisions.move(CORPS, new Vec2(5f, 5f), List.of(MUR_VERTICAL)));
    }

    @Test
    void enDiagonaleContreUnMurHorizontalSeulXAvance() {
        assertEquals(new Vec2(15f, 50f), Collisions.move(CORPS, new Vec2(5f, 5f), List.of(MUR_HORIZONTAL)));
    }

    @Test
    void dansUnCoinLeCorpsNeBougePas() {
        assertEquals(new Vec2(10f, 50f),
                Collisions.move(CORPS, new Vec2(5f, 5f), List.of(MUR_VERTICAL, MUR_HORIZONTAL)));
    }

    @Test
    void leCorpsPeutVenirSeCollerAuMur() {
        assertEquals(new Vec2(12f, 50f), Collisions.move(CORPS, new Vec2(2f, 0f), List.of(MUR_VERTICAL)));
    }
}
