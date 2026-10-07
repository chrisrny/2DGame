package game.model.combat;

import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;

/**
 * Collisions entre cercles (personnages, projectiles) et rectangles (murs, obstacles, portes).
 */
public final class Collisions {

    private Collisions() {
    }

    /**
     * Indique si un cercle chevauche un rectangle. Un cercle qui touche juste le bord
     * ne le chevauche pas : il peut glisser le long.
     *
     * @param c le cercle
     * @param r le rectangle
     * @return {@code true} si le cercle entre dans le rectangle
     */
    public static boolean overlaps(Circle c, Rect r) {
        float closestX = Math.clamp(c.center().x(), r.x(), r.right());
        float closestY = Math.clamp(c.center().y(), r.y(), r.top());
        float dx = c.center().x() - closestX;
        float dy = c.center().y() - closestY;
        return dx * dx + dy * dy < c.radius() * c.radius();
    }

    /**
     * Indique si deux cercles se chevauchent. Deux cercles qui se touchent juste
     * ne se chevauchent pas.
     *
     * @param a le premier cercle
     * @param b le second cercle
     * @return {@code true} si les cercles se chevauchent
     */
    public static boolean overlaps(Circle a, Circle b) {
        float dx = a.center().x() - b.center().x();
        float dy = a.center().y() - b.center().y();
        float radii = a.radius() + b.radius();
        return dx * dx + dy * dy < radii * radii;
    }

    /**
     * Déplace un cercle en le faisant glisser le long des murs au lieu de l'arrêter net.
     *
     * <p>Le déplacement est appliqué en x puis en y ; chaque axe est annulé s'il fait entrer
     * le cercle dans un mur. En diagonale contre un mur vertical, x est bloqué mais y continue.</p>
     *
     * <p>Hypothèses : un déplacement par pas reste plus petit que le rayon, sinon le cercle peut
     * traverser un mur fin ; un cercle qui démarre dans un mur peut y rester bloqué.</p>
     *
     * @param body  le cercle à déplacer
     * @param delta le déplacement voulu pendant ce pas
     * @param walls les rectangles infranchissables
     * @return le nouveau centre du cercle
     */
    public static Vec2 move(Circle body, Vec2 delta, List<Rect> walls) {
        Vec2 start = body.center();
        Vec2 afterX = new Vec2(start.x() + delta.x(), start.y());
        if (hitsAnyWall(body.movedTo(afterX), walls)) {
            afterX = start;
        }
        Vec2 afterY = new Vec2(afterX.x(), afterX.y() + delta.y());
        if (hitsAnyWall(body.movedTo(afterY), walls)) {
            afterY = afterX;
        }
        return afterY;
    }

    private static boolean hitsAnyWall(Circle c, List<Rect> walls) {
        return walls.stream().anyMatch(wall -> overlaps(c, wall));
    }
}
