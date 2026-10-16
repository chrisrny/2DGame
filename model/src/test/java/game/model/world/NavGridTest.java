package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Collisions;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de la grille de navigation. */
class NavGridTest {

    // Arène de 400x400 coupée par un mur vertical, avec un passage seulement en haut (y > 300).
    private static final Rect ARENE = new Rect(0f, 0f, 400f, 400f);
    private static final List<Rect> MURS = List.of(new Rect(190f, 0f, 20f, 300f));
    private static final Vec2 CIBLE = new Vec2(300f, 100f);

    private final NavGrid grille = new NavGrid(ARENE, MURS, 8f);

    @Test
    void lesCasesDansOuContreLeMurNeSontPasPraticables() {
        assertFalse(grille.isWalkable(new Vec2(200f, 100f)));
        assertFalse(grille.isWalkable(new Vec2(185f, 100f)));
        assertTrue(grille.isWalkable(new Vec2(100f, 100f)));
    }

    @Test
    void lePointViseFaitContournerLeMurParLeHaut() {
        grille.updateTarget(CIBLE);
        Vec2 depart = new Vec2(100f, 100f);
        Vec2 point = grille.nextWaypoint(depart, 8f, CIBLE);
        assertTrue(point.y() > depart.y(), "il faut monter vers le passage");
        assertTrue(Collisions.pathIsClear(depart, point, 8f, MURS), "le point visé est atteignable tout droit");
    }

    @Test
    void enSuivantLesPointsVisesOnArriveALaCible() {
        grille.updateTarget(CIBLE);
        Vec2 position = new Vec2(100f, 100f);
        for (int i = 0; i < 2000 && position.distanceTo(CIBLE) > 1f; i++) {
            Vec2 point = grille.nextWaypoint(position, 8f, CIBLE);
            float pas = Math.min(2f, position.distanceTo(point));
            position = Collisions.move(new game.model.commun.Circle(position, 8f),
                    point.sub(position).normalized().scale(pas), MURS);
        }
        assertEquals(0f, position.distanceTo(CIBLE), 1f);
    }

    @Test
    void uneCibleInaccessibleEstViseeTelleQuelle() {
        // Mur complet : aucun chemin. La grille renvoie la cible, l'ennemi glissera contre le mur.
        List<Rect> murComplet = List.of(new Rect(190f, 0f, 20f, 400f));
        NavGrid fermee = new NavGrid(ARENE, murComplet, 8f);
        fermee.updateTarget(CIBLE);
        assertFalse(fermee.canReachTarget(new Vec2(100f, 100f)));
        assertEquals(CIBLE, fermee.nextWaypoint(new Vec2(100f, 100f), 8f, CIBLE));
    }
}
