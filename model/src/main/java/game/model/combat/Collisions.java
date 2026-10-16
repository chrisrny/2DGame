package game.model.combat;

import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;

/**
 * Collisions entre cercles (personnages, projectiles) et rectangles (murs, obstacles, portes).
 */
public final class Collisions {

    /**
     * Enfoncement toléré dans un mur, en unités. Les calculs en float laissent parfois un cercle
     * collé à un mur avec un chevauchement minuscule : sans cette tolérance, il ne pourrait plus
     * glisser le long et resterait bloqué contre un coin.
     */
    public static final float TOLERANCE = 0.01f;

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
     * <p>Un axe n'est annulé que s'il enfonce le cercle plus profondément dans un mur (au-delà de
     * {@link #TOLERANCE}) : un cercle qui frôle déjà un mur peut toujours glisser le long.</p>
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
        if (goesIntoWall(body, body.movedTo(afterX), walls)) {
            afterX = start;
        }
        Vec2 afterY = new Vec2(afterX.x(), afterX.y() + delta.y());
        if (goesIntoWall(body.movedTo(afterX), body.movedTo(afterY), walls)) {
            afterY = afterX;
        }
        return afterY;
    }

    /**
     * Indique si un cercle peut aller en ligne droite d'un point à un autre sans toucher de mur.
     * Sert à la ligne de vue (rayon 0) et à savoir si un ennemi peut foncer tout droit.
     *
     * <p>Le cercle qui glisse le long du segment touche un mur si le segment traverse ce mur
     * élargi du rayon dans toutes les directions. L'élargissement en rectangle est un peu plus
     * prudent qu'un vrai arrondi aux coins : on refuse parfois un passage qui frôle un angle.</p>
     *
     * @param from   point de départ
     * @param to     point d'arrivée
     * @param radius rayon du cercle qui se déplace (0 pour une simple ligne de vue)
     * @param walls  les rectangles infranchissables
     * @return {@code true} si aucun mur n'est sur le chemin
     */
    public static boolean pathIsClear(Vec2 from, Vec2 to, float radius, List<Rect> walls) {
        for (Rect wall : walls) {
            if (segmentHitsRect(from, to, wall.x() - radius, wall.y() - radius,
                    wall.right() + radius, wall.top() + radius)) {
                return false;
            }
        }
        return true;
    }

    // Algorithme de Liang-Barsky : on rogne le segment [from, to] par les quatre bords du
    // rectangle. S'il en reste un morceau, le segment entre dans le rectangle. Un segment qui
    // ne fait que longer un bord n'y entre pas, comme pour overlaps.
    private static boolean segmentHitsRect(Vec2 from, Vec2 to, float minX, float minY, float maxX, float maxY) {
        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float[] p = {-dx, dx, -dy, dy};
        float[] q = {from.x() - minX, maxX - from.x(), from.y() - minY, maxY - from.y()};
        float enter = 0f;
        float exit = 1f;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0f) {
                if (q[i] <= 0f) {
                    return false;
                }
            } else {
                float t = q[i] / p[i];
                if (p[i] < 0f) {
                    enter = Math.max(enter, t);
                } else {
                    exit = Math.min(exit, t);
                }
            }
        }
        return enter < exit;
    }

    // Un déplacement est refusé s'il enfonce le cercle dans un mur au-delà de la tolérance, et
    // plus profondément qu'avant. Un cercle déjà collé d'un cheveu peut donc encore glisser.
    private static boolean goesIntoWall(Circle before, Circle after, List<Rect> walls) {
        for (Rect wall : walls) {
            float depth = penetration(after, wall);
            if (depth > TOLERANCE && depth > penetration(before, wall)) {
                return true;
            }
        }
        return false;
    }

    // De combien le cercle entre dans le rectangle : négatif s'il est dehors.
    private static float penetration(Circle c, Rect r) {
        float closestX = Math.clamp(c.center().x(), r.x(), r.right());
        float closestY = Math.clamp(c.center().y(), r.y(), r.top());
        float dx = c.center().x() - closestX;
        float dy = c.center().y() - closestY;
        return c.radius() - (float) Math.sqrt(dx * dx + dy * dy);
    }
}
