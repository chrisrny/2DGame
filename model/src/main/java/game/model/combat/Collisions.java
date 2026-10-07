package game.model.combat;

import game.model.commun.Circle;
import game.model.commun.Rect;

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
}
