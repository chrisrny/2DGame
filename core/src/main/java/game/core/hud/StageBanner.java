package game.core.hud;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.world.Run;

/**
 * L'étage en cours : une plaque laquée en haut à droite (numéro de l'étage, ennemis vaincus et
 * jauge), et au début de chaque étage son annonce sur un coup de pinceau à l'encre, avec son nom.
 */
public final class StageBanner implements Disposable {

    private static final float MARGIN = 8f;
    private static final float PLAQUE_WIDTH = 132f;
    private static final float PLAQUE_HEIGHT = 20f;

    /** Durée de l'entrée et de la sortie de l'annonce, en secondes. */
    private static final float FADE = 0.4f;

    /** Le nom de chaque étage, affiché sous son numéro. */
    private static final String[] NAMES = {"Les ruelles en flammes", "Le canal des lanternes", "Le torii de l'Oni"};

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = Fonts.create();
    private final BitmapFont title = Fonts.title();
    private final UiAtlas ui = new UiAtlas();

    /**
     * Crée le bandeau d'étage.
     *
     * @param width largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public StageBanner(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
    }

    /**
     * Adapte le bandeau à la nouvelle taille de la fenêtre.
     *
     * @param width largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine la plaque de l'étage en cours et, au début d'un étage, son annonce.
     *
     * @param run la partie à afficher
     */
    public void draw(Run run) {
        viewport.apply();
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawPlaque(run, width - MARGIN - PLAQUE_WIDTH, height - MARGIN - PLAQUE_HEIGHT);
        float left = run.stageIntroLeft();
        if (left > 0f) {
            drawIntro(run, left, width, height);
        }
        batch.end();
    }

    private void drawPlaque(Run run, float x, float y) {
        UiAtlas.three(batch, ui.plaqueLeft, ui.plaqueMid, ui.plaqueRight, x, y, PLAQUE_WIDTH);
        title.getData().setScale(0.62f);
        title.setColor(0.96f, 0.82f, 0.45f, 1f);
        title.draw(batch, "Étage " + run.stage(), x + 10f, y + PLAQUE_HEIGHT / 2f + title.getCapHeight() / 2f);
        font.getData().setScale(0.45f);
        float right = x + PLAQUE_WIDTH - 10f;
        float textY = y + PLAQUE_HEIGHT / 2f + font.getCapHeight() / 2f + 2f;
        if (run.isBossStage()) {
            font.setColor(1f, 0.45f, 0.35f, 1f);
            font.draw(batch, "BOSS", x, textY, right - x, Align.right, false);
        } else {
            int kills = run.stageKills();
            int goal = run.currentStage().killsToClear();
            font.setColor(0.95f, 0.92f, 0.86f, 1f);
            font.draw(batch, kills + " / " + goal, x, textY, right - x, Align.right, false);
            // jauge des ennemis vaincus, sous le chiffre
            float gw = 46f;
            float gx = right - gw;
            float gy = y + 4f;
            batch.setColor(0.02f, 0.01f, 0.02f, 1f);
            batch.draw(ui.white, gx - 1f, gy - 1f, gw + 2f, 4f);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(ui.fillXp, gx, gy, gw * Math.min(1f, (float) kills / goal), 2f);
        }
        font.setColor(1f, 1f, 1f, 1f);
        title.setColor(1f, 1f, 1f, 1f);
    }

    // Le ruban glisse depuis la gauche, le titre apparaît, puis tout s'efface.
    private void drawIntro(Run run, float left, float width, float height) {
        float elapsed = Run.STAGE_INTRO - left;
        float in = Math.min(1f, elapsed / FADE);
        float out = Math.min(1f, left / FADE);
        float alpha = Math.min(in, out);
        float ease = 1f - (1f - in) * (1f - in) * (1f - in);
        float rw = ui.ribbon.getRegionWidth() * 1.3f;
        float rh = ui.ribbon.getRegionHeight() * 1.3f;
        float rx = (width - rw) / 2f - (1f - ease) * 60f;
        float ry = height * 0.6f - rh / 2f;
        batch.setColor(1f, 1f, 1f, alpha * 0.92f);
        batch.draw(ui.ribbon, rx, ry, rw, rh);
        batch.setColor(1f, 1f, 1f, 1f);

        String head = run.isBossStage() ? "Étage " + run.stage() + " — Boss" : "Étage " + run.stage();
        title.getData().setScale(1.5f);
        title.setColor(0f, 0f, 0f, alpha * 0.8f);
        title.draw(batch, head, 1.2f, ry + rh * 0.66f - 1.2f, width, Align.center, false);
        title.setColor(0.98f, 0.84f, 0.42f, alpha);
        title.draw(batch, head, 0f, ry + rh * 0.66f, width, Align.center, false);

        String name = NAMES[Math.min(NAMES.length, Math.max(1, run.stage())) - 1];
        font.getData().setScale(0.55f);
        font.setColor(0.95f, 0.9f, 0.85f, alpha);
        font.draw(batch, name, 0f, ry + rh * 0.3f, width, Align.center, false);
        font.setColor(1f, 1f, 1f, 1f);
        title.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        title.dispose();
        ui.dispose();
    }
}
