package game.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.core.Main;
import game.core.hud.Fonts;
import java.util.Objects;

/**
 * Base commune des écrans de menu : titre, pause, options, victoire, défaite.
 *
 * <p>Chaque image : l'écran lit les touches, puis dessine son fond et son contenu. Si la lecture
 * des touches a fait changer d'écran, il ne dessine rien : il vient peut-être d'être libéré.
 * Le texte est écrit avec la police du jeu ({@link Fonts}) dans les mêmes unités logiques que le jeu
 * (640x360) ; l'écran possède sa police, son pinceau et son batch, et les libère dans
 * {@link #dispose()}.</p>
 */
public abstract class MenuScreen extends ScreenAdapter {

    /** Largeur de l'écran logique, en unités. */
    protected static final float WIDTH = GameScreen.WORLD_WIDTH;

    /** Hauteur de l'écran logique, en unités. */
    protected static final float HEIGHT = GameScreen.WORLD_HEIGHT;

    /** Échelle des grands titres. */
    protected static final float TITLE_SCALE = 3f;

    /** Échelle du texte courant. */
    protected static final float TEXT_SCALE = 0.9f;

    /** Couleur du texte courant. */
    protected static final Color TEXT = new Color(0.85f, 0.85f, 0.9f, 1f);

    /** Couleur du texte discret (aides, touches). */
    protected static final Color HINT = new Color(0.55f, 0.55f, 0.62f, 1f);

    /** Couleur de l'entrée de menu sélectionnée. */
    protected static final Color SELECTED = new Color(1f, 0.85f, 0.35f, 1f);

    /** Le jeu, pour changer d'écran. */
    protected final Main main;

    /** Le batch pour écrire le texte. */
    protected final SpriteBatch batch = new SpriteBatch();

    /** Le pinceau de formes, pour les voiles et les cadres. */
    protected final ShapeRenderer shapes = new ShapeRenderer();

    /** La police de l'écran. */
    protected final BitmapFont font = Fonts.create();

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);

    /**
     * Crée l'écran.
     *
     * @param main le jeu, pour changer d'écran
     */
    protected MenuScreen(Main main) {
        this.main = Objects.requireNonNull(main, "main");
        font.setUseIntegerPositions(false);
    }

    @Override
    public final void render(float delta) {
        if (handleInput()) {
            return;
        }
        drawBackground();
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        drawContent();
    }

    /**
     * Lit les touches de l'image et réagit.
     *
     * @return {@code true} si l'écran a demandé un changement d'écran : il ne doit plus rien dessiner
     */
    protected abstract boolean handleInput();

    /** Dessine le fond. Par défaut, un fond uni sombre. */
    protected void drawBackground() {
        ScreenUtils.clear(0.05f, 0.04f, 0.07f, 1f);
    }

    /** Dessine le contenu de l'écran, dans les unités logiques 640x360. */
    protected abstract void drawContent();

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Écrit une ligne de texte centrée horizontalement.
     *
     * @param text  le texte
     * @param y     hauteur du haut du texte, en unités
     * @param scale échelle de la police
     * @param color couleur du texte
     */
    protected void centered(String text, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, text, 0f, y, WIDTH, Align.center, false);
    }

    /**
     * Couvre tout l'écran d'un voile noir transparent, par exemple par-dessus la partie figée.
     *
     * @param alpha opacité du voile, de 0 (invisible) à 1 (noir)
     */
    protected void veil(float alpha) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, alpha);
        shapes.rect(0f, 0f, WIDTH, HEIGHT);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Indique si une des touches vient d'être enfoncée à cette image.
     *
     * @param keys les touches, en codes {@link Keys}
     * @return {@code true} si l'une d'elles vient d'être enfoncée
     */
    protected static boolean pressed(int... keys) {
        for (int key : keys) {
            if (Gdx.input.isKeyJustPressed(key)) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} si Entrée vient d'être enfoncée */
    protected static boolean confirmPressed() {
        return pressed(Keys.ENTER, Keys.NUMPAD_ENTER);
    }

    /** @return {@code true} si Échap vient d'être enfoncée */
    protected static boolean backPressed() {
        return pressed(Keys.ESCAPE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}
