package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.List;

/**
 * La nuit et ses lumières : un calque d'obscurité troué de taches de lumière chaudes, posé sur la
 * scène ({@code assets/sprites/light.png}).
 */
public final class LightingRenderer implements Disposable {

    // Couleur de la nuit. Avec la multiplication par deux, 0,5 laisserait la scène telle quelle.
    private static final float NIGHT_R = 0.25f;
    private static final float NIGHT_G = 0.215f;
    private static final float NIGHT_B = 0.31f;

    /** Part de chaque lumière dans le calque : avec la multiplication par deux, 0,5 double l'éclairage. */
    private static final float LIGHT_SCALE = 0.5f;

    /** Vitesse du vacillement des flammes, en radians par seconde. */
    private static final float FLICKER_SPEED = 11f;

    private final FrameBuffer buffer;
    private final TextureRegion result;
    private final Texture texture;
    private final TextureRegion glow;
    private final TextureRegion smoke;
    private final float viewW;
    private final float viewH;

    /**
     * Prépare l'image hors écran et la tache de lumière.
     *
     * @param viewW largeur de la vue, en unités du monde
     * @param viewH hauteur de la vue, en unités du monde
     */
    public LightingRenderer(float viewW, float viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int) viewW, (int) viewH, false);
        // Lissage : le calque est agrandi à l'écran, la lumière doit rester douce.
        buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        result = new TextureRegion(buffer.getColorBufferTexture());
        result.flip(false, true);
        texture = new Texture(Gdx.files.internal("sprites/light.png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        glow = new TextureRegion(texture, 0, 0, 64, 64);
        smoke = new TextureRegion(texture, 64, 0, 64, 64);
    }

    /** @return la bouffée de fumée douce, pour {@link AmbientEffects} */
    public TextureRegion smoke() {
        return smoke;
    }

    /**
     * Construit le calque de lumière et le pose sur la scène déjà dessinée.
     *
     * @param batch le pinceau d'images (fermé à l'appel, refermé au retour)
     * @param camera la caméra du monde
     * @param viewport la vue de l'écran de jeu, remise en place après l'image hors écran
     * @param lights les lumières, en unités du monde (celles hors de la vue sont ignorées)
     * @param time temps écoulé, en secondes, pour le vacillement
     */
    public void apply(SpriteBatch batch, OrthographicCamera camera, Viewport viewport, List<Light> lights, float time) {
        float left = camera.position.x - viewW / 2f;
        float bottom = camera.position.y - viewH / 2f;

        buffer.begin();
        Gdx.gl.glClearColor(NIGHT_R, NIGHT_G, NIGHT_B, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Light light : lights) {
            float r = light.radius();
            if (light.x() + r < left || light.x() - r > left + viewW
                    || light.y() + r < bottom || light.y() - r > bottom + viewH) {
                continue;
            }
            float k = light.strength() * LIGHT_SCALE;
            if (light.flicker() > 0f) {
                // Deux sinus de vitesses différentes : un vacillement qui ne se répète pas trop.
                float wave = (float) (Math.sin(time * FLICKER_SPEED + light.phase())
                        + 0.5 * Math.sin(time * FLICKER_SPEED * 2.3 + light.phase() * 1.7)) / 1.5f;
                k *= 1f + light.flicker() * wave;
                r *= 1f + light.flicker() * 0.3f * wave;
            }
            batch.setColor(light.r() * k, light.g() * k, light.b() * k, 1f);
            batch.draw(glow, light.x() - r, light.y() - r, r * 2f, r * 2f);
        }
        batch.end();
        buffer.end();
        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Multiplication par deux : couleur finale = scène × calque × 2.
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_SRC_COLOR);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(result, left, bottom, viewW, viewH);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();
    }

    @Override
    public void dispose() {
        buffer.dispose();
        texture.dispose();
    }
}
