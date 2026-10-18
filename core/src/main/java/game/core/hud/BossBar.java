package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.entity.Boss;
import game.model.world.Run;

/**
 * Barre de vie du boss, en haut au centre : son nom, puis une barre laquée à cornes d'oni.
 *
 * <p>Comme la barre du héros, la vie perdue reste un instant en clair puis fond. Un repère
 * marque le seuil de la phase 2 ({@link Boss#SECOND_PHASE_THRESHOLD}) ; une fois passé, la barre
 * brûle et bat.</p>
 */
public final class BossBar implements Disposable {

    /** Nom du boss affiché au-dessus de sa barre. */
    public static final String NAME = "L'Oni du quartier";

    private static final float TOP_MARGIN = 10f;
    private static final float BAR_WIDTH = 270f;
    private static final float FILL_INSET = 20f;
    /** Lignes de la barre où se dessine le remplissage (laque du milieu). */
    private static final float FILL_Y = 6f;
    private static final float FILL_H = 6f;
    private static final float TRAIL_DELAY = 0.4f;
    private static final float TRAIL_SPEED = 0.5f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont title = Fonts.title();
    private final UiAtlas ui = new UiAtlas();
    private float trail = 1f;
    private float trailWait;
    private int lastHp = -1;
    private float time;

    /**
     * Crée la barre du boss.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public BossBar(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
    }

    /**
     * Adapte la barre à la nouvelle taille de la fenêtre.
     *
     * @param width  largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine la barre du boss, s'il est là.
     *
     * @param run la partie
     */
    public void draw(Run run) {
        Boss boss = run.boss().orElse(null);
        if (boss == null) {
            lastHp = -1;
            trail = 1f;
            return;
        }
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        time += delta;
        float ratio = Math.clamp((float) boss.hp() / boss.maxHp(), 0f, 1f);
        if (lastHp >= 0 && boss.hp() < lastHp) {
            trailWait = TRAIL_DELAY;
        }
        lastHp = boss.hp();
        if (ratio >= trail) {
            trail = ratio;
        } else if (trailWait > 0f) {
            trailWait -= delta;
        } else {
            trail = Math.max(ratio, trail - TRAIL_SPEED * delta);
        }

        viewport.apply();
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        float x = (width - BAR_WIDTH) / 2f;
        float y = height - TOP_MARGIN - 34f;
        float fx = x + FILL_INSET;
        float fw = BAR_WIDTH - 2f * FILL_INSET;
        boolean secondPhase = boss.phaseNumber() >= 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        title.getData().setScale(0.85f);
        title.setColor(0f, 0f, 0f, 0.8f);
        title.draw(batch, NAME, 1f, height - TOP_MARGIN - 1f, width, Align.center, false);
        title.setColor(secondPhase ? 1f : 0.96f, secondPhase ? 0.55f : 0.86f, secondPhase ? 0.35f : 0.6f, 1f);
        title.draw(batch, NAME, 0f, height - TOP_MARGIN, width, Align.center, false);
        title.setColor(1f, 1f, 1f, 1f);

        UiAtlas.three(batch, ui.bossLeft, ui.bossMid, ui.bossRight, x, y, BAR_WIDTH);
        batch.draw(ui.fillEmpty, fx, y + FILL_Y, fw, FILL_H);
        batch.draw(ui.fillTrail, fx, y + FILL_Y, fw * trail, FILL_H);
        if (secondPhase) {
            // Phase 2 : la barre brûle et bat.
            float k = 0.85f + 0.15f * (float) Math.sin(time * 9.0);
            batch.setColor(1f, k, k * 0.8f, 1f);
        }
        batch.draw(ui.fillBoss, fx, y + FILL_Y, fw * ratio, FILL_H);
        batch.setColor(1f, 1f, 1f, 1f);
        // Repère du seuil de la phase 2.
        float mark = fx + fw * Boss.SECOND_PHASE_THRESHOLD;
        batch.setColor(0.96f, 0.82f, 0.45f, 1f);
        batch.draw(ui.white, mark, y + FILL_Y - 2f, 1f, FILL_H + 4f);
        batch.setColor(1f, 1f, 1f, 1f);
        if (secondPhase) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.45f, 0.15f, 0.35f + 0.15f * (float) Math.sin(time * 9.0));
            batch.draw(ui.glow, fx - 10f, y - 8f, fw * ratio + 20f, 32f);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        batch.dispose();
        title.dispose();
        ui.dispose();
    }
}
