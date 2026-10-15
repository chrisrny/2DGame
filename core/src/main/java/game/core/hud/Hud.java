package game.core.hud;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.entity.Player;

/**
 * Interface en jeu, dessinée par-dessus le monde : barre de points de vie et jauge de
 * recharge de l'esquive, en haut à gauche.
 *
 * <p>Le HUD a sa propre caméra, qui ne bouge jamais : la caméra du jeu suit le joueur sur la
 * map, celle du HUD reste fixe, donc la barre reste toujours au même endroit. Il utilise les
 * mêmes unités logiques que le jeu (640x360 dans un {@link FitViewport}) : il s'agrandit avec
 * la fenêtre et garde les mêmes bandes noires.</p>
 *
 * <p>Le HUD lit le joueur, il ne le modifie jamais.</p>
 */
public final class Hud implements Disposable {

    /** Distance entre le HUD et les bords de l'écran logique, en unités. */
    private static final float MARGIN = 12f;

    /** Largeur des barres, en unités. */
    private static final float BAR_WIDTH = 120f;

    /** Hauteur de la barre de points de vie, en unités. */
    private static final float HP_HEIGHT = 10f;

    /** Hauteur de la jauge d'esquive, en unités. */
    private static final float DODGE_HEIGHT = 4f;

    /** Espace vertical entre la barre de vie et la jauge d'esquive, en unités. */
    private static final float BAR_GAP = 4f;

    /** Espace entre la barre de vie et le texte des points de vie, en unités. */
    private static final float TEXT_GAP = 6f;

    /** Épaisseur du cadre sombre autour des barres, en unités. */
    private static final float FRAME = 1f;

    /** Échelle de la police par défaut : à 1, elle est trop grande pour un écran de 360 de haut. */
    private static final float FONT_SCALE = 0.6f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    /**
     * Crée le HUD.
     *
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public Hud(float width, float height) {
        this.viewport = new FitViewport(width, height, camera);
        font.getData().setScale(FONT_SCALE);
        font.setUseIntegerPositions(false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    /**
     * Adapte le HUD à la nouvelle taille de la fenêtre.
     *
     * @param width  largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        // true : la caméra du HUD reste centrée sur l'écran logique, elle ne bouge jamais.
        viewport.update(width, height, true);
    }

    /**
     * Dessine le HUD. À appeler après le monde, pour qu'il soit au-dessus.
     *
     * @param player le joueur dont on affiche l'état
     */
    public void draw(Player player) {
        viewport.apply();
        float hpY = viewport.getWorldHeight() - MARGIN - HP_HEIGHT;
        float dodgeY = hpY - BAR_GAP - DODGE_HEIGHT;
        float dodgeRatio = dodgeReadyRatio(player);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawBar(hpY, HP_HEIGHT, hpRatio(player), 0.85f, 0.20f, 0.20f);
        if (dodgeRatio >= 1f) {
            drawBar(dodgeY, DODGE_HEIGHT, 1f, 0.35f, 0.95f, 0.95f);
        } else {
            drawBar(dodgeY, DODGE_HEIGHT, dodgeRatio, 0.30f, 0.45f, 0.55f);
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float textX = MARGIN + BAR_WIDTH + TEXT_GAP;
        float textY = hpY + HP_HEIGHT / 2f + font.getCapHeight() / 2f;
        font.draw(batch, player.hp() + " / " + player.maxHp(), textX, textY);
        batch.end();
    }

    /**
     * Dessine une barre : un cadre sombre, un fond gris, puis un remplissage proportionnel.
     * Le {@link ShapeRenderer} doit déjà être ouvert en mode {@code Filled}.
     */
    private void drawBar(float y, float height, float ratio, float r, float g, float b) {
        shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
        shapes.rect(MARGIN - FRAME, y - FRAME, BAR_WIDTH + 2f * FRAME, height + 2f * FRAME);
        shapes.setColor(0.18f, 0.18f, 0.20f, 1f);
        shapes.rect(MARGIN, y, BAR_WIDTH, height);
        shapes.setColor(r, g, b, 1f);
        shapes.rect(MARGIN, y, BAR_WIDTH * ratio, height);
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
     * @return 0 juste après une esquive, puis de plus en plus près de 1 ; 1 quand l'esquive est prête
     */
    static float dodgeReadyRatio(Player player) {
        float cooldown = player.stats().dodgeCooldown();
        if (cooldown <= 0f) {
            return 1f;
        }
        return Math.clamp(1f - player.dodgeCooldownLeft() / cooldown, 0f, 1f);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
