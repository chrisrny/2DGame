package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.entity.Player;
import game.model.player.Experience;
import game.model.player.state.PlayerStateKind;
import game.model.relic.Relic;
import game.model.skill.Skill;
import java.util.List;
import java.util.Optional;

/**
 * Interface en jeu, en haut à gauche, dans le style du quartier (laque noire, or, vermillon) :
 * portrait du héros dans un médaillon, barre de vie, barre d'expérience et sceau du niveau, jauge
 * d'esquive, les deux compétences avec leur recharge, et les reliques déjà prises.
 */
public final class Hud implements Disposable {

    private static final float MARGIN = 8f;
    private static final float PORTRAIT = 48f;
    private static final float BAR_WIDTH = 164f;
    private static final float BAR_HEIGHT = 16f;
    /** Partie de la barre où se dessine le remplissage, entre les embouts. */
    private static final float FILL_INSET_X = 7f;
    private static final float FILL_Y = 4f;
    private static final float FILL_H = 8f;
    private static final float XP_WIDTH = 150f;
    private static final float DODGE_WIDTH = 70f;
    private static final float SLOT = 28f;
    private static final float SLOT_GAP = 6f;
    private static final float RELIC = 24f;
    /** Points de vie entre deux graduations de la barre. */
    private static final int HP_TICK = 20;

    /** Temps pendant lequel la vie perdue reste visible avant de fondre, en secondes. */
    private static final float TRAIL_DELAY = 0.35f;
    /** Vitesse à laquelle la vie perdue fond, en part de la barre par seconde. */
    private static final float TRAIL_SPEED = 0.9f;
    /** Durée du flash rouge d'un coup reçu, en secondes. */
    private static final float HURT_FLASH = 0.35f;
    /** Durée de l'éclat d'une compétence qui redevient prête, en secondes. */
    private static final float READY_FLASH = 0.45f;
    /** En dessous de cette part de vie, le voile rouge bat. */
    private static final float LOW_HP = 0.3f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = Fonts.create();
    private final UiAtlas ui = new UiAtlas();
    private final Texture heroTexture;
    private final TextureRegion heroHead;

    private float time;
    private float trail = 1f;
    private float trailWait;
    private int lastHp = -1;
    private float hurtFlash;
    private final float[] readyFlash = new float[2];
    private final boolean[] wasReady = {true, true};
    private boolean gamepadLabels;

