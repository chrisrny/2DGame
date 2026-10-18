package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.relic.Relic;
import java.util.List;

/**
 * Écran de montée de niveau, par-dessus le jeu figé : un voile sombre, un titre sur un coup de
 * pinceau, et une carte laquée par relique proposée (icône dans un médaillon, nom, effet).
 *
 * <p>Les cartes entrent l'une après l'autre en montant. Celle sous la souris s'élève et
 * s'illumine. Le joueur choisit avec les touches 1, 2, 3 (rangée du haut ou pavé numérique) ou en
 * cliquant sur une carte. L'écran ne fait que lire le choix : c'est la partie
 * ({@code Run.chooseRelic}) qui donne la relique.</p>
 */
public final class LevelUpOverlay implements Disposable {

    private static final float CARD_WIDTH = 152f;
    private static final float CARD_HEIGHT = 200f;
    private static final float CARD_GAP = 22f;
    private static final float ICON_SCALE = 2f;
    /** Durée de l'entrée d'une carte, et décalage entre deux cartes, en secondes. */
    private static final float CARD_IN = 0.28f;
    private static final float CARD_STAGGER = 0.09f;
    /** Hauteur dont s'élève la carte survolée, en unités. */
    private static final float HOVER_LIFT = 5f;

    private static final int[][] KEYS = {
        {Input.Keys.NUM_1, Input.Keys.NUMPAD_1},
        {Input.Keys.NUM_2, Input.Keys.NUMPAD_2},
        {Input.Keys.NUM_3, Input.Keys.NUMPAD_3},
    };

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = Fonts.create();
    private final BitmapFont title = Fonts.title();
    private final UiAtlas ui = new UiAtlas();
    private final Vector3 touch = new Vector3();
    private List<Relic> shown;
    private float time;
    private final float[] hover = new float[3];

    /**
     * Crée l'écran de montée de niveau.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public LevelUpOverlay(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
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
            int index = cardUnderPointer(count);
            if (index >= 0) {
                return index;
            }
        }
        return -1;
    }

    private int cardUnderPointer(int count) {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touch);
        for (int i = 0; i < count; i++) {
            float x = cardX(i, count);
            float y = cardY();
            if (touch.x >= x && touch.x <= x + CARD_WIDTH && touch.y >= y && touch.y <= y + CARD_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Dessine le voile, le titre et les cartes. À appeler en dernier, par-dessus le jeu et le HUD.
     *
     * @param offer les reliques proposées
     */
    public void draw(List<Relic> offer) {
        if (offer != shown) {
            // Nouvelle offre : les cartes refont leur entrée.
            shown = offer;
            time = 0f;
        }
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        time += delta;
        viewport.apply();
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        int under = cardUnderPointer(offer.size());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float veil = Math.min(1f, time / 0.2f);
        batch.setColor(0.02f, 0.01f, 0.03f, 0.72f * veil);
        batch.draw(ui.white, 0f, 0f, width, height);
        batch.setColor(0f, 0f, 0f, 0.8f * veil);
        batch.draw(ui.vignette, 0f, 0f, width, height);
        batch.setColor(1f, 1f, 1f, veil);
        float rw = ui.ribbon.getRegionWidth() * 1.25f;
        float rh = ui.ribbon.getRegionHeight() * 1.1f;
        float ry = cardY() + CARD_HEIGHT + 2f;
        batch.draw(ui.ribbon, (width - rw) / 2f, ry, rw, rh);
        title.getData().setScale(1.05f);
        title.setColor(0.98f, 0.84f, 0.42f, veil);
        title.draw(batch, "Choisis une relique", 0f, ry + rh * 0.68f, width, Align.center, false);
        title.setColor(1f, 1f, 1f, 1f);

        for (int i = 0; i < offer.size(); i++) {
            float target = i == under ? 1f : 0f;
            hover[i] += (target - hover[i]) * Math.min(1f, delta * 14f);
            drawCard(offer.get(i), i, offer.size());
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawCard(Relic relic, int index, int count) {
        float t = Math.clamp((time - index * CARD_STAGGER) / CARD_IN, 0f, 1f);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        float lift = hover[index] * HOVER_LIFT;
        float x = cardX(index, count);
        float y = cardY() - (1f - ease) * 40f + lift;
        float alpha = ease;

        // Lueur derrière la carte survolée.
        if (hover[index] > 0.01f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.7f, 0.3f, 0.35f * hover[index] * alpha);
            batch.draw(ui.glow, x - 30f, y - 30f, CARD_WIDTH + 60f, CARD_HEIGHT + 60f);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(ui.card, x, y);

        // Médaillon et icône, agrandie deux fois (pixels nets).
        float discX = x + (CARD_WIDTH - ui.cardDisc.getRegionWidth()) / 2f;
        float discY = y + CARD_HEIGHT - 26f - ui.cardDisc.getRegionHeight() - 14f;
        batch.draw(ui.cardDisc, discX, discY);
        TextureRegion icon = ui.icon(relic);
        float iw = icon.getRegionWidth() * ICON_SCALE;
        float bob = (float) Math.sin(time * 2.4 + index) * 1.5f * hover[index];
        batch.draw(icon, discX + 26f - iw / 2f, discY + 26f - iw / 2f + bob, iw, iw);

        // Sceau du numéro, en haut à gauche.
        batch.draw(ui.seal, x + 10f, y + CARD_HEIGHT - 28f);
        font.getData().setScale(0.6f);
        font.setColor(1f, 0.95f, 0.88f, alpha);
        font.draw(batch, String.valueOf(index + 1), x + 10f, y + CARD_HEIGHT - 19f + font.getCapHeight() / 2f, 18f,
                Align.center, false);

        // Nom sur la plaque, puis effet.
        title.getData().setScale(0.62f);
        title.setColor(0.98f, 0.84f, 0.42f, alpha);
        title.draw(batch, relic.name(), x + 14f, y + CARD_HEIGHT - 100f - 11f + title.getCapHeight() / 2f + 1f,
                CARD_WIDTH - 28f, Align.center, false);
        font.getData().setScale(0.52f);
        font.setColor(0.9f, 0.88f, 0.84f, alpha);
        font.draw(batch, relic.description(), x + 16f, y + 62f, CARD_WIDTH - 32f, Align.center, true);
        font.getData().setScale(0.38f);
        font.setColor(0.75f, 0.6f, 0.35f, alpha * (0.6f + 0.4f * hover[index]));
        font.draw(batch, "TOUCHE " + (index + 1) + " OU CLIC", x, y + 20f, CARD_WIDTH, Align.center, false);
        font.setColor(1f, 1f, 1f, 1f);
        title.setColor(1f, 1f, 1f, 1f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private float cardX(int index, int count) {
        float total = count * CARD_WIDTH + (count - 1) * CARD_GAP;
        float start = (viewport.getWorldWidth() - total) / 2f;
        return start + index * (CARD_WIDTH + CARD_GAP);
    }

    private float cardY() {
        return (viewport.getWorldHeight() - CARD_HEIGHT) / 2f - 22f;
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        title.dispose();
        ui.dispose();
    }
}
