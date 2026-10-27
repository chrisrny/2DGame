package game.model.commun;

/**
 * Rectangle aligné sur les axes : un mur, un obstacle, une porte ou l'arène entière.
 *
 * @param x abscisse du coin bas gauche
 * @param y ordonnée du coin bas gauche
 * @param width largeur, positive ou nulle
 * @param height hauteur, positive ou nulle
 */
public record Rect(float x, float y, float width, float height) {

    /**
     * Vérifie que le rectangle a une taille valide.
     *
     * @throws IllegalArgumentException si la largeur ou la hauteur est négative
     */
    public Rect {
        if (width < 0f || height < 0f) {
            throw new IllegalArgumentException("Un rectangle ne peut pas avoir une taille négative");
        }
    }

    /** Abscisse du bord droit. */
    public float right() {
        return x + width;
    }

    /** Ordonnée du bord haut. */
    public float top() {
        return y + height;
    }

    /**
     * Indique si un point est à l'intérieur du rectangle (bords compris).
     *
     * @param point le point à tester
     * @return {@code true} si le point est dans le rectangle
     */
    public boolean contains(Vec2 point) {
        return point.x() >= x && point.x() <= right() && point.y() >= y && point.y() <= top();
    }
}
