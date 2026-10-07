package game.model.commun;

import java.util.Objects;

/**
 * Cercle : la zone de collision d'un personnage ou d'un projectile, ou une zone d'effet.
 *
 * @param center centre du cercle
 * @param radius rayon, positif ou nul
 */
public record Circle(Vec2 center, float radius) {

    /**
     * Vérifie que le cercle est valide.
     *
     * @throws NullPointerException     si le centre est {@code null}
     * @throws IllegalArgumentException si le rayon est négatif
     */
    public Circle {
        Objects.requireNonNull(center, "center");
        if (radius < 0f) {
            throw new IllegalArgumentException("Un cercle ne peut pas avoir un rayon négatif");
        }
    }

    /**
     * Le même cercle déplacé à un autre centre.
     *
     * @param newCenter le nouveau centre
     * @return un nouveau cercle de même rayon
     */
    public Circle movedTo(Vec2 newCenter) {
        return new Circle(newCenter, radius);
    }
}
