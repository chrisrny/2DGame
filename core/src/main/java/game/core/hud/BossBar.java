package game.core.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.entity.Boss;
import game.model.world.Run;

/**
 * Barre de vie du boss, en haut au centre de l'écran : son nom, puis ses points de vie.
 *
 * <p>Elle n'apparaît qu'au stage 3, une fois le boss arrivé. Comme le HUD, elle a sa propre
 * caméra fixe et les unités logiques 640x360, et lit la partie sans jamais la modifier.</p>
 */
public final class BossBar implements Disposable {

    /** Nom du boss affiché au-dessus de sa barre. */
    public static final String NAME = "Le Gardien";

    /** Distance entre le nom et le haut de l'écran logique, en unités : la même que le HUD. */
    private static final float MARGIN = 12f;

    /** Largeur de la barre, en unités : bien plus large que celle du joueur, c'est un boss. */
    private static final float BAR_WIDTH = 240f;

    /** Hauteur de la barre, en unités. */
    private static final float BAR_HEIGHT = 6f;

    /** Épaisseur du cadre sombre autour de la barre, en unités. */
    private static final float FRAME = 1f;

    /** Espace entre le nom et la barre, en unités. */
    private static final float NAME_GAP = 4f;

    /** Échelle du nom. */
    private static final float NAME_SCALE = 0.6f;

    private static final Color FRAME_COLOR = new Color(0.08f, 0.07f, 0.1f, 1f);
    private static final Color EMPTY_COLOR = new Color(0.3f, 0.06f, 0.1f, 1f);
    private static final Color HP_COLOR = new Color(0.85f, 0.15f, 0.2f, 1f);

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = Fonts.create();

    /**
     * Crée la barre de vie du boss.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public BossBar(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
        font.setUseIntegerPositions(false);
        font.getData().setScale(NAME_SCALE);
    }

    /**
     * Adapte l'affichage à la nouvelle taille de la fenêtre.
     *
     * @param width  largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine le nom et la barre de vie du boss, s'il est là.
     *
     * @param run la partie à afficher
     */
    public void draw(Run run) {
        Boss boss = run.boss().orElse(null);
        if (boss == null) {
            return;
        }
        viewport.apply();
        float width = viewport.getWorldWidth();
        float nameTop = viewport.getWorldHeight() - MARGIN;
        float barX = (width - BAR_WIDTH) / 2f;
        float barY = nameTop - font.getCapHeight() - NAME_GAP - BAR_HEIGHT;
        float ratio = (float) boss.hp() / boss.maxHp();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(FRAME_COLOR);
        shapes.rect(barX - FRAME, barY - FRAME, BAR_WIDTH + 2f * FRAME, BAR_HEIGHT + 2f * FRAME);
        shapes.setColor(EMPTY_COLOR);
        shapes.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);
        shapes.setColor(HP_COLOR);
        shapes.rect(barX, barY, BAR_WIDTH * ratio, BAR_HEIGHT);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, NAME, 0f, nameTop, width, Align.center, false);
        batch.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
