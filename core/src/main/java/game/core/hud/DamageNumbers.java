package game.core.hud;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import game.model.commun.Faction;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.Objects;

/**
 * Chiffres de dégâts qui s'élèvent au-dessus des cibles touchées puis s'effacent.
 *
 * <p>Deux patterns :</p>
 * <ul>
 *   <li><b>Observer</b> : les chiffres s'abonnent au bus du modèle et réagissent à chaque
 *   {@link GameEvent.EntityHit}. Le modèle publie le coup sans savoir que la vue l'affiche.</li>
 *   <li><b>Object Pool</b> : les {@link FloatingNumber} sont pris dans une réserve et y retournent
 *   quand ils disparaissent, au lieu d'être créés à chaque coup puis jetés. Pendant un combat,
 *   ça évite de donner du travail au ramasse-miettes, qui peut causer des micro-saccades.</li>
 * </ul>
 *
 * <p>Les chiffres vivent dans le monde (ils suivent la caméra du jeu) et avancent avec le temps
 * réel de l'image : c'est purement visuel, le modèle ne les connaît pas.</p>
 */
public final class DamageNumbers implements Disposable {

    /** Durée d'affichage d'un chiffre, en secondes. */
    public static final float LIFETIME = 0.8f;

    /** Hauteur dont monte un chiffre pendant sa durée de vie, en unités. */
    public static final float RISE = 24f;

    /** Décalage vertical de départ au-dessus du centre de la cible, en unités. */
    private static final float START_OFFSET = 10f;

    private static final float NORMAL_SCALE = 0.6f;
    private static final float CRITICAL_SCALE = 0.85f;

    private final Pool<FloatingNumber> pool = new Pool<>(16) {
        @Override
        protected FloatingNumber newObject() {
            return new FloatingNumber();
        }
    };
    private final Array<FloatingNumber> active = new Array<>(false, 16);
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    /**
     * Crée les chiffres de dégâts et les abonne au bus de la partie.
     *
     * @param events le bus d'événements du modèle
     */
    public DamageNumbers(EventBus events) {
        Objects.requireNonNull(events, "events").subscribe(this::onEvent);
        font.setUseIntegerPositions(false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void onEvent(GameEvent event) {
        if (event instanceof GameEvent.EntityHit hit) {
            FloatingNumber number = pool.obtain();
            number.init(hit.position().x(), hit.position().y() + START_OFFSET,
                    hit.damage(), hit.critical(), hit.target() == Faction.PLAYER);
            active.add(number);
        }
    }

    /**
     * Fait vieillir les chiffres et rend à la réserve ceux qui ont fini de s'afficher.
     *
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(float delta) {
        for (int i = active.size - 1; i >= 0; i--) {
            FloatingNumber number = active.get(i);
            number.age += delta;
            if (number.age >= LIFETIME) {
                active.removeIndex(i);
                pool.free(number);
            }
        }
    }

    /**
     * Dessine les chiffres. À appeler avec la projection de la caméra du jeu, après le monde.
     *
     * @param worldProjection la projection de la caméra qui suit le joueur
     */
    public void draw(Matrix4 worldProjection) {
        if (active.isEmpty()) {
            return;
        }
        batch.setProjectionMatrix(worldProjection);
        batch.begin();
        for (FloatingNumber number : active) {
            float progress = Math.min(1f, number.age / LIFETIME);
            float alpha = 1f - progress;
            if (number.onPlayer) {
                font.setColor(1f, 0.30f, 0.30f, alpha);
            } else if (number.critical) {
                font.setColor(1f, 0.85f, 0.20f, alpha);
            } else {
                font.setColor(1f, 1f, 1f, alpha);
            }
            font.getData().setScale(number.critical ? CRITICAL_SCALE : NORMAL_SCALE);
            font.draw(batch, number.text, number.x, number.y + RISE * progress, 0f, Align.center, false);
        }
        batch.end();
        font.setColor(1f, 1f, 1f, 1f);
        font.getData().setScale(1f);
    }

    /** @return le nombre de chiffres affichés en ce moment */
    public int activeCount() {
        return active.size;
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
