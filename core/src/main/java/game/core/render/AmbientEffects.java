package game.core.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import game.model.commun.Vec2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * L'air du quartier qui brûle : braises qui montent et fumée qui s'échappe des feux. Purement
 * décoratif, seulement autour de la vue.
 */
public final class AmbientEffects {

    /** Nombre de braises en l'air autour de la vue. */
    private static final int EMBERS = 80;

    /** Nouvelles bouffées de fumée par seconde et par feu visible. */
    private static final float SMOKE_RATE = 1.4f;

    /** Durée de vie d'une bouffée de fumée, en secondes. */
    private static final float SMOKE_LIFE = 2.6f;

    /** Opacité maximale de la fumée : un voile léger, pas un brouillard. */
    private static final float SMOKE_ALPHA = 0.16f;

    // Marge autour de la vue où l'on fait vivre braises et fumée, en unités.
    private static final float MARGIN = 60f;

    /** Une particule : braise ou bouffée de fumée. */
    private static final class Particle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;
        private final float hue;

        private Particle(float x, float y, float vx, float vy, float life, float size, float hue) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.hue = hue;
        }
    }

    private final List<Particle> embers = new ArrayList<>();
    private final List<Particle> smoke = new ArrayList<>();
    private final Random random = new Random();
    private float smokeDebt;

    /**
     * Fait vivre braises et fumée autour de la vue.
     *
     * @param center le centre de la vue
     * @param viewW largeur de la vue, en unités
     * @param viewH hauteur de la vue, en unités
     * @param fireSpots les endroits qui brûlent
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Vec2 center, float viewW, float viewH, List<Vec2> fireSpots, float delta) {
        float x0 = center.x() - viewW / 2f - MARGIN;
        float x1 = center.x() + viewW / 2f + MARGIN;
        float y0 = center.y() - viewH / 2f - MARGIN;
        float y1 = center.y() + viewH / 2f + MARGIN;
        List<Vec2> near = new ArrayList<>();
        for (Vec2 spot : fireSpots) {
            if (spot.x() > x0 && spot.x() < x1 && spot.y() > y0 && spot.y() < y1) {
                near.add(spot);
            }
        }
        // Braises : la plupart partent des feux proches, quelques-unes flottent n'importe où.
        while (embers.size() < EMBERS) {
            float x;
            float y;
            if (!near.isEmpty() && random.nextFloat() < 0.6f) {
                Vec2 spot = near.get(random.nextInt(near.size()));
                x = spot.x() + (random.nextFloat() - 0.5f) * 16f;
                y = spot.y() + random.nextFloat() * 10f;
            } else {
                x = x0 + random.nextFloat() * (x1 - x0);
                y = y0 + random.nextFloat() * (y1 - y0);
            }
            embers.add(new Particle(x, y, (random.nextFloat() - 0.5f) * 10f, 14f + random.nextFloat() * 22f,
                    1.5f + random.nextFloat() * 2.5f, random.nextFloat() < 0.25f ? 2f : 1f, random.nextFloat()));
        }
        smokeDebt += near.size() * SMOKE_RATE * delta;
        while (smokeDebt >= 1f && !near.isEmpty()) {
            smokeDebt -= 1f;
            Vec2 spot = near.get(random.nextInt(near.size()));
            smoke.add(new Particle(spot.x() + (random.nextFloat() - 0.5f) * 10f, spot.y() + 12f,
                    3f + random.nextFloat() * 4f, 10f + random.nextFloat() * 6f, SMOKE_LIFE, 18f, 0f));
        }
        if (near.isEmpty()) {
            smokeDebt = 0f;
        }
        step(embers, delta, x0, y0, x1, y1, true);
        step(smoke, delta, x0, y0, x1, y1, false);
    }

    private static void step(List<Particle> list, float delta, float x0, float y0, float x1, float y1, boolean sway) {
        for (Particle p : list) {
            p.life -= delta;
            if (sway) {
                p.vx += (float) Math.sin(p.life * 3f + p.hue * 10f) * 12f * delta;
            }
            p.x += p.vx * delta;
            p.y += p.vy * delta;
        }
        list.removeIf(p -> p.life <= 0f || p.x < x0 || p.x > x1 || p.y < y0 || p.y > y1);
    }

    /**
     * Dessine la fumée, en mélange normal. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param puff la bouffée de fumée douce
     */
    public void drawSmoke(SpriteBatch batch, TextureRegion puff) {
        for (Particle p : smoke) {
            float t = 1f - p.life / p.maxLife;                  // 0 à la naissance, 1 à la fin
            float size = p.size * (1f + t * 1.6f);
            float alpha = SMOKE_ALPHA * (float) Math.sin(Math.PI * t);
            batch.setColor(0.42f, 0.38f, 0.44f, alpha);
            batch.draw(puff, p.x - size / 2f, p.y - size / 2f, size, size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Dessine les braises, en lumière additive. Le {@link SpriteBatch} doit déjà être ouvert ; le
     * mélange normal est remis à la fin.
     *
     * @param batch le pinceau d'images
     * @param white un pixel blanc, coloré pour chaque braise
     */
    public void drawEmbers(SpriteBatch batch, TextureRegion white) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Particle p : embers) {
            float t = p.life / p.maxLife;
            float twinkle = 0.7f + 0.3f * (float) Math.sin(p.life * 9f + p.hue * 20f);
            batch.setColor(1f, 0.45f + 0.4f * p.hue, 0.15f + 0.2f * p.hue, Math.min(1f, t * 2f) * twinkle);
            batch.draw(white, p.x, p.y, p.size, p.size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
