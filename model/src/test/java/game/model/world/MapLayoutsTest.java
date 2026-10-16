package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Collisions;
import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests de la map de la démo. */
class MapLayoutsTest {

    private static final MapLayout MAP = MapLayouts.main();
    private static final float CASE = 8f;

    private record Case(int x, int y) {
    }

    private static boolean libre(Circle corps) {
        return MAP.walls().stream().noneMatch(mur -> Collisions.overlaps(corps, mur));
    }

    private static Case caseDe(Vec2 point) {
        return new Case(Math.round(point.x() / CASE), Math.round(point.y() / CASE));
    }

    // Parcours en largeur sur une grille de 8 unités : une case est praticable si un corps de
    // rayon 8, comme le joueur, peut s'y tenir sans toucher de mur.
    private static Set<Case> accessiblesDepuis(Vec2 depart) {
        Set<Case> atteintes = new HashSet<>();
        Queue<Case> aVisiter = new ArrayDeque<>();
        Case debut = caseDe(depart);
        atteintes.add(debut);
        aVisiter.add(debut);
        while (!aVisiter.isEmpty()) {
            Case c = aVisiter.remove();
            for (Case voisine : new Case[] {new Case(c.x() + 1, c.y()), new Case(c.x() - 1, c.y()),
                    new Case(c.x(), c.y() + 1), new Case(c.x(), c.y() - 1)}) {
                Vec2 centre = new Vec2(voisine.x() * CASE, voisine.y() * CASE);
                if (MAP.arena().contains(centre) && !atteintes.contains(voisine) && libre(new Circle(centre, 8f))) {
                    atteintes.add(voisine);
                    aVisiter.add(voisine);
                }
            }
        }
        return atteintes;
    }

    @Test
    void laMapFait3x3Ecrans() {
        assertEquals(new Rect(0f, 0f, 1920f, 1080f), MAP.arena());
    }

    @Test
    void chaquePointDApparitionEstLibre() {
        for (Vec2 point : MAP.spawnPoints()) {
            assertTrue(libre(new Circle(point, 16f)), point.toString());
        }
    }

    @Test
    void leCentreEstLibrePourLeJoueur() {
        assertTrue(libre(new Circle(MAP.center(), 24f)));
    }

    @Test
    void chaquePointDApparitionEstAccessibleDepuisLeCentre() {
        Set<Case> accessibles = accessiblesDepuis(MAP.center());
        for (Vec2 point : MAP.spawnPoints()) {
            assertTrue(accessibles.contains(caseDe(point)), point.toString());
        }
    }

    private static boolean seChevauchent(Rect a, Rect b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.top() && b.y() < a.top();
    }

    @Test
    void lAreneDuBossNaQueLesQuatrePiliersDuDepart() {
        long obstacles = MAP.walls().stream().filter(mur -> seChevauchent(MAP.bossArena(), mur)).count();
        assertEquals(4, obstacles);
        assertTrue(MAP.bossArena().contains(MAP.center()), "le joueur part du centre de l'arène");
    }

    @Test
    void lAreneDuBossADesPointsDApparitionAssezLargesPourLui() {
        List<Vec2> points = MAP.spawnPoints().stream().filter(MAP.bossArena()::contains).toList();
        assertTrue(points.size() >= 2, "le boss peut apparaître loin du joueur, où qu'il soit");
        for (Vec2 point : points) {
            assertTrue(libre(new Circle(point, Boss.RADIUS)), point.toString());
        }
    }
}
