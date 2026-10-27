package game.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.core.Main;
import game.core.hud.Fonts;
import game.core.input.Gamepad;
import game.core.input.Gamepad.Button;
import game.core.input.Gamepad.Direction;
import java.util.Objects;
import java.util.Random;

/** Base commune des écrans de menu : titre, pause, options, victoire, défaite. */
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

    /** Police des titres (Shojumaru), utilisée par {@link #centered} pour les grandes tailles. */
    protected final BitmapFont titleFont = Fonts.title();

    /** À partir de cette échelle, un texte centré est un titre : il prend la police des titres. */
    private static final float TITLE_FONT_FROM = 1.9f;

    /** Nombre de braises qui montent sur le fond des menus. */
    private static final int EMBERS = 70;

    private final Texture backdrop = new Texture(Gdx.files.internal("sprites/menu_bg.png"));
    private final Texture white;
    private final float[] emberX = new float[EMBERS];
    private final float[] emberY = new float[EMBERS];
    private final float[] emberSpeed = new float[EMBERS];
    private final Random random = new Random();
    private float time;

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
        backdrop.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        white = new Texture(pixel);
        pixel.dispose();
        for (int i = 0; i < EMBERS; i++) {
            emberX[i] = random.nextFloat() * WIDTH;
            emberY[i] = random.nextFloat() * HEIGHT;
            emberSpeed[i] = 8f + random.nextFloat() * 22f;
        }
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
     * @return {@code true} si l'écran a demandé un changement d'écran : il ne doit plus rien
     *     dessiner
     */
    protected abstract boolean handleInput();

    /** Dessine le fond. Par défaut, un fond uni sombre. */
    protected void drawBackground() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        // Le quartier qui brûle sous la lune, et des braises qui montent.
        batch.draw(backdrop, 0f, 0f, WIDTH, HEIGHT);
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        time += delta;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < EMBERS; i++) {
            emberY[i] += emberSpeed[i] * delta;
            emberX[i] += (float) Math.sin(time * 1.3 + i) * 6f * delta;
            if (emberY[i] > HEIGHT) {
                emberY[i] = -4f;
                emberX[i] = random.nextFloat() * WIDTH;
            }
            float twinkle = 0.55f + 0.45f * (float) Math.sin(time * 5 + i * 1.7);
            batch.setColor(1f, 0.45f + 0.35f * (i % 3) / 2f, 0.15f, twinkle * (1f - emberY[i] / HEIGHT * 0.6f));
            float size = i % 5 == 0 ? 2f : 1f;
            batch.draw(white, emberX[i], emberY[i], size, size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();
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
     * @param text le texte
     * @param y hauteur du haut du texte, en unités
     * @param scale échelle de la police
     * @param color couleur du texte
     */
    protected void centered(String text, float y, float scale, Color color) {
        BitmapFont f = scale >= TITLE_FONT_FROM ? titleFont : font;
        f.getData().setScale(scale);
        if (f == titleFont) {
            // Ombre portée sous les titres.
            f.setColor(0f, 0f, 0f, 0.75f * color.a);
            f.draw(batch, text, 1.5f, y - 1.5f, WIDTH, Align.center, false);
        }
        f.setColor(color);
        f.draw(batch, text, 0f, y, WIDTH, Align.center, false);
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

    /** @return {@code true} si Entrée, ou A (✕) sur la manette, vient d'être enfoncé */
    protected boolean confirmPressed() {
        return pressed(Keys.ENTER, Keys.NUMPAD_ENTER) || pad().justPressed(Button.A);
    }

    /** @return {@code true} si Échap, ou B (○) sur la manette, vient d'être enfoncé */
    protected boolean backPressed() {
        return pressed(Keys.ESCAPE) || pad().justPressed(Button.B);
    }

    /** @return {@code true} si haut vient d'être demandé : flèche, Z (W), croix ou stick */
    protected boolean upPressed() {
        return pressed(Keys.UP, Keys.W) || pad().justPressed(Direction.UP);
    }

    /** @return {@code true} si bas vient d'être demandé : flèche, S, croix ou stick */
    protected boolean downPressed() {
        return pressed(Keys.DOWN, Keys.S) || pad().justPressed(Direction.DOWN);
    }

    /** @return {@code true} si gauche vient d'être demandé : flèche, Q (A), croix ou stick */
    protected boolean leftPressed() {
        return pressed(Keys.LEFT, Keys.A) || pad().justPressed(Direction.LEFT);
    }

    /** @return {@code true} si droite vient d'être demandé : flèche, D, croix ou stick */
    protected boolean rightPressed() {
        return pressed(Keys.RIGHT, Keys.D) || pad().justPressed(Direction.RIGHT);
    }

    /**
     * Choisit le texte d'aide selon la source utilisée en dernier.
     *
     * @param keyboard le texte pour le clavier
     * @param gamepad le texte pour la manette
     * @return celui qui correspond à ce que le joueur a en main
     */
    protected String hint(String keyboard, String gamepad) {
        return main.usingGamepad() ? gamepad : keyboard;
    }

    /** @return la manette du jeu */
    protected Gamepad pad() {
        return main.gamepad();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        titleFont.dispose();
        backdrop.dispose();
        white.dispose();
    }
}
