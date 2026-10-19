package game.model.combat;

import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.event.GameEvent;
import game.model.world.World;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Un projectile en vol : flèche d'Archer, lame de l'Éventail du joueur, projectile du boss.
 *
 * <p>À chaque pas, il avance en ligne droite et disparaît dans trois cas : sa durée de vie
 * est écoulée, il entre dans un mur (l'eau ne l'arrête pas : il passe au-dessus), ou il touche une cible de l'autre faction (qui reçoit
 * un {@link Hit}). Une cible qui refuse le coup, par exemple le joueur pendant son esquive,
 * est traversée : c'est ce qui permet d'esquiver les tirs.</p>
 *
 * <p>Un projectile perçant ({@code pierce > 1}) traverse ses premières cibles : il en touche
 * jusqu'à {@code pierce}, chacune une seule fois, puis disparaît.</p>
 */
public class Projectile {

    /** Vitesse du recul infligé à la cible touchée, en unités par seconde. */
    public static final float KNOCKBACK = 60f;

    private Vec2 position;
    private final Vec2 velocity;
    private final float radius;
    private final Faction faction;
    private final int damage;
    private final int pierce;
    private final Runnable onHit;
    private final Set<Damageable> alreadyHit = Collections.newSetFromMap(new IdentityHashMap<>());
    private float lifetime;
    private boolean alive = true;

    /**
     * Crée un projectile.
     *
     * @param position position de départ
     * @param velocity vitesse, en unités par seconde
     * @param radius   rayon de la zone de collision
     * @param faction  faction de celui qui tire
     * @param damage   dégâts infligés à la cible touchée
     * @param lifetime durée de vie maximale, en secondes
     */
    public Projectile(Vec2 position, Vec2 velocity, float radius, Faction faction, int damage, float lifetime) {
        this(position, velocity, radius, faction, damage, lifetime, 1);
    }

    /**
     * Crée un projectile perçant, sans personne à prévenir de ses coups.
     *
     * @param position position de départ
     * @param velocity vitesse, en unités par seconde
     * @param radius   rayon de la zone de collision
     * @param faction  faction de celui qui tire
     * @param damage   dégâts infligés à chaque cible touchée
     * @param lifetime durée de vie maximale, en secondes
     * @param pierce   nombre de cibles différentes qu'il peut toucher avant de disparaître, au moins 1
     */
    public Projectile(Vec2 position, Vec2 velocity, float radius, Faction faction, int damage, float lifetime,
            int pierce) {
        this(position, velocity, radius, faction, damage, lifetime, pierce, () -> { });
    }

    /**
     * Crée un projectile perçant dont le tireur est prévenu à chaque coup.
     *
     * @param position position de départ
     * @param velocity vitesse, en unités par seconde
     * @param radius   rayon de la zone de collision
     * @param faction  faction de celui qui tire
     * @param damage   dégâts infligés à chaque cible touchée
     * @param lifetime durée de vie maximale, en secondes
     * @param pierce   nombre de cibles différentes qu'il peut toucher avant de disparaître, au moins 1
     * @param onHit    appelé à chaque cible touchée : le tireur en est prévenu (reliques du joueur)
     */
    public Projectile(Vec2 position, Vec2 velocity, float radius, Faction faction, int damage, float lifetime,
            int pierce, Runnable onHit) {
        if (pierce < 1) {
            throw new IllegalArgumentException("Un projectile touche au moins une cible");
        }
        this.pierce = pierce;
        this.onHit = Objects.requireNonNull(onHit, "onHit");
        this.position = Objects.requireNonNull(position, "position");
        this.velocity = Objects.requireNonNull(velocity, "velocity");
        this.faction = Objects.requireNonNull(faction, "faction");
        this.radius = radius;
        this.damage = damage;
        this.lifetime = lifetime;
    }

    /**
     * Fait avancer le projectile d'un pas de jeu, puis vérifie s'il doit disparaître.
     * Un projectile déjà disparu ne fait plus rien.
     *
     * @param world le monde, pour ses murs, ses cibles et son bus d'événements
     * @param dt    durée du pas, en secondes
     */
    public void update(World world, float dt) {
        if (!alive) {
            return;
        }
        setPosition(position.add(velocity.scale(dt)));
        setLifetime(lifetime - dt);
        if (lifetime <= 0f || hitsWall(world)) {
            alive = false;
            return;
        }
        hitTargets(world);
    }

    // Les murs sont épais (16 u au moins) et un projectile avance de quelques unités par pas :
    // il ne peut pas traverser un mur entre deux pas.
    private boolean hitsWall(World world) {
        Circle box = hitbox();
        return world.solidWalls().stream().anyMatch(wall -> Collisions.overlaps(box, wall));
    }

    // Chaque cible n'est touchée qu'une fois ; le projectile disparaît à sa dernière cible.
    private void hitTargets(World world) {
        List<Damageable> targets = world.targetsIn(hitbox(), faction);
        Vec2 push = velocity.normalized().scale(KNOCKBACK);
        for (Damageable target : targets) {
            if (alreadyHit.contains(target)) {
                continue;
            }
            Hit hit = new Hit(damage, faction, false, push);
            if (target.receive(hit)) {
                world.events().publish(new GameEvent.EntityHit(
                        target.hitbox().center(), damage, false, target.faction()));
                alreadyHit.add(target);
                onHit.run();
                if (alreadyHit.size() >= pierce) {
                    alive = false;
                    return;
                }
            }
        }
    }

    /** @return {@code true} tant que le projectile est en vol */
    public boolean isAlive() {
        return alive;
    }

    /** @return la position actuelle */
    public Vec2 position() {
        return position;
    }

    /** @return la vitesse, en unités par seconde */
    public Vec2 velocity() {
        return velocity;
    }

    /** @return la zone de collision du projectile */
    public Circle hitbox() {
        return new Circle(position, radius);
    }

    /** @return la faction de celui qui a tiré */
    public Faction faction() {
        return faction;
    }

    /** @return les dégâts infligés à la cible touchée */
    public int damage() {
        return damage;
    }

    /** @return le nombre de cibles différentes que le projectile peut toucher */
    public int pierce() {
        return pierce;
    }

    /** @return le nombre de cibles déjà touchées */
    public int hits() {
        return alreadyHit.size();
    }

    /** @return le temps de vie restant, en secondes */
    public float lifetime() {
        return lifetime;
    }

    /**
     * Change la position du projectile (utilisé par son déplacement).
     *
     * @param newPosition la nouvelle position
     */
    protected void setPosition(Vec2 newPosition) {
        this.position = Objects.requireNonNull(newPosition, "newPosition");
    }

    /**
     * Change le temps de vie restant (utilisé par son déplacement).
     *
     * @param newLifetime le nouveau temps de vie, en secondes
     */
    protected void setLifetime(float newLifetime) {
        this.lifetime = newLifetime;
    }
}