    /**
     * Crée le HUD.
     *
     * @param width largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public Hud(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
        heroTexture = new Texture(Gdx.files.internal("sprites/hero.png"));
        heroTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        // La tête du héros, prise dans sa première image de repos.
        heroHead = new TextureRegion(heroTexture, 6, 0, 36, 31);
    }

    /**
     * Adapte le HUD à la nouvelle taille de la fenêtre.
     *
     * @param width largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine le HUD. À appeler après le monde, pour qu'il soit au-dessus.
     *
     * @param player le joueur dont on affiche l'état
     * @param experience l'expérience et le niveau du joueur
     */
    public void draw(Player player, Experience experience) {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        time += delta;
        float ratio = hpRatio(player);
        updateTrail(player, ratio, delta);

        viewport.apply();
        float w = viewport.getWorldWidth();
        float h = viewport.getWorldHeight();
        float px = MARGIN;
        float py = h - MARGIN - PORTRAIT;
        float bx = px + PORTRAIT + 2f;
        float by = h - MARGIN - 4f - BAR_HEIGHT;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawDanger(ratio, w, h);
        drawPortrait(player, px, py, ratio);
        drawHpBar(player, bx, by, ratio);
        float xpY = by - 5f;
        drawThinBar(ui.fillXp, bx + FILL_INSET_X, xpY, XP_WIDTH, xpRatio(experience));
        float dodgeY = xpY - 10f;
        drawDodge(player, bx + 4f, dodgeY);
        drawSeal(experience, px + PORTRAIT - 16f, py - 4f);
        float slotY = dodgeY - 6f - SLOT;
        drawSlot(0, Optional.of(player.skill1()), ui.iconShockwave, bx, slotY);
        drawSlot(1, player.skill2(), ui.iconFan, bx + SLOT + SLOT_GAP, slotY);
        batch.end();

        drawCooldowns(player, bx, slotY);

        batch.begin();
        drawSlotTexts(player, bx, slotY);
        drawRelics(player.relics(), px, slotY - 10f - RELIC);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updateTrail(Player player, float ratio, float delta) {
        if (lastHp >= 0 && player.hp() < lastHp) {
            hurtFlash = HURT_FLASH;
            trailWait = TRAIL_DELAY;
        }
        lastHp = player.hp();
        hurtFlash = Math.max(0f, hurtFlash - delta);
        if (ratio >= trail) {
            trail = ratio;                                   // soin : la barre monte tout de suite
        } else if (trailWait > 0f) {
            trailWait -= delta;
        } else {
            trail = Math.max(ratio, trail - TRAIL_SPEED * delta);
        }
    }

    // Voile rouge : il bat quand la vie est basse et flashe à chaque coup reçu.
    private void drawDanger(float ratio, float w, float h) {
        float alpha = hurtFlash / HURT_FLASH * 0.45f;
        if (ratio > 0f && ratio < LOW_HP) {
            alpha = Math.max(alpha, 0.28f + 0.14f * (float) Math.sin(time * 6.0));
        }
        if (alpha <= 0f) {
            return;
        }
        batch.setColor(0.85f, 0.05f, 0.05f, alpha);
        batch.draw(ui.vignette, 0f, 0f, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawPortrait(Player player, float x, float y, float ratio) {
        batch.draw(ui.portraitBack, x, y);
        boolean hurt = player.stateKind() == PlayerStateKind.HURT || hurtFlash > HURT_FLASH * 0.6f;
        if (player.stateKind() == PlayerStateKind.DEAD) {
            batch.setColor(0.35f, 0.32f, 0.36f, 1f);
        } else if (hurt) {
            batch.setColor(1f, 0.45f, 0.45f, 1f);
        } else if (ratio < LOW_HP) {
            float k = 0.8f + 0.2f * (float) Math.sin(time * 6.0);
            batch.setColor(1f, k, k, 1f);
        }
        batch.draw(heroHead, x + PORTRAIT / 2f - 18f, y + PORTRAIT / 2f - 16f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(ui.portraitFrame, x, y);
    }

    private void drawHpBar(Player player, float x, float y, float ratio) {
        float fx = x + FILL_INSET_X;
        float fw = BAR_WIDTH - 2f * FILL_INSET_X;
        UiAtlas.three(batch, ui.barLeft, ui.barMid, ui.barRight, x, y, BAR_WIDTH);
        batch.draw(ui.fillEmpty, fx, y + FILL_Y, fw, FILL_H);
        batch.draw(ui.fillTrail, fx, y + FILL_Y, fw * trail, FILL_H);
        batch.draw(ui.fillHp, fx, y + FILL_Y, fw * ratio, FILL_H);
        // Reflet qui glisse le long de la barre : elle paraît vivante.
        float shine = (time * 0.35f) % 1.6f;
        if (shine < 1f && ratio > 0f) {
            batch.setColor(1f, 1f, 1f, 0.22f);
            batch.draw(ui.white, fx + fw * ratio * shine, y + FILL_Y, 4f, FILL_H);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        // Graduations tous les 20 points de vie.
        batch.setColor(0f, 0f, 0f, 0.45f);
        for (int hp = HP_TICK; hp < player.maxHp(); hp += HP_TICK) {
            batch.draw(ui.white, fx + fw * hp / player.maxHp(), y + FILL_Y, 1f, FILL_H);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        String text = player.hp() + " / " + player.maxHp();
        font.getData().setScale(0.5f);
        shadowed(text, x, y + FILL_Y + FILL_H / 2f + font.getCapHeight() / 2f, BAR_WIDTH, 1f, 0.97f, 0.9f);
    }

    private void drawThinBar(TextureRegion fill, float x, float y, float width, float ratio) {
        batch.setColor(0.03f, 0.02f, 0.03f, 1f);
        batch.draw(ui.white, x - 1f, y - 1f, width + 2f, 5f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(ui.fillEmpty, x, y, width, 3f);
        batch.draw(fill, x, y, width * ratio, 3f);
    }

    private void drawDodge(Player player, float x, float y) {
        float ready = dodgeReadyRatio(player);
        batch.draw(ready >= 1f ? ui.pipFull : ui.pipEmpty, x, y - 2f);
        if (ready >= 1f) {
            float pulse = 0.25f + 0.2f * (float) Math.sin(time * 5.0);
            batch.setColor(0.4f, 1f, 0.9f, pulse);
            batch.draw(ui.glow, x - 6f, y - 8f, 20f, 20f);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        drawThinBar(ui.fillDodge, x + 11f, y + 1f, DODGE_WIDTH, ready);
        font.getData().setScale(0.38f);
        font.setColor(0.6f, 0.9f, 0.86f, 1f);
        font.draw(batch, "ESQUIVE", x + DODGE_WIDTH + 15f, y + 5f);
        font.setColor(1f, 1f, 1f, 1f);
    }

    private void drawSeal(Experience experience, float x, float y) {
        batch.draw(ui.seal, x, y);
        font.getData().setScale(0.55f);
        font.setColor(1f, 0.95f, 0.88f, 1f);
        font.draw(batch, String.valueOf(experience.level()), x, y + 9f + font.getCapHeight() / 2f, 18f, Align.center,
                false);
        font.setColor(1f, 1f, 1f, 1f);
    }

    private void drawSlot(int index, Optional<Skill> skill, TextureRegion icon, float x, float y) {
        boolean ready = skill.isPresent() && skill.get().isReady();
        if (ready && !wasReady[index]) {
            readyFlash[index] = READY_FLASH;
        }
        wasReady[index] = ready || skill.isEmpty();
        readyFlash[index] = Math.max(0f, readyFlash[index] - Math.min(Gdx.graphics.getDeltaTime(), 0.1f));
        batch.draw(ready ? ui.slotReady : ui.slot, x, y);
        if (skill.isEmpty()) {
            return;
        }
        if (!ready) {
            batch.setColor(0.55f, 0.55f, 0.6f, 1f);
        }
        batch.draw(icon, x + 2f, y + 2f);
        batch.setColor(1f, 1f, 1f, 1f);
        if (readyFlash[index] > 0f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            float k = readyFlash[index] / READY_FLASH;
            batch.setColor(0.5f, 1f, 0.95f, k);
            float s = SLOT + 20f * (1f - k);
            batch.draw(ui.glow, x + SLOT / 2f - s / 2f, y + SLOT / 2f - s / 2f, s, s);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    // Recharge : une part de cercle sombre qui se referme, comme une aiguille.
    private void drawCooldowns(Player player, float x, float y) {
        // batch.end() coupe le mélange : on le remet pour la part de cercle transparente.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.01f, 0.03f, 0.62f);
        cooldownPie(Optional.of(player.skill1()), x, y);
        cooldownPie(player.skill2(), x + SLOT + SLOT_GAP, y);
        shapes.end();
    }

    private void cooldownPie(Optional<Skill> skill, float x, float y) {
        if (skill.isEmpty() || skill.get().isReady()) {
            return;
        }
        float left = Math.clamp(skill.get().cooldownRatio(), 0f, 1f);
        shapes.arc(x + SLOT / 2f, y + SLOT / 2f, 11f, 90f, 360f * left, 32);
    }

    private void drawSlotTexts(Player player, float x, float y) {
        slotText(Optional.of(player.skill1()), gamepadLabels ? "RB" : "CLIC D", x, y);
        slotText(player.skill2(), gamepadLabels ? "LB" : "E", x + SLOT + SLOT_GAP, y);
    }

    /**
     * Choisit les touches affichées sous les compétences : celles de la manette ou du clavier.
     *
     * @param gamepad {@code true} si le joueur joue à la manette
     */
    public void setGamepadLabels(boolean gamepad) {
        this.gamepadLabels = gamepad;
    }

    private void slotText(Optional<Skill> skill, String key, float x, float y) {
        if (skill.isPresent() && !skill.get().isReady()) {
            font.getData().setScale(0.6f);
            String seconds = String.valueOf((int) Math.ceil(skill.get().cooldownLeft()));
            shadowed(seconds, x, y + SLOT / 2f + font.getCapHeight() / 2f, SLOT, 1f, 1f, 1f);
        }
        font.getData().setScale(0.34f);
        font.setColor(0.92f, 0.78f, 0.45f, 1f);
        font.draw(batch, key, x - 6f, y - 2f, SLOT + 12f, Align.center, false);
        font.setColor(1f, 1f, 1f, 1f);
    }

    private void drawRelics(List<Relic> relics, float x, float y) {
        for (int i = 0; i < relics.size(); i++) {
            float rx = x + i * (RELIC + 3f);
            batch.setColor(0f, 0f, 0f, 0.5f);
            batch.draw(ui.glow, rx - 3f, y - 3f, RELIC + 6f, RELIC + 6f);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(ui.icon(relics.get(i)), rx, y);
        }
    }

    // Texte avec une ombre portée d'un pixel, lisible sur n'importe quel fond.
    private void shadowed(String text, float x, float y, float width, float r, float g, float b) {
        font.setColor(0f, 0f, 0f, 0.85f);
        font.draw(batch, text, x + 0.7f, y - 0.7f, width, Align.center, false);
        font.setColor(r, g, b, 1f);
        font.draw(batch, text, x, y, width, Align.center, false);
        font.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Part de points de vie restants.
     *
     * @param player le joueur
     * @return un nombre entre 0 (mort) et 1 (vie pleine)
     */
    static float hpRatio(Player player) {
        if (player.maxHp() <= 0) {
            return 0f;
        }
        return Math.clamp((float) player.hp() / player.maxHp(), 0f, 1f);
    }

    /**
     * Avancement de la recharge de l'esquive.
     *
     * @param player le joueur
     * @return 0 juste après une esquive, puis de plus en plus près de 1 ; 1 quand l'esquive est
     *     prête
     */
    static float dodgeReadyRatio(Player player) {
        float cooldown = player.stats().dodgeCooldown();
        if (cooldown <= 0f) {
            return 1f;
        }
        return Math.clamp(1f - player.dodgeCooldownLeft() / cooldown, 0f, 1f);
    }

    /**
     * Avancement vers le niveau suivant.
     *
     * @param experience l'expérience du joueur
     * @return un nombre entre 0 (niveau tout juste atteint) et 1
     */
    static float xpRatio(Experience experience) {
        int needed = experience.xpToNextLevel();
        if (needed <= 0) {
            return 0f;
        }
        return Math.clamp((float) experience.xp() / needed, 0f, 1f);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        ui.dispose();
        heroTexture.dispose();
    }
}
