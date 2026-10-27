package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Vec2;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Effets autour du boss : onde de choc et fissures de lave quand la massue frappe, gerbes de
 * braises au passage en phase 2 et à la mort, tremblement de l'écran ({@code
 * assets/sprites/boss_fx.png}).
 */
public final class BossEffects implements Disposable {

    private static final int CELL = 128;
    private static final int RING_FRAMES = 4;
    private static final int CRACK_FRAME = 4;

    /** Durée de l'onde de choc, en secondes. */
    private static final float RING_DURATION = 0.28f;

    /** Durée des fissures au sol, en secondes. */
    private static final float CRACK_DURATION = 1.5f;

    // Tremblements : amplitude en unités, durée en secondes.
    private static final float IMPACT_SHAKE = 3f;
    private static final float IMPACT_SHAKE_TIME = 0.25f;
    private static final float PHASE_SHAKE = 4f;
    private static final float PHASE_SHAKE_TIME = 0.45f;
    private static final float DEATH_SHAKE = 3f;
    private static final float DEATH_SHAKE_TIME = 0.6f;

    /** Hauteur du torse au-dessus du centre du boss, d'où partent les braises. */
    private static final float CHEST_HEIGHT = 30f;

    // Pixel blanc de fx.png, coloré pour les braises.
    private static final int FX_WHITE_X = 62;

    /** Un effet qui vieillit à un endroit. */
    private static final class Mark {
        private final Vec2 center;
        private float time;

        private Mark(Vec2 center) {
            this.center = center;
        }
    }

    /** Une braise : position, vitesse, durée de vie. */
    private static final class Ember {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;

        private Ember(float x, float y, float vx, float vy, float life, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
        }
    }

    private final Texture texture;
    private final TextureRegion[] rings = new TextureRegion[RING_FRAMES];
    private final TextureRegion crack;
    private final TextureRegion white;
    private final List<Mark> ringMarks = new ArrayList<>();
    private final List<Mark> crackMarks = new ArrayList<>();
    private final List<Ember> embers = new ArrayList<>();
    private final Random random = new Random();
    private float shakeAmplitude;
    private float shakeLeft;
    private float shakeDuration = 1f;
    private float clock;

    /**
     * Charge les images des effets.
     *
     * @param fx la texture des petits effets, dont le pixel blanc sert aux braises
     */
    public BossEffects(Texture fx) {
        Objects.requireNonNull(fx, "fx");
        texture = new Texture(Gdx.files.internal("sprites/boss_fx.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[] row = TextureRegion.split(texture, CELL, CELL)[0];
        System.arraycopy(row, 0, rings, 0, RING_FRAMES);
        crack = row[CRACK_FRAME];
        white = new TextureRegion(fx, FX_WHITE_X, 0, 1, 1);
    }

    /**
     * La massue frappe le sol : onde, fissures, quelques braises et un tremblement.
     *
     * @param center le centre de la zone frappée
     */
    public void slamImpact(Vec2 center) {
        ringMarks.add(new Mark(center));
        crackMarks.add(new Mark(center));
        burst(center.x(), center.y(), 10, 60f);
        shake(IMPACT_SHAKE, IMPACT_SHAKE_TIME);
    }

    /**
     * Le boss passe en phase 2 : grande gerbe de braises et tremblement.
     *
     * @param boss la position du boss
     */
    public void phaseChange(Vec2 boss) {
        burst(boss.x(), boss.y() + CHEST_HEIGHT, 40, 110f);
        shake(PHASE_SHAKE, PHASE_SHAKE_TIME);
    }

    /**
     * Le boss meurt : braises et tremblement.
     *
     * @param boss la dernière position du boss
     */
    public void death(Vec2 boss) {
        burst(boss.x(), boss.y() + CHEST_HEIGHT, 30, 70f);
        shake(DEATH_SHAKE, DEATH_SHAKE_TIME);
    }

    private void burst(float x, float y, int count, float speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            float v = speed * (0.4f + random.nextFloat() * 0.6f);
            embers.add(new Ember(x, y, (float) Math.cos(angle) * v, (float) Math.sin(angle) * v * 0.7f + 30f,
                    0.5f + random.nextFloat() * 0.7f, 1f + random.nextInt(2)));
        }
    }

    private void shake(float amplitude, float duration) {
        // Le plus fort l'emporte : deux secousses en même temps ne s'additionnent pas.
        if (amplitude >= shakeAmplitude * (shakeLeft / shakeDuration)) {
            shakeAmplitude = amplitude;
            shakeLeft = duration;
            shakeDuration = duration;
        }
    }

    /**
     * Fait vieillir les effets. À appeler une fois par image tant que la partie avance.
     *
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(float delta) {
        clock += delta;
        shakeLeft = Math.max(0f, shakeLeft - delta);
        for (Mark mark : ringMarks) {
            mark.time += delta;
        }
        for (Mark mark : crackMarks) {
            mark.time += delta;
        }
        ringMarks.removeIf(mark -> mark.time >= RING_DURATION);
        crackMarks.removeIf(mark -> mark.time >= CRACK_DURATION);
        for (Ember ember : embers) {
            ember.life -= delta;
            ember.x += ember.vx * delta;
            ember.y += ember.vy * delta;
            // Freinées par l'air, elles finissent par monter avec la chaleur.
            ember.vx *= 1f - 2.5f * delta;
            ember.vy = ember.vy * (1f - 2.5f * delta) + 40f * delta;
        }
        embers.removeIf(ember -> ember.life <= 0f);
    }

    /** Décalage de caméra du tremblement en cours, qui s'affaiblit jusqu'à zéro. */
    public Vec2 shakeOffset() {
        if (shakeLeft <= 0f) {
            return Vec2.ZERO;
        }
        float strength = shakeAmplitude * (shakeLeft / shakeDuration);
        return new Vec2((float) Math.sin(clock * 95f) * strength, (float) Math.cos(clock * 73f) * strength * 0.7f);
    }

    /**
     * Dessine les fissures au sol, en lumière additive. Le {@link SpriteBatch} doit déjà être
     * ouvert ; le mélange normal est remis à la fin.
     *
     * @param batch le pinceau d'images
     */
    public void drawGround(SpriteBatch batch) {
        if (crackMarks.isEmpty()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Mark mark : crackMarks) {
            float fade = 1f - mark.time / CRACK_DURATION;
            batch.setColor(1f, 1f, 1f, fade * fade);
            batch.draw(crack, mark.center.x() - CELL / 2f, mark.center.y() - CELL / 2f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Dessine l'onde de choc et les braises. Le mélange additif doit déjà être réglé (section des
     * lueurs de {@link SceneRenderer}).
     *
     * @param batch le pinceau d'images
     */
    public void drawGlows(SpriteBatch batch) {
        for (Mark mark : ringMarks) {
            int index = Math.min(RING_FRAMES - 1, (int) (mark.time / RING_DURATION * RING_FRAMES));
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(rings[index], mark.center.x() - CELL / 2f, mark.center.y() - CELL / 2f);
        }
        for (Ember ember : embers) {
            float t = ember.life / ember.maxLife;
            // Du jaune clair au rouge en refroidissant.
            batch.setColor(1f, 0.35f + 0.55f * t, 0.15f + 0.35f * t * t, Math.min(1f, t * 1.5f));
            batch.draw(white, ember.x, ember.y, ember.size, ember.size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
