package game.model.world;

import game.model.combat.Collisions;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Enemy;
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
    private final List<Rect> water;
    private final EventBus events = new EventBus();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Enemy> spawned = new ArrayList<>();
    private final List<Damageable> targets = new ArrayList<>();
    private Vec2 playerPosition = Vec2.ZERO;

    /**
     * Crée un faux monde.
     *
     * @param arena les limites de l'arène
     * @param walls les murs
     */
    public FakeWorld(Rect arena, List<Rect> walls) {
        this(arena, walls, List.of());
    }

    /**
     * Crée un faux monde avec de l'eau : des murs que les projectiles survolent.
     *
     * @param arena les limites de l'arène
     * @param walls les murs, eau comprise
     * @param water ceux des murs qui sont de l'eau
     */
    public FakeWorld(Rect arena, List<Rect> walls, List<Rect> water) {
        this.arena = arena;
        this.walls = List.copyOf(walls);
        this.water = List.copyOf(water);
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
    public List<Rect> solidWalls() {
        return walls.stream().filter(wall -> !water.contains(wall)).toList();
    }

    @Override
    public List<Damageable> targetsIn(Circle zone, Faction attacker) {
        return targets.stream()
                .filter(Damageable::isAlive)
                .filter(target -> target.faction() != attacker)
                .filter(target -> Collisions.overlaps(zone, target.hitbox()))
                .toList();
    }

    /** @return les ennemis apparus en cours de partie (invocations du boss), dans l'ordre */
    public List<Enemy> spawned() {
        return spawned;
    }

    @Override
    public void spawnEnemy(Enemy enemy) {
        spawned.add(enemy);
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
