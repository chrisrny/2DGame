package game.model.world;

import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.event.EventBus;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Salle provisoire : une arène vide, sans mur ni ennemi.
 *
 * <p>Elle permet de faire tourner le joueur dans {@link Run} avant que {@code Room} (P1-B5)
 * existe. Elle sera supprimée quand {@code Run} utilisera les vraies salles.</p>
 */
final class EmptyRoom implements World {

    private final Rect arena;
    private final EventBus events;
    private final Supplier<Vec2> playerPosition;

    /**
     * Crée une salle vide.
     *
     * @param arena          les limites de l'arène
     * @param events         le bus d'événements de la partie
     * @param playerPosition donne la position actuelle du joueur
     */
    EmptyRoom(Rect arena, EventBus events, Supplier<Vec2> playerPosition) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.events = Objects.requireNonNull(events, "events");
        this.playerPosition = Objects.requireNonNull(playerPosition, "playerPosition");
    }

    @Override
    public Vec2 playerPosition() {
        return playerPosition.get();
    }

    @Override
    public Rect arena() {
        return arena;
    }

    @Override
    public List<Rect> walls() {
        return List.of();
    }

    @Override
    public List<Damageable> targetsIn(Circle zone, Faction attacker) {
        return List.of();
    }

    /**
     * Les projectiles ne sont pas gérés par la salle provisoire.
     *
     * @throws UnsupportedOperationException toujours : les projectiles arrivent avec {@code Room}
     */
    @Override
    public void spawnProjectile(Projectile projectile) {
        throw new UnsupportedOperationException("La salle provisoire ne gère pas les projectiles");
    }

    @Override
    public EventBus events() {
        return events;
    }
}
