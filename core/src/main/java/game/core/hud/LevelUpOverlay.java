package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.relic.Relic;
import java.util.List;

/**
 * Écran de montée de niveau, dessiné par-dessus le jeu figé : un voile sombre et une carte par
 * relique proposée, avec son numéro, son nom et sa description.
 *
 * <p>Le joueur choisit avec les touches 1, 2, 3 (rangée du haut ou pavé numérique) ou en
 * cliquant sur une carte. L'écran ne fait que lire le choix : c'est la partie
 * ({@code Run.chooseRelic}) qui donne la relique.</p>
 */
public final class LevelUpOverlay implements Disposable {

    private static final float CARD_WIDTH = 160f;
    private static final float CARD_HEIGHT = 110f;
    private static final float CARD_GAP = 20f;
    private static final float CARD_PADDING = 10f;
    private static final float TITLE_SCALE = 1f;
    private static final float NAME_SCALE = 0.8f;
    private static final float TEXT_SCALE = 0.6f;

    private static final int[][] KEYS = {
        {Input.Keys.NUM_1, Input.Keys.NUMPAD_1},
        {Input.Keys.NUM_2, Input.Keys.NUMPAD_2},
        {Input.Keys.NUM_3, Input.Keys.NUMPAD_3},
    };

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final Vector3 touch = new Vector3();

    /**
     * Crée l'écran de montée de niveau.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public LevelUpOverlay(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
        font.setUseIntegerPositions(false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    /**
     * Adapte l'écran à la nouvelle taille de la fenêtre.
     *
     * @param width  largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Lit le choix du joueur à cette image.
     *
     * @param count nombre de reliques proposées
     * @return la position de la relique choisie, ou -1 si le joueur n'a rien choisi
     */
    public int readChoice(int count) {
        for (int i = 0; i < Math.min(count, KEYS.length); i++) {
            for (int key : KEYS[i]) {
                if (Gdx.input.isKeyJustPressed(key)) {
                    return i;
                }
            }
        }
        if (Gdx.input.justTouched()) {
            touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            viewport.unproject(touch);
            for (int i = 0; i < count; i++) {
                float x = cardX(i, count);
                float y = cardY();
                if (touch.x >= x && touch.x <= x + CARD_WIDTH && touch.y >= y && touch.y <= y + CARD_HEIGHT) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Dessine le voile et les cartes. À appeler en dernier, par-dessus le jeu et le HUD.
     *
     * @param offer les reliques proposées
     */
    public void draw(List<Relic> offer) {
        viewport.apply();
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(0f, 0f, width, height);
        for (int i = 0; i < offer.size(); i++) {
            float x = cardX(i, offer.size());
            float y = cardY();
            shapes.setColor(0.95f, 0.78f, 0.25f, 1f);
            shapes.rect(x - 1f, y - 1f, CARD_WIDTH + 2f, CARD_HEIGHT + 2f);
            shapes.setColor(0.12f, 0.12f, 0.15f, 1f);
            shapes.rect(x, y, CARD_WIDTH, CARD_HEIGHT);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.getData().setScale(TITLE_SCALE);
        font.setColor(0.95f, 0.78f, 0.25f, 1f);
        font.draw(batch, "Niveau supérieur ! Choisis une relique", 0f, cardY() + CARD_HEIGHT + 34f,
                width, Align.center, false);
        for (int i = 0; i < offer.size(); i++) {
            Relic relic = offer.get(i);
            float x = cardX(i, offer.size()) + CARD_PADDING;
            float inner = CARD_WIDTH - 2f * CARD_PADDING;
            float top = cardY() + CARD_HEIGHT - CARD_PADDING;
            font.getData().setScale(NAME_SCALE);
            font.setColor(0.95f, 0.78f, 0.25f, 1f);
            font.draw(batch, String.valueOf(i + 1), x, top, inner, Align.left, false);
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, relic.name(), x, top - 22f, inner, Align.center, true);
            font.getData().setScale(TEXT_SCALE);
            font.setColor(0.80f, 0.80f, 0.85f, 1f);
            font.draw(batch, relic.description(), x, top - 50f, inner, Align.center, true);
        }
        batch.end();
    }

    private float cardX(int index, int count) {
        float total = count * CARD_WIDTH + (count - 1) * CARD_GAP;
        float start = (viewport.getWorldWidth() - total) / 2f;
        return start + index * (CARD_WIDTH + CARD_GAP);
    }

    private float cardY() {
        return (viewport.getWorldHeight() - CARD_HEIGHT) / 2f - 10f;
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
