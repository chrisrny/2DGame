package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Boss;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.skill.BladeFan;
import game.model.skill.Shockwave;
import game.model.world.Run;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Les effets des sorts du héros ({@code assets/sprites/skill_fx.png}) : c'est ce qui rend l'Onde de
 * choc et l'Éventail de lames spectaculaires.
 */
public final class SkillEffects implements Disposable {

    private static final int CELL = 128;
    private static final int SMALL = 32;

    // Durées, en secondes.
    private static final float HITSTOP = 0.07f;
    private static final float FLASH_TIME = 0.12f;
    private static final float RING_TIME = 0.26f;
    private static final float RING2_DELAY = 0.05f;
    private static final float SIGIL_TIME = 0.9f;
    private static final float CRACK_TIME = 1.3f;
    private static final float WAVE_LIGHT_TIME = 0.4f;
    private static final float MUZZLE_TIME = 0.16f;
    private static final float IMPACT_TIME = 0.22f;

    // Secousses : amplitude en unités, durée en secondes.
    private static final float WAVE_SHAKE = 3.5f;
    private static final float WAVE_SHAKE_TIME = 0.3f;
    private static final float FAN_SHAKE = 1.5f;
    private static final float FAN_SHAKE_TIME = 0.12f;

    /** Force du second passage du sceau et des fissures, par-dessus la nuit. */
    private static final float DECAL_GLOW = 0.45f;

    /** Nombre de positions gardées pour la traînée d'une lame. */
    private static final int TRAIL = 7;

    // Couleurs : bleu glacé et or pour l'onde, turquoise pour les lames.
    private static final float[] ICE = {0.55f, 0.8f, 1f};
    private static final float[] GOLD = {1f, 0.85f, 0.45f};
    private static final float[] JADE = {0.45f, 1f, 0.92f};

    /** Une Onde de choc en cours, à l'endroit où elle est partie. */
    private static final class Wave {
        private final Vec2 center;
        private float time;

        private Wave(Vec2 center) {
            this.center = center;
        }
    }

    /** Un effet bref orienté : éclat de départ de l'Éventail, entaille d'un coup de lame. */
    private static final class Mark {
        private final Vec2 center;
        private final float angle;
        private float time;

        private Mark(Vec2 center, float angle) {
            this.center = center;
            this.angle = angle;
        }
    }

    /** Une étincelle, tracée comme un petit trait dans le sens de sa course. */
    private static final class Spark {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float length;
        private final float[] color;

        private Spark(float x, float y, float vx, float vy, float life, float length, float[] color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.length = length;
            this.color = color;
        }
    }

    /** Ce que la vue garde d'une lame en vol : ses dernières positions et ses coups. */
    private static final class Trail {
        private final Deque<Vec2> points = new ArrayDeque<>();
        private int hits;
        private Vec2 last;
        private float angle;
        private float lifetime;
        private int pierce;
    }

    private final Texture texture;
    private final TextureRegion sigil;
    private final TextureRegion cracks;
    private final TextureRegion ring;
    private final TextureRegion flash;
    private final TextureRegion blade;
    private final TextureRegion streak;
    private final TextureRegion cross;
    private final TextureRegion star;
    private final List<Wave> waves = new ArrayList<>();
    private final List<Mark> muzzles = new ArrayList<>();
    private final List<Mark> impacts = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Map<Projectile, Trail> trails = new IdentityHashMap<>();
    private final Random random = new Random();
    private float lastShockwave = Float.POSITIVE_INFINITY;
    private float lastFan = Float.POSITIVE_INFINITY;
    private float hitstop;
    private float shakeAmplitude;
    private float shakeLeft;
    private float shakeDuration = 1f;
    private float clock;

