package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Les polices du jeu : Inter pour le texte (licence libre SIL OFL, voir {@code
 * assets/fonts/OFL.txt}) et Shojumaru pour les titres ({@link #title()}).
 */
public final class Fonts {

    /** Hauteur des capitales de la police par défaut de libGDX, en unités, à l'échelle 1. */
    private static final float DEFAULT_CAP_HEIGHT = 11f;

    private Fonts() {
    }

    /**
     * Crée une police. Chaque écran ou élément d'interface crée la sienne et la libère dans son
     * {@code dispose} : la police libère aussi sa texture.
     */
    public static BitmapFont create() {
        return load("ui");
    }

    /**
     * Crée la police des titres : Shojumaru (licence libre SIL OFL, voir {@code
     * assets/fonts/OFL-Shojumaru.txt}), aux lettres taillées comme au pinceau.
     */
    public static BitmapFont title() {
        return load("title");
    }

    private static BitmapFont load(String name) {
        ScaledFontData data = new ScaledFontData(Gdx.files.internal("fonts/" + name + ".fnt"));
        // Mipmaps : la police est réduite proprement, sans scintiller ni crénelage.
        Texture texture = new Texture(Gdx.files.internal("fonts/" + name + ".png"), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        BitmapFont font = new BitmapFont(data, new TextureRegion(texture), false);
        font.setOwnsTexture(true);
        font.setUseIntegerPositions(false);
        font.getData().setScale(1f);
        return font;
    }

    /**
     * Données de police dont l'échelle est ramenée à celle de la police par défaut de libGDX :
     * {@code setScale(1)} donne des capitales de {@link #DEFAULT_CAP_HEIGHT} unités.
     */
    private static final class ScaledFontData extends BitmapFont.BitmapFontData {

        private final float ratio;

        ScaledFontData(FileHandle file) {
            super(file, false);
            // capHeight est lu dans le fichier, à l'échelle 1 : on en déduit le rapport une fois.
            this.ratio = DEFAULT_CAP_HEIGHT / capHeight;
        }

        @Override
        public void setScale(float scaleX, float scaleY) {
            // Pendant le constructeur parent, ratio vaut encore 0 : on applique l'échelle telle quelle.
            float r = ratio == 0f ? 1f : ratio;
            super.setScale(scaleX * r, scaleY * r);
        }
    }
}
