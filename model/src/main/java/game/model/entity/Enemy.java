package game.model.entity;

import game.model.ai.EnemyBehavior;
import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.world.World;
import java.util.Objects;

/** Base commune de tous les ennemis et du boss. */
public abstract class Enemy implements Damageable {

    /** Durée d'amortissement du recul, en secondes : la même que pour le joueur. */
    public static final float KNOCKBACK_DURATION = 0.15f;

    private Vec2 position;
    private Vec2 knockbackVelocity = Vec2.ZERO;
    private float knockbackTimer;
    private float stunLeft;
    private int hp;
    private final int maxHp;
    private final float radius;
    private final float speed;
    private final int contactDamage;
    private final float multiplier;
    private final EnemyBehavior behavior;

    /**
     * Crée un ennemi aux valeurs de référence (multiplicateur 1).
     *
     * @param position position de départ
     * @param maxHp points de vie maximum
     * @param radius rayon de la zone de collision
     * @param speed vitesse de déplacement, en unités par seconde
     * @param contactDamage dégâts infligés au joueur qui le touche
     * @param behavior comportement propre à cet ennemi (une instance par ennemi)
     */
    protected Enemy(Vec2 position, int maxHp, float radius, float speed, int contactDamage, EnemyBehavior behavior) {
        this(position, maxHp, radius, speed, contactDamage, behavior, 1f);
    }

    /**
     * Crée un ennemi renforcé (ou affaibli) par un multiplicateur, celui du stage et de la
     * difficulté.
     *
     * @param position position de départ
     * @param maxHp points de vie maximum de référence
     * @param radius rayon de la zone de collision
     * @param speed vitesse de déplacement, en unités par seconde
     * @param contactDamage dégâts de contact de référence
     * @param behavior comportement propre à cet ennemi (une instance par ennemi)
     * @param multiplier multiplicateur des points de vie et des dégâts, strictement positif
     * @throws IllegalArgumentException si le multiplicateur n'est pas strictement positif
     */
    protected Enemy(Vec2 position, int maxHp, float radius, float speed, int contactDamage, EnemyBehavior behavior,
            float multiplier) {
        if (!(multiplier > 0f)) {
            throw new IllegalArgumentException("Le multiplicateur doit être strictement positif");
        }
        this.position = Objects.requireNonNull(position, "position");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.multiplier = multiplier;
        this.maxHp = Math.max(1, Math.round(maxHp * multiplier));
        this.hp = this.maxHp;
        this.radius = radius;
        this.speed = speed;
        this.contactDamage = Math.round(contactDamage * multiplier);
    }

    /**
     * Fait avancer l'ennemi d'un pas de jeu : s'il est repoussé, il subit son recul sans agir ;
     * s'il est étourdi, il reste sur place ; sinon, son comportement décide de ce qu'il fait.
     *
     * @param world le monde où il évolue
     * @param dt durée du pas, en secondes
     */
    public void update(World world, float dt) {
        boolean stunned = stunLeft > 0f;
        stunLeft = Math.max(0f, stunLeft - dt);
        if (knockbackTimer > 0f) {
            applyKnockback(world, dt);
            return;
        }
        if (!stunned) {
            behavior.act(this, world, dt);
        }
    }

    /**
     * Avance vers une cible à sa vitesse, en glissant le long des murs, sans la dépasser.
     *
     * @param target la position visée
     * @param world le monde, pour ses murs
     * @param dt durée du pas, en secondes
     */
    public void moveToward(Vec2 target, World world, float dt) {
        float step = Math.min(speed * dt, position.distanceTo(target));
        Vec2 delta = target.sub(position).normalized().scale(step);
        setPosition(Collisions.move(hitbox(), delta, world.walls()));
    }

    /**
     * Poursuit le joueur : va droit sur lui si rien ne gêne, sinon contourne les murs en suivant le
     * chemin donné par le monde ({@link World#pathTowardPlayer}).
     *
     * @param world le monde où il évolue
     * @param dt durée du pas, en secondes
     */
    public void chase(World world, float dt) {
        moveToward(world.pathTowardPlayer(position, radius), world, dt);
    }

    /**
     * Se déplace d'un vecteur donné, en glissant le long des murs, sans tenir compte de sa vitesse.
     * Sert aux déplacements spéciaux, comme la charge du Chargeur.
     *
     * @param delta le déplacement voulu pendant ce pas
     * @param world le monde, pour ses murs
     * @return le déplacement réellement effectué : plus court que {@code delta} si un mur a bloqué
     */
    public Vec2 moveBy(Vec2 delta, World world) {
        Vec2 start = position;
        setPosition(Collisions.move(hitbox(), delta, world.walls()));
        return position.sub(start);
    }

    /**
     * Applique un coup : ignore le tir ami et les coups reçus une fois mort, puis retire les points
     * de vie.
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
        stunLeft = Math.max(stunLeft, hit.stun());
        return true;
    }

    /** Un ennemi étourdi ne bouge plus, n'attaque plus et ne blesse pas au contact. */
    public boolean isStunned() {
        return stunLeft > 0f;
    }

    /** @return le temps d'étourdissement restant, en secondes ({@code 0} s'il n'est pas étourdi) */
    public float stunLeft() {
        return stunLeft;
    }

    private void applyKnockback(World world, float dt) {
        Vec2 delta = knockbackVelocity.scale(knockbackTimer / KNOCKBACK_DURATION * dt);
        setPosition(Collisions.move(hitbox(), delta, world.walls()));
        knockbackTimer -= dt;
    }

    /**
     * Résistance de l'ennemi quand deux ennemis qui se chevauchent sont écartés : celui qui résiste
     * le plus recule le moins.
     */
    public float pushResistance() {
        return 1f;
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

    /** @return le multiplicateur des points de vie et des dégâts (1 aux valeurs de référence) */
    public float multiplier() {
        return multiplier;
    }

    /**
     * Applique le multiplicateur de l'ennemi à des dégâts de référence, par exemple ceux d'un
     * projectile tiré par un Archer.
     *
     * @param baseDamage les dégâts de référence
     * @return les dégâts réels, arrondis à l'entier le plus proche
     */
    public int scaleDamage(int baseDamage) {
        return Math.round(baseDamage * multiplier);
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
