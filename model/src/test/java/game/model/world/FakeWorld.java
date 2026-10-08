package game.model.world;

import game.model.combat.Collisions;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.event.EventBus;
import java.util.ArrayList;
import java.util.List;

/**
 * Faux monde pour les tests : une arène, des murs et des cibles choisis par le test.
 * Les projectiles tirés sont gardés dans une liste que le test peut vérifier.
 */
public final class FakeWorld implements World {

    private final Rect arena;
    private final List<Rect> walls;
    private final EventBus events = new EventBus();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Damageable> targets = new ArrayList<>();
    private Vec2 playerPosition = Vec2.ZERO;

    /**
     * Crée un faux monde.
     *
     * @param arena les limites de l'arène
     * @param walls les murs
     */
    public FakeWorld(Rect arena, List<Rect> walls) {
        this.arena = arena;
        this.walls = List.copyOf(walls);
    }

    /**
     * Crée une grande arène vide de 1000x1000.
     *
     * @return un faux monde sans mur
     */
    public static FakeWorld empty() {
        return new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of());
    }

    /**
     * Fixe la position du joueur renvoyée par {@link #playerPosition()}.
     *
     * @param position la position du joueur
     */
    public void setPlayerPosition(Vec2 position) {
        this.playerPosition = position;
    }

    /**
     * Ajoute une cible que les attaques peuvent toucher.
     *
     * @param target la cible
     */
    public void addTarget(Damageable target) {
        targets.add(target);
    }

    /** @return les projectiles tirés depuis la création du monde */
    public List<Projectile> projectiles() {
        return projectiles;
    }

    @Override
    public Vec2 playerPosition() {
        return playerPosition;
    }

    @Override
    public Rect arena() {
        return arena;
    }

    @Override
    public List<Rect> walls() {
        return walls;
    }

    @Override
    public List<Damageable> targetsIn(Circle zone, Faction attacker) {
        return targets.stream()
                .filter(Damageable::isAlive)
                .filter(target -> target.faction() != attacker)
                .filter(target -> Collisions.overlaps(zone, target.hitbox()))
                .toList();
    }

    @Override
    public void spawnProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    @Override
    public EventBus events() {
        return events;
    }
}
