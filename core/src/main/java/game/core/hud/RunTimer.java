package game.core.hud;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.world.Run;

/** Le chrono de la run : le temps de jeu écoulé, en mm:ss, en haut au centre de l'écran. */
public final class RunTimer implements Disposable {

    /** Distance entre le haut de l'écran et le haut du chrono, en unités. */
    private static final float TOP_MARGIN = 12f;

    /** Décalage vers le bas quand la barre du boss est affichée, pour passer sous elle. */
    private static final float BOSS_BAR_OFFSET = 38f;

    /** Taille du texte (échelle de {@link Fonts}). */
    private static final float SCALE = 0.75f;

    /** Décalage de l'ombre portée sous le texte, en unités. */
    private static final float SHADOW = 1f;

    private static final int SECONDS_PER_MINUTE = 60;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = Fonts.create();

    /**
     * Crée le chrono.
     *
     * @param width largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public RunTimer(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
    }

    /**
     * Adapte le chrono à la nouvelle taille de la fenêtre.
     *
     * @param width largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine le temps de jeu écoulé de la partie.
     *
     * @param run la partie à afficher
     */
    public void draw(Run run) {
        viewport.apply();
        float width = viewport.getWorldWidth();
        float top = viewport.getWorldHeight() - TOP_MARGIN;
        if (run.boss().isPresent()) {
            top -= BOSS_BAR_OFFSET;
        }
        String text = format(run.elapsed());

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.getData().setScale(SCALE);
        // Ombre portée, pour rester lisible sur le décor clair des incendies.
        font.setColor(0f, 0f, 0f, 0.75f);
        font.draw(batch, text, SHADOW, top - SHADOW, width, Align.center, false);
        font.setColor(0.96f, 0.82f, 0.45f, 1f);
        font.draw(batch, text, 0f, top, width, Align.center, false);
        font.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    /**
     * Met une durée au format mm:ss. Les secondes entamées ne comptent pas (59,9 s donne « 00:59
     * »), et une durée négative donne « 00:00 ».
     *
     * @param seconds la durée, en secondes
     * @return la durée au format mm:ss, par exemple « 01:01 » pour 61 secondes
     */
    public static String format(float seconds) {
        int total = (int) Math.max(0f, seconds);
        return String.format("%02d:%02d", total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
