package game.core.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.world.Run;

/**
 * Affichage des stages : en haut à droite, le stage en cours et les ennemis tués sur ceux à
 * tuer ; au centre de l'écran, la grande annonce « Stage N » au début de chaque stage.
 *
 * <p>Comme le HUD, il a sa propre caméra fixe et les unités logiques 640x360. Il lit la partie
 * sans jamais la modifier : la durée de l'annonce vient de {@link Run#stageIntroLeft()}, la vue
 * ne garde aucun minuteur à elle.</p>
 */
public final class StageBanner implements Disposable {

    /** Distance entre le texte et les bords de l'écran logique, en unités. */
    private static final float MARGIN = 12f;

    /** Échelle du texte en haut à droite. */
    private static final float INFO_SCALE = 0.6f;

    /** Échelle de la grande annonce. */
    private static final float TITLE_SCALE = 2f;

    /** Hauteur de la grande annonce, en part de la hauteur de l'écran. */
    private static final float TITLE_HEIGHT = 0.68f;

    /** Durée du fondu à la fin de l'annonce, en secondes. */
    private static final float FADE = 0.5f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = Fonts.create();

    /**
     * Crée l'affichage des stages.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public StageBanner(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
        font.setUseIntegerPositions(false);
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
     * Dessine le stage en cours et, au début d'un stage, son annonce.
     *
     * @param run la partie à afficher
     */
    public void draw(Run run) {
        viewport.apply();
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.getData().setScale(INFO_SCALE);
        font.setColor(Color.WHITE);
        font.draw(batch, info(run), 0f, height - MARGIN, width - MARGIN, Align.right, false);

        float left = run.stageIntroLeft();
        if (left > 0f) {
            float alpha = Math.min(1f, left / FADE);
            font.getData().setScale(TITLE_SCALE);
            font.setColor(1f, 0.9f, 0.45f, alpha);
            font.draw(batch, title(run), 0f, height * TITLE_HEIGHT, width, Align.center, false);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    private static String info(Run run) {
        if (run.isBossStage()) {
            return "Stage " + run.stage() + " : Boss";
        }
        return "Stage " + run.stage() + "   " + run.stageKills() + " / " + run.currentStage().killsToClear();
    }

    private static String title(Run run) {
        return run.isBossStage() ? "Stage " + run.stage() + " : Boss" : "Stage " + run.stage();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
