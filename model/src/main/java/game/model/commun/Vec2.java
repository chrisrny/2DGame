package game.model.commun;

/**
 * Vecteur 2D immuable : une position, une vitesse ou une direction dans le monde du jeu.
 *
 * <p>Repère du modèle : 1 unité = 1 pixel de l'écran logique 640x360, origine en bas à gauche,
 * y vers le haut. Chaque opération renvoie un nouveau {@code Vec2} : un vecteur partagé entre
 * plusieurs objets ne peut donc jamais être modifié par erreur.</p>
 *
 * <p>Le modèle n'utilise pas le {@code Vector2} de libGDX, pour rester en Java pur.</p>
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
     * <p>Par exemple {@code fromAxes(1, 1)} (haut + droite) donne une direction de longueur 1 :
     * on ne va pas plus vite en diagonale.</p>
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

    /**
     * Longueur (norme) du vecteur.
     *
     * @return la longueur, toujours positive ou nulle
     */
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
     * Produit scalaire. Sert notamment à mesurer l'angle entre deux directions :
     * pour deux directions normalisées, il vaut 1 si elles sont identiques, 0 si elles
     * sont perpendiculaires et -1 si elles sont opposées.
     *
     * @param other l'autre vecteur
     * @return le produit scalaire
     */
    public float dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    /**
     * Le même vecteur ramené à une longueur de 1.
     *
     * <p>Le vecteur nul n'a pas de direction : le diviser par sa longueur donnerait
     * {@code NaN}, qui se propagerait partout. On renvoie donc {@link #ZERO} dans ce cas.</p>
     *
     * @return la direction de longueur 1, ou {@link #ZERO} pour le vecteur nul
     */
    public Vec2 normalized() {
        float length = length();
        if (length == 0f) {
            return ZERO;
        }
        return new Vec2(x / length, y / length);
    }
}