    /** Charge les images des effets. */
    public SkillEffects() {
        texture = new Texture(Gdx.files.internal("sprites/skill_fx.png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        sigil = new TextureRegion(texture, 0, 0, CELL, CELL);
        cracks = new TextureRegion(texture, CELL, 0, CELL, CELL);
        ring = new TextureRegion(texture, 2 * CELL, 0, CELL, CELL);
        flash = new TextureRegion(texture, 3 * CELL, 0, CELL, CELL);
        blade = new TextureRegion(texture, 0, CELL, SMALL, SMALL);
        streak = new TextureRegion(texture, SMALL, CELL, SMALL, SMALL);
        cross = new TextureRegion(texture, 2 * SMALL, CELL, SMALL, SMALL);
        star = new TextureRegion(texture, 3 * SMALL, CELL, SMALL, SMALL);
    }

    /**
     * Repère les sorts qui viennent de partir et les coups des lames, puis fait vieillir les
     * effets. À appeler une fois par image tant que la partie avance.
     *
     * @param run la partie
     * @param hero la position du héros à l'écran (lissée)
     * @param smooth donne la position lissée d'un projectile
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(Run run, Vec2 hero, Function<Projectile, Vec2> smooth, float delta) {
        clock += delta;
        hitstop = Math.max(0f, hitstop - delta);
        shakeLeft = Math.max(0f, shakeLeft - delta);
        Player player = run.player();

        float sinceWave = player.skill1().timeSinceActivation();
        if (sinceWave < lastShockwave) {
            shockwave(hero);
        }
        lastShockwave = sinceWave;
        float sinceFan = player.skill2().map(skill -> skill.timeSinceActivation()).orElse(Float.POSITIVE_INFINITY);
        if (sinceFan < lastFan) {
            fan(hero, player.facing());
        }
        lastFan = sinceFan;

        followBlades(run.map().projectiles(), smooth);
        age(delta);
    }

    private void shockwave(Vec2 center) {
        waves.add(new Wave(center));
        hitstop = HITSTOP;
        shake(WAVE_SHAKE, WAVE_SHAKE_TIME);
        for (int i = 0; i < 46; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            float v = 230f + random.nextFloat() * 260f;
            float[] color = random.nextFloat() < 0.6f ? GOLD : ICE;
            sparks.add(new Spark(center.x(), center.y(), (float) Math.cos(a) * v, (float) Math.sin(a) * v,
                    0.22f + random.nextFloat() * 0.3f, 6f + random.nextFloat() * 8f, color));
        }
    }

    private void fan(Vec2 hero, Vec2 aim) {
        float angle = (float) Math.toDegrees(Math.atan2(aim.y(), aim.x()));
        Vec2 start = hero.add(aim.scale(Player.RADIUS + 4f));
        muzzles.add(new Mark(start, angle));
        shake(FAN_SHAKE, FAN_SHAKE_TIME);
        float spread = BladeFan.SPREAD_DEGREES;
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(angle - spread / 2f + random.nextFloat() * spread);
            float v = 160f + random.nextFloat() * 160f;
            sparks.add(new Spark(start.x(), start.y(), (float) Math.cos(a) * v, (float) Math.sin(a) * v,
                    0.12f + random.nextFloat() * 0.15f, 5f + random.nextFloat() * 5f, JADE));
        }
    }

    // Suit chaque lame du joueur : traînée, coups portés, fin de course.
    private void followBlades(List<Projectile> projectiles, Function<Projectile, Vec2> smooth) {
        for (Projectile projectile : projectiles) {
            if (projectile.faction() != Faction.PLAYER) {
                continue;
            }
            Trail trail = trails.computeIfAbsent(projectile, p -> new Trail());
            Vec2 p = smooth.apply(projectile);
            Vec2 v = projectile.velocity();
            trail.angle = (float) Math.toDegrees(Math.atan2(v.y(), v.x()));
            trail.points.addFirst(p);
            while (trail.points.size() > TRAIL) {
                trail.points.removeLast();
            }
            if (projectile.hits() > trail.hits) {
                bladeHit(p, trail.angle);
                trail.hits = projectile.hits();
            }
            trail.last = p;
            trail.lifetime = projectile.lifetime();
            trail.pierce = projectile.pierce();
        }
        Iterator<Map.Entry<Projectile, Trail>> it = trails.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, Trail> entry = it.next();
            if (projectiles.contains(entry.getKey())) {
                continue;
            }
            Trail trail = entry.getValue();
            // Disparue avant la fin de sa course sans avoir épuisé ses cibles : elle a heurté un
            // mur ou touché sa dernière cible entre deux images.
            if (trail.last != null && trail.lifetime > 0.03f) {
                if (entry.getKey().hits() > trail.hits) {
                    bladeHit(trail.last, trail.angle);
                } else {
                    fizzle(trail.last);
                }
            } else if (trail.last != null) {
                fizzle(trail.last);
            }
            it.remove();
        }
    }

    private void bladeHit(Vec2 at, float angle) {
        impacts.add(new Mark(at, angle + 45f + random.nextFloat() * 20f));
        for (int i = 0; i < 9; i++) {
            double a = Math.toRadians(angle + (random.nextFloat() - 0.5f) * 140f);
            float v = 120f + random.nextFloat() * 160f;
            sparks.add(new Spark(at.x(), at.y(), (float) Math.cos(a) * v, (float) Math.sin(a) * v,
                    0.12f + random.nextFloat() * 0.18f, 4f + random.nextFloat() * 5f, i % 3 == 0 ? GOLD : JADE));
        }
    }

    private void fizzle(Vec2 at) {
        for (int i = 0; i < 4; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            float v = 40f + random.nextFloat() * 60f;
            sparks.add(new Spark(at.x(), at.y(), (float) Math.cos(a) * v, (float) Math.sin(a) * v,
                    0.12f + random.nextFloat() * 0.1f, 3f, JADE));
        }
    }

    private void age(float delta) {
        for (Wave wave : waves) {
            wave.time += delta;
        }
        waves.removeIf(wave -> wave.time >= CRACK_TIME);
        for (Mark mark : muzzles) {
            mark.time += delta;
        }
        muzzles.removeIf(mark -> mark.time >= MUZZLE_TIME);
        for (Mark mark : impacts) {
            mark.time += delta;
        }
        impacts.removeIf(mark -> mark.time >= IMPACT_TIME);
        for (Spark spark : sparks) {
            spark.life -= delta;
            spark.x += spark.vx * delta;
            spark.y += spark.vy * delta;
            spark.vx *= 1f - 5f * delta;
            spark.vy *= 1f - 5f * delta;
        }
        sparks.removeIf(spark -> spark.life <= 0f);
    }

    private void shake(float amplitude, float duration) {
        if (amplitude >= shakeAmplitude * (shakeLeft / shakeDuration)) {
            shakeAmplitude = amplitude;
            shakeLeft = duration;
            shakeDuration = duration;
        }
    }

    /**
     * Arrêt sur image : pendant ce court instant après l'Onde de choc, l'écran de jeu ne fait pas
     * avancer le modèle. Le coup « pèse » ; les effets, eux, continuent.
     */
    public boolean frozen() {
        return hitstop > 0f;
    }

    /** Décalage de caméra du tremblement en cours. */
    public Vec2 shakeOffset() {
        if (shakeLeft <= 0f) {
            return Vec2.ZERO;
        }
        float strength = shakeAmplitude * (shakeLeft / shakeDuration);
        return new Vec2((float) Math.sin(clock * 91f) * strength, (float) Math.cos(clock * 67f) * strength * 0.7f);
    }

    /**
     * Ajoute les lumières des sorts à celles de l'image : éclat de l'onde, départ de l'éventail,
     * lames en vol, coups portés.
     *
     * @param lights les lumières de l'image
     */
    public void addLights(List<Light> lights) {
        for (Wave wave : waves) {
            if (wave.time < WAVE_LIGHT_TIME) {
                float t = wave.time / WAVE_LIGHT_TIME;
                float radius = 70f + Shockwave.RADIUS * 2.2f * easeOut(t);
                lights.add(Light.steady(wave.center.x(), wave.center.y(), radius, 0.7f, 0.85f, 1f, 1f - t));
            }
        }
        for (Mark mark : muzzles) {
            float t = mark.time / MUZZLE_TIME;
            lights.add(Light.steady(mark.center.x(), mark.center.y(), 110f, 0.5f, 1f, 0.9f, 1f - t));
        }
        for (Trail trail : trails.values()) {
            Vec2 p = trail.points.peekFirst();
            if (p != null) {
                lights.add(Light.steady(p.x(), p.y(), 44f, JADE[0], JADE[1], JADE[2], 0.85f));
            }
        }
        for (Mark mark : impacts) {
            float t = mark.time / IMPACT_TIME;
            lights.add(Light.steady(mark.center.x(), mark.center.y(), 50f, 1f, 0.95f, 0.7f, 1f - t));
        }
    }

    /**
     * Dessine ce qui est posé au sol : sceau et fissures de l'onde. Le {@link SpriteBatch} doit
     * être ouvert ; le mélange normal est remis à la fin.
     *
     * @param batch le pinceau d'images
     */
    public void drawGround(SpriteBatch batch) {
        if (waves.isEmpty()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawDecals(batch, 1f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // Sceau et fissures.
    private void drawDecals(SpriteBatch batch, float strength) {
        for (Wave wave : waves) {
            float x = wave.center.x();
            float y = wave.center.y();
            if (wave.time < SIGIL_TIME) {
                float t = wave.time / SIGIL_TIME;
                float size = Shockwave.RADIUS * 2f * (0.55f + 0.45f * easeOut(Math.min(1f, wave.time / 0.12f)));
                float spin = wave.time * 40f;
                batch.setColor(ICE[0], ICE[1], ICE[2], strength * (1f - t) * (1f - t));
                batch.draw(sigil, x - size / 2f, y - size / 2f, size / 2f, size / 2f, size, size, 1f, 1f, spin);
            }
            float t = wave.time / CRACK_TIME;
            float size = Shockwave.RADIUS * 1.6f;
            batch.setColor(0.8f, 0.92f, 1f, strength * Math.min(1f, wave.time / 0.04f) * (1f - t) * (1f - t));
            batch.draw(cracks, x - size / 2f, y - size / 2f, size, size);
        }
    }

    /**
     * Dessine une lame de l'Éventail, à sa place dans le tri par profondeur.
     *
     * @param batch le pinceau d'images
     * @param projectile la lame
     * @param position sa position lissée
     */
    public void drawBlade(SpriteBatch batch, Projectile projectile, Vec2 position) {
        Vec2 v = projectile.velocity();
        float angle = (float) Math.toDegrees(Math.atan2(v.y(), v.x()));
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(blade, position.x() - SMALL / 2f, position.y() - SMALL / 2f, SMALL / 2f, SMALL / 2f,
                SMALL, SMALL, 1f, 1f, angle);
    }

    /**
     * Dessine tout ce qui brille par-dessus la nuit : éclairs, anneaux, traînées, entailles,
     * étincelles, étoiles des étourdis. Le mélange additif doit déjà être réglé.
     *
     * @param batch le pinceau d'images
     * @param enemies les ennemis, pour les étoiles des étourdis
     * @param smoothPos donne la position lissée d'un ennemi
     */
    public void drawGlows(SpriteBatch batch, List<Enemy> enemies, Function<Enemy, Vec2> smoothPos) {
        drawDecals(batch, DECAL_GLOW);
        for (Wave wave : waves) {
            drawWave(batch, wave);
        }
        for (Mark mark : muzzles) {
            drawMuzzle(batch, mark);
        }
        for (Trail trail : trails.values()) {
            drawTrail(batch, trail);
        }
        for (Mark mark : impacts) {
            float t = mark.time / IMPACT_TIME;
            float size = SMALL * (0.9f + 0.6f * easeOut(t));
            batch.setColor(1f, 0.97f, 0.85f, 1f - t);
            batch.draw(cross, mark.center.x() - size / 2f, mark.center.y() - size / 2f, size / 2f, size / 2f,
                    size, size, 1f, 1f, mark.angle);
            float f = 26f * (1f - t);
            batch.setColor(JADE[0], JADE[1], JADE[2], 0.8f * (1f - t));
            batch.draw(flash, mark.center.x() - f / 2f, mark.center.y() - f / 2f, f, f);
        }
        for (Spark spark : sparks) {
            float t = spark.life / spark.maxLife;
            float angle = (float) Math.toDegrees(Math.atan2(spark.vy, spark.vx));
            float len = spark.length * (0.4f + 0.6f * t);
            batch.setColor(spark.color[0], spark.color[1], spark.color[2], Math.min(1f, t * 1.6f));
            batch.draw(streak, spark.x - len, spark.y - 1.5f, len, 1.5f, len, 3f, 1f, 1f, angle);
        }
        for (Enemy enemy : enemies) {
            if (enemy.isStunned() && !(enemy instanceof Boss)) {
                drawStars(batch, enemy, smoothPos.apply(enemy));
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawWave(SpriteBatch batch, Wave wave) {
        float x = wave.center.x();
        float y = wave.center.y();
        if (wave.time < FLASH_TIME) {
            float t = wave.time / FLASH_TIME;
            float size = 40f + 110f * easeOut(t);
            batch.setColor(1f, 1f, 1f, 1f - t);
            batch.draw(flash, x - size / 2f, y - size / 2f, size, size);
        }
        // Premier anneau, or, au rayon exact de l'onde : c'est la zone touchée.
        ring(batch, x, y, wave.time, RING_TIME, Shockwave.RADIUS, GOLD);
        // Second anneau, glacé, un peu en retard et un peu plus large.
        ring(batch, x, y, wave.time - RING2_DELAY, RING_TIME * 1.3f, Shockwave.RADIUS * 1.15f, ICE);
    }

    private void ring(SpriteBatch batch, float x, float y, float time, float duration, float radius, float[] c) {
        if (time < 0f || time >= duration) {
            return;
        }
        float t = time / duration;
        // L'anneau de la texture a son bord à 61 px sur 64 : on corrige pour tomber sur le rayon voulu.
        float size = 2f * radius * easeOut(t) * (64f / 61f);
        float alpha = t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
        batch.setColor(c[0], c[1], c[2], alpha);
        batch.draw(ring, x - size / 2f, y - size / 2f, size, size);
        batch.setColor(1f, 1f, 1f, alpha * 0.5f);
        batch.draw(ring, x - size / 2f, y - size / 2f, size, size);
    }

    private void drawMuzzle(SpriteBatch batch, Mark mark) {
        float t = mark.time / MUZZLE_TIME;
        float x = mark.center.x();
        float y = mark.center.y();
        float size = 34f + 20f * t;
        batch.setColor(JADE[0], JADE[1], JADE[2], 1f - t);
        batch.draw(flash, x - size / 2f, y - size / 2f, size, size);
        // Un trait par lame, le long de sa direction de départ.
        float spread = BladeFan.SPREAD_DEGREES;
        float step = spread / (BladeFan.BLADE_COUNT - 1);
        float len = 18f + 26f * easeOut(t);
        batch.setColor(1f, 1f, 1f, 1f - t);
        for (int i = 0; i < BladeFan.BLADE_COUNT; i++) {
            float a = mark.angle - spread / 2f + i * step;
            batch.draw(streak, x, y - 1.5f, 0f, 1.5f, len, 3f, 1f, 1f, a);
        }
    }

    private void drawTrail(SpriteBatch batch, Trail trail) {
        int i = 0;
        Vec2 previous = null;
        for (Vec2 p : trail.points) {
            float k = 1f - (float) i / TRAIL;
            if (i > 0) {
                // Images rémanentes de la lame, de plus en plus pâles.
                batch.setColor(JADE[0], JADE[1], JADE[2], 0.45f * k * k);
                batch.draw(blade, p.x() - SMALL / 2f, p.y() - SMALL / 2f, SMALL / 2f, SMALL / 2f, SMALL, SMALL,
                        1f, 1f, trail.angle);
            }
            if (previous != null) {
                // Traînée continue entre deux positions.
                float dx = previous.x() - p.x();
                float dy = previous.y() - p.y();
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 0.5f) {
                    batch.setColor(JADE[0], JADE[1], JADE[2], 0.7f * k);
                    batch.draw(streak, p.x(), p.y() - 1.5f, 0f, 1.5f, len, 3f, 1f, 1f, trail.angle);
                }
            }
            previous = p;
            i++;
        }
        Vec2 head = trail.points.peekFirst();
        if (head != null) {
            float size = 22f;
            batch.setColor(JADE[0], JADE[1], JADE[2], 0.55f);
            batch.draw(flash, head.x() - size / 2f, head.y() - size / 2f, size, size);
        }
    }

    // Trois étoiles qui tournent au-dessus de la tête d'un ennemi étourdi.
    private void drawStars(SpriteBatch batch, Enemy enemy, Vec2 p) {
        float head = headHeight(enemy);
        float cx = p.x();
        float cy = p.y() + head;
        for (int k = 0; k < 3; k++) {
            double a = clock * 7.0 + k * Math.PI * 2 / 3;
            float sx = cx + (float) Math.cos(a) * 9f;
            float sy = cy + (float) Math.sin(a) * 3f;
            float size = 9f + (float) Math.sin(a) * 2f;
            batch.setColor(GOLD[0], GOLD[1], GOLD[2], 0.95f);
            batch.draw(star, sx - size / 2f, sy - size / 2f, size, size);
        }
    }

    // Hauteur du haut de la tête au-dessus de la position, selon la silhouette.
    private static float headHeight(Enemy enemy) {
        if (enemy instanceof Charger) {
            return 40f;
        }
        if (enemy instanceof Archer) {
            return 30f;
        }
        return 28f;
    }

    private static float easeOut(float t) {
        float u = 1f - Math.clamp(t, 0f, 1f);
        return 1f - u * u * u;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
