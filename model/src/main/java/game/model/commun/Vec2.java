package game.model.commun;

/**
 * Vecteur 2D immuable : une position, une vitesse ou une direction dans le monde du jeu.
 *
 * @param x composante horizontale
 * @param y composante verticale
 */
public record Vec2(float x, float y) {

    /** Le vecteur nul (0, 0). */
    public static final Vec2 ZERO = new Vec2(0f, 0f);

    /**
     * Direction normalisée à partir de deux axes, comme ceux du clavier.
     *
     * @param x -1 (gauche), 0 ou 1 (droite)
     * @param y -1 (bas), 0 ou 1 (haut)
     * @return la direction normalisée, ou {@link #ZERO} si les deux axes valent 0
     */
    public static Vec2 fromAxes(int x, int y) {
        return new Vec2(x, y).normalized();
    }

    /**
     * Somme de deux vecteurs.
     *
     * @param other le vecteur à ajouter
     * @return {@code this + other}
     */
    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    /**
     * Différence de deux vecteurs.
     *
     * @param other le vecteur à soustraire
     * @return {@code this - other}
     */
    public Vec2 sub(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    /**
     * Multiplication par un nombre.
     *
     * @param factor le facteur
     * @return le vecteur multiplié
     */
    public Vec2 scale(float factor) {
        return new Vec2(x * factor, y * factor);
    }

    /** Longueur (norme) du vecteur. */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Distance entre deux points.
     *
     * @param other l'autre point
     * @return la distance entre {@code this} et {@code other}
     */
    public float distanceTo(Vec2 other) {
        return sub(other).length();
    }

    /**
     * Produit scalaire.
     *
     * @param other l'autre vecteur
     * @return le produit scalaire
     */
    public float dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    /** Le même vecteur ramené à une longueur de 1. */
    public Vec2 normalized() {
        float length = length();
        if (length == 0f) {
            return ZERO;
        }
        return new Vec2(x / length, y / length);
    }

    /**
     * Le même vecteur tourné d'un angle, dans le sens inverse des aiguilles d'une montre (y vers le
     * haut). Sa longueur ne change pas.
     *
     * @param radians l'angle de rotation, en radians
     * @return le vecteur tourné
     */
    public Vec2 rotated(float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vec2(x * cos - y * sin, x * sin + y * cos);
    }
}
