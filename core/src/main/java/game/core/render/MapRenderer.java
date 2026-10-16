package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Rect;
import java.util.List;

/**
 * Dessine le décor en fausse 3D (vue 3/4) avec les tuiles de {@code assets/sprites/tiles.png} :
 * le sol en dalles et les murs en blocs.
 *
 * <p>Un mur garde au sol son emprise de collision (le rectangle du modèle). À l'écran, sa face
 * du dessus est remontée de {@link #WALL_FRONT} unités et sa face avant, en briques, occupe le
 * bas de l'emprise : le mur paraît avoir une hauteur. Le tri par profondeur ({@link SceneRenderer})
 * fait le reste : on passe derrière un mur quand on est au nord de lui.</p>
 */
public final class MapRenderer implements Disposable {

    /** Taille d'une tuile, en pixels (et en unités du monde). */
    public static final int TILE = 16;

    /** Hauteur visible de la face avant d'un mur, en unités. */
    public static final float WALL_FRONT = 12f;

    /** Épaisseur de l'ombre que projette un mur au sol, en unités. */
    private static final float WALL_SHADOW = 5f;

    // Dans tiles.png : ligne 0 = 4 variantes de sol, ligne 1 = 2 dessus de mur puis 2 faces avant.
    private static final int FLOOR_VARIANTS = 4;
    private static final int TOP_SRC_X = 0;
    private static final int FRONT_SRC_X = 32;
    private static final int ROW_1 = 16;

    private final Texture tiles;
    private final Texture fx;

    /**
     * Charge les tuiles.
     *
     * @param fx la planche d'effets, pour son pixel blanc (liserés des murs)
     */
    public MapRenderer(Texture fx) {
        this.fx = fx;
        tiles = new Texture(Gdx.files.internal("sprites/tiles.png"));
        tiles.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /**
     * Dessine les dalles visibles par la caméra, puis l'ombre des murs au sol.
     * Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch  le pinceau d'images
     * @param arena  les limites de la map
     * @param walls  les murs
     * @param camera la caméra du monde, pour ne dessiner que ce qui est à l'écran
     * @param viewW  largeur visible, en unités
     * @param viewH  hauteur visible, en unités
     */
    public void drawGround(SpriteBatch batch, Rect arena, List<Rect> walls, OrthographicCamera camera,
            float viewW, float viewH) {
        int tx0 = (int) Math.floor(Math.max(arena.x(), camera.position.x - viewW / 2f) / TILE);
        int ty0 = (int) Math.floor(Math.max(arena.y(), camera.position.y - viewH / 2f) / TILE);
        int tx1 = (int) Math.ceil(Math.min(arena.right(), camera.position.x + viewW / 2f) / TILE);
        int ty1 = (int) Math.ceil(Math.min(arena.top(), camera.position.y + viewH / 2f) / TILE);
        for (int ty = ty0; ty < ty1; ty++) {
            for (int tx = tx0; tx < tx1; tx++) {
                int variant = variant(tx, ty) * TILE;
                batch.draw(tiles, tx * TILE, ty * TILE, TILE, TILE, variant, 0, TILE, TILE, false, false);
            }
        }
        // Ombre portée : une bande sombre sous la face avant, décalée vers la gauche.
        batch.setColor(0f, 0f, 0f, 0.45f);
        for (Rect wall : walls) {
            batch.draw(fx, wall.x() - 3f, wall.y() - WALL_SHADOW, wall.width(), WALL_SHADOW, 62, 0, 1, 1, false, false);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Dessine un mur en bloc : face avant en briques, puis dessus remonté avec un liseré clair
     * du côté de la lumière (en haut et à droite). Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param wall  l'emprise du mur dans le modèle
     */
    public void drawWall(SpriteBatch batch, Rect wall) {
        int v = variant((int) (wall.x() / TILE), (int) (wall.y() / TILE)) % 2;
        // Face avant : les 12 dernières lignes de la tuile de briques.
        fill(batch, wall.x(), wall.y(), wall.width(), WALL_FRONT,
                FRONT_SRC_X + v * TILE, ROW_1 + TILE - (int) WALL_FRONT, (int) WALL_FRONT);
        // Dessus : toute l'emprise, remontée de la hauteur de la face avant.
        fill(batch, wall.x(), wall.y() + WALL_FRONT, wall.width(), wall.height(), TOP_SRC_X + v * TILE, ROW_1, TILE);
        float top = wall.y() + WALL_FRONT + wall.height();
        batch.setColor(0.70f, 0.66f, 0.85f, 1f);
        batch.draw(fx, wall.x(), top - 1f, wall.width(), 1f, 62, 0, 1, 1, false, false);
        batch.draw(fx, wall.right() - 1f, wall.y() + WALL_FRONT, 1f, wall.height(), 62, 0, 1, 1, false, false);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // Répète une tuile sur un rectangle ; les bouts qui dépassent sont coupés.
    private void fill(SpriteBatch batch, float x, float y, float w, float h, int srcX, int srcY, int srcH) {
        for (float py = y; py < y + h; py += srcH) {
            float ch = Math.min(srcH, y + h - py);
            for (float px = x; px < x + w; px += TILE) {
                float cw = Math.min(TILE, x + w - px);
                batch.draw(tiles, px, py, cw, ch, srcX, srcY + srcH - (int) ch, (int) cw, (int) ch, false, false);
            }
        }
    }

    // Variante de dalle stable pour une case donnée : surtout les deux dalles simples, parfois les autres.
    private static int variant(int tx, int ty) {
        int h = tx * 73856093 ^ ty * 19349663;
        h ^= h >>> 13;
        int roll = Math.floorMod(h, 16);
        return roll < 7 ? 0 : roll < 14 ? 1 : roll == 14 ? 2 : FLOOR_VARIANTS - 1;
    }

    @Override
    public void dispose() {
        tiles.dispose();
    }
}
