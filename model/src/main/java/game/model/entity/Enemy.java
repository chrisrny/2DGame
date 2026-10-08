package game.model.entity;

import game.model.ai.EnemyBehavior;
import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.world.World;
import java.util.Objects;

/**
 * Base commune de tous les ennemis et du boss.
 *
 * <p>Un ennemi délègue ses décisions à un {@link EnemyBehavior} (pattern Strategy) :
 * poursuivre, garder ses distances, charger. Ses déplacements passent par
 * {@link Collisions#move} : il glisse le long des murs, mais traverse les autres ennemis.</p>
 */
public abstract class Enemy implements Damageable {

    /** Durée d'amortissement du recul, en secondes : la même que pour le joueur. */
    public static final float KNOCKBACK_DURATION = 0.15f;

    private Vec2 position;
    private Vec2 knockbackVelocity = Vec2.ZERO;
    private float knockbackTimer;
    private int hp;
    private final int maxHp;
    private final float radius;
    private final float speed;
    private final int contactDamage;
    private final EnemyBehavior behavior;

    /**
     * Crée un ennemi.
     *
     * @param position      position de départ
     * @param maxHp         points de vie maximum
     * @param radius        rayon de la zone de collision
     * @param speed         vitesse de déplacement, en unités par seconde
     * @param contactDamage dégâts infligés au joueur qui le touche
     * @param behavior      comportement propre à cet ennemi (une instance par ennemi)
     */
    protected Enemy(Vec2 position, int maxHp, float radius, float speed, int contactDamage, EnemyBehavior behavior) {
        this.position = Objects.requireNonNull(position, "position");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.radius = radius;
        this.speed = speed;
        this.contactDamage = contactDamage;
    }

    /**
     * Fait avancer l'ennemi d'un pas de jeu : s'il est repoussé, il subit son recul sans agir ;
     * sinon, son comportement décide de ce qu'il fait.
     *
     * @param world le monde où il évolue
     * @param dt    durée du pas, en secondes
     */
    public void update(World world, float dt) {
        if (knockbackTimer > 0f) {
            applyKnockback(world, dt);
            return;
        }
        behavior.act(this, world, dt);
    }

    /**
     * Avance vers une cible à sa vitesse, en glissant le long des murs, sans la dépasser.
     *
     * @param target la position visée
     * @param world  le monde, pour ses murs
     * @param dt     durée du pas, en secondes
     */
    public void moveToward(Vec2 target, World world, float dt) {
        float step = Math.min(speed * dt, position.distanceTo(target));
        Vec2 delta = target.sub(position).normalized().scale(step);
        setPosition(Collisions.move(hitbox(), delta, world.walls()));
    }

    /**
     * Applique un coup : ignore le tir ami et les coups reçus une fois mort, puis retire les
     * points de vie. Le recul du coup est une vitesse, en unités par seconde, amortie
     * jusqu'à zéro en {@link #KNOCKBACK_DURATION}.
     *
     * @param hit le coup reçu
     * @return {@code true} si le coup a été appliqué
     */
    @Override
    public boolean receive(Hit hit) {
        if (!isAlive() || hit.source() == faction()) {
            return false;
        }
        setHp(hp - hit.damage());
        if (hit.knockback().length() > 0f) {
            knockbackVelocity = hit.knockback();
            knockbackTimer = KNOCKBACK_DURATION;
        }
        return true;
    }

    private void applyKnockback(World world, float dt) {
        Vec2 delta = knockbackVelocity.scale(knockbackTimer / KNOCKBACK_DURATION * dt);
        setPosition(Collisions.move(hitbox(), delta, world.walls()));
        knockbackTimer -= dt;
    }

    /** @return la position actuelle */
    public Vec2 position() {
        return position;
    }

    /** @return les points de vie actuels */
    public int hp() {
        return hp;
    }

    /** @return les points de vie maximum */
    public int maxHp() {
        return maxHp;
    }

    /** @return la vitesse de déplacement, en unités par seconde */
    public float speed() {
        return speed;
    }

    /** @return les dégâts infligés au joueur qui le touche */
    public int contactDamage() {
        return contactDamage;
    }

    @Override
    public Circle hitbox() {
        return new Circle(position, radius);
    }

    @Override
    public Faction faction() {
        return Faction.ENEMY;
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    /**
     * Change la position de l'ennemi.
     *
     * @param newPosition la nouvelle position
     */
    protected void setPosition(Vec2 newPosition) {
        this.position = Objects.requireNonNull(newPosition, "newPosition");
    }

    /**
     * Change les points de vie, bornés entre 0 et le maximum.
     *
     * @param newHp les nouveaux points de vie
     */
    protected void setHp(int newHp) {
        this.hp = Math.max(0, Math.min(maxHp, newHp));
    }
}
