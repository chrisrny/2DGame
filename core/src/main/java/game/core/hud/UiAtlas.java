package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.relic.AshBoots;
import game.model.relic.Echo;
import game.model.relic.EmberHeart;
import game.model.relic.EmberStep;
import game.model.relic.HunterEye;
import game.model.relic.Relic;
import game.model.relic.ShadowFeather;
import game.model.relic.SharpenedBlade;
import game.model.relic.VampireFang;

/**
 * Les images de l'interface ({@code assets/sprites/ui.png}) : laque noire, or et vermillon,
 * comme le quartier. Cadres, barres, cases de compétence, icônes des compétences et des
 * reliques, carte de relique, ruban d'encre, et le voile ({@code vignette.png}).
 *
 * <p>Une seule texture pour tout le HUD : les éléments qui partagent un {@link SpriteBatch} se
 * dessinent d'un coup, sans changer de texture.</p>
 */
public final class UiAtlas implements Disposable {

    private final Texture texture;
    private final Texture vignetteTexture;

    final TextureRegion portraitFrame;
    final TextureRegion portraitBack;
    final TextureRegion barLeft;
    final TextureRegion barMid;
    final TextureRegion barRight;
    final TextureRegion fillHp;
    final TextureRegion fillTrail;
    final TextureRegion fillXp;
    final TextureRegion fillDodge;
    final TextureRegion fillBoss;
    final TextureRegion fillEmpty;
    final TextureRegion slot;
    final TextureRegion slotReady;
    final TextureRegion seal;
    final TextureRegion key;
    final TextureRegion pipFull;
    final TextureRegion pipEmpty;
    final TextureRegion iconShockwave;
    final TextureRegion iconFan;
    final TextureRegion bossLeft;
    final TextureRegion bossMid;
    final TextureRegion bossRight;
    final TextureRegion plaqueLeft;
    final TextureRegion plaqueMid;
    final TextureRegion plaqueRight;
    final TextureRegion ribbon;
    final TextureRegion card;
    final TextureRegion cardDisc;
    final TextureRegion glow;
    final TextureRegion white;
    final TextureRegion vignette;
    private final TextureRegion[] relics = new TextureRegion[8];

    /** Charge les images de l'interface. */
    public UiAtlas() {
        texture = new Texture(Gdx.files.internal("sprites/ui.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        vignetteTexture = new Texture(Gdx.files.internal("sprites/vignette.png"));
        vignetteTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        portraitFrame = region(0, 0, 48, 48);
        portraitBack = region(48, 0, 48, 48);
        barLeft = region(96, 0, 10, 16);
        barMid = region(106, 0, 1, 16);
        barRight = region(107, 0, 10, 16);
        fillHp = region(120, 0, 1, 10);
        fillTrail = region(121, 0, 1, 10);
        fillXp = region(122, 0, 1, 4);
        fillDodge = region(123, 0, 1, 4);
        fillBoss = region(124, 0, 1, 8);
        fillEmpty = region(125, 0, 1, 10);
        slot = region(128, 0, 28, 28);
        slotReady = region(156, 0, 28, 28);
        seal = region(184, 0, 18, 18);
        key = region(202, 0, 14, 10);
        pipFull = region(216, 0, 8, 8);
        pipEmpty = region(224, 0, 8, 8);
        iconShockwave = region(0, 48, 24, 24);
        iconFan = region(24, 48, 24, 24);
        for (int i = 0; i < relics.length; i++) {
            relics[i] = region(48 + 24 * i, 48, 24, 24);
        }
        bossLeft = region(0, 80, 22, 20);
        bossMid = region(22, 80, 1, 20);
        bossRight = region(23, 80, 22, 20);
        plaqueLeft = region(48, 80, 8, 20);
        plaqueMid = region(56, 80, 1, 20);
        plaqueRight = region(57, 80, 8, 20);
        ribbon = region(0, 104, 300, 48);
        card = region(300, 0, 152, 200);
        cardDisc = region(452, 0, 52, 52);
        glow = region(452, 52, 52, 52);
        white = region(122, 0, 1, 1);
        vignette = new TextureRegion(vignetteTexture);
    }

    private TextureRegion region(int x, int y, int w, int h) {
        return new TextureRegion(texture, x, y, w, h);
    }

    /**
     * Icône d'une relique, d'après son type.
     *
     * @param relic la relique
     * @return son icône de 24x24
     */
    public TextureRegion icon(Relic relic) {
        int index;
        if (relic instanceof HunterEye) {
            index = 0;
        } else if (relic instanceof SharpenedBlade) {
            index = 1;
        } else if (relic instanceof EmberHeart) {
            index = 2;
        } else if (relic instanceof VampireFang) {
            index = 3;
        } else if (relic instanceof Echo) {
            index = 4;
        } else if (relic instanceof EmberStep) {
            index = 5;
        } else if (relic instanceof AshBoots) {
            index = 6;
        } else if (relic instanceof ShadowFeather) {
            index = 7;
        } else {
            index = 1;
        }
        return relics[index];
    }

    /**
     * Dessine un cadre en trois morceaux (embout, milieu étiré, embout) sur une largeur donnée.
     *
     * @param batch le pinceau, ouvert
     * @param left  embout gauche
     * @param mid   colonne du milieu, étirée
     * @param right embout droit
     * @param x     bord gauche
     * @param y     bas
     * @param width largeur totale
     */
    static void three(SpriteBatch batch, TextureRegion left, TextureRegion mid, TextureRegion right,
            float x, float y, float width) {
        float lw = left.getRegionWidth();
        float rw = right.getRegionWidth();
        batch.draw(left, x, y);
        batch.draw(mid, x + lw, y, width - lw - rw, mid.getRegionHeight());
        batch.draw(right, x + width - rw, y);
    }

    @Override
    public void dispose() {
        texture.dispose();
        vignetteTexture.dispose();
    }
}
