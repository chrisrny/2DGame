package game.model.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de la ligne de vue et du passage en ligne droite entre les murs. */
class PathIsClearTest {

    private static final List<Rect> MUR = List.of(new Rect(100f, 0f, 20f, 100f));

    @Test
    void unSegmentQuiTraverseLeMurEstBloque() {
        assertFalse(Collisions.pathIsClear(new Vec2(50f, 50f), new Vec2(200f, 50f), 0f, MUR));
    }

    @Test
    void unSegmentQuiPasseAuDessusDuMurEstLibre() {
        assertTrue(Collisions.pathIsClear(new Vec2(50f, 150f), new Vec2(200f, 150f), 0f, MUR));
    }

    @Test
    void unCercleQuiFroleLeMurNePassePas() {
        // La ligne passe 5 unités au-dessus du mur : libre pour un point, pas pour un cercle de rayon 8.
        Vec2 depart = new Vec2(50f, 105f);
        Vec2 arrivee = new Vec2(200f, 105f);
        assertTrue(Collisions.pathIsClear(depart, arrivee, 0f, MUR));
        assertFalse(Collisions.pathIsClear(depart, arrivee, 8f, MUR));
    }

    @Test
    void unSegmentEnDiagonaleQuiCoupeLeCoinEstBloque() {
        assertFalse(Collisions.pathIsClear(new Vec2(90f, 80f), new Vec2(130f, 120f), 0f, MUR));
    }

    @Test
    void unSegmentDeLongueurNulleHorsDuMurEstLibre() {
        Vec2 point = new Vec2(50f, 50f);
        assertTrue(Collisions.pathIsClear(point, point, 0f, MUR));
    }
}
