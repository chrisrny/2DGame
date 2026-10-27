package game.core.render;

/**
 * Une source de lumière pour {@link LightingRenderer} : une tache ronde et douce, colorée.
 *
 * @param x centre, en unités du monde
 * @param y centre, en unités du monde
 * @param radius rayon, en unités
 * @param r rouge, de 0 à 1
 * @param g vert, de 0 à 1
 * @param b bleu, de 0 à 1
 * @param strength intensité, de 0 à 1
 * @param flicker amplitude du vacillement (0 = lumière fixe, 0,2 = flamme)
 * @param phase décalage du vacillement, pour que deux flammes ne battent pas ensemble
 */
public record Light(float x, float y, float radius, float r, float g, float b, float strength, float flicker,
        float phase) {

    /**
     * Lumière fixe, sans vacillement.
     *
     * @param x centre, en unités du monde
     * @param y centre, en unités du monde
     * @param radius rayon, en unités
     * @param r rouge, de 0 à 1
     * @param g vert, de 0 à 1
     * @param b bleu, de 0 à 1
     * @param strength intensité, de 0 à 1
     * @return la lumière
     */
    public static Light steady(float x, float y, float radius, float r, float g, float b, float strength) {
        return new Light(x, y, radius, r, g, b, strength, 0f, 0f);
    }

    /**
     * Lumière de flamme, qui vacille.
     *
     * @param x centre, en unités du monde
     * @param y centre, en unités du monde
     * @param radius rayon, en unités
     * @param strength intensité, de 0 à 1
     * @return la lumière, orangée
     */
    public static Light flame(float x, float y, float radius, float strength) {
        return new Light(x, y, radius, 1f, 0.5f, 0.2f, strength, 0.18f, x * 0.37f + y * 0.11f);
    }
}
