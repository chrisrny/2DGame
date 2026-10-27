package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Nuages de fumée et de braises à la mort d'un ennemi. */
public final class DeathPuffs implements Disposable {

    /** Durée d'une frame de fumée, en secondes. */
    private static final float FRAME = 0.08f;
    private static final int FRAMES = 6;
    private static final int CELL = 32;

    /** Un nuage en cours : où il est et depuis combien de temps. */
    public static final class Puff {
        private final Vec2 position;
        private float time;

        private Puff(Vec2 position) {
            this.position = position;
        }

        /** @return la position de l'ennemi mort */
        public Vec2 position() {
            return position;
        }
    }

    private final Texture texture;
    private final Animation<TextureRegion> animation;
    private final List<Puff> puffs = new ArrayList<>();

    /**
     * Charge la fumée et s'abonne au bus de la partie.
     *
     * @param events le bus d'événements du modèle
     */
    public DeathPuffs(EventBus events) {
        texture = new Texture(Gdx.files.internal("sprites/smoke.png"));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        animation = new Animation<>(FRAME, TextureRegion.split(texture, CELL, CELL)[0]);
        Objects.requireNonNull(events, "events").subscribe(this::onEvent);
    }

    private void onEvent(GameEvent event) {
        if (event instanceof GameEvent.EntityDied died && died.faction() == Faction.ENEMY) {
            puffs.add(new Puff(died.position()));
        }
    }

    /**
     * Fait avancer les nuages et retire ceux qui sont finis.
     *
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(float delta) {
        for (Puff puff : puffs) {
            puff.time += delta;
        }
        puffs.removeIf(puff -> puff.time >= FRAME * FRAMES);
    }

    /** @return les nuages en cours, pour le tri par profondeur */
    public List<Puff> puffs() {
        return puffs;
    }

    /**
     * Dessine un nuage. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param puff le nuage
     */
    public void draw(SpriteBatch batch, Puff puff) {
        batch.draw(animation.getKeyFrame(puff.time), puff.position.x() - CELL / 2f, puff.position.y() - 10f);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
