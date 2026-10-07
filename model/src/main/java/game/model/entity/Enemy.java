package game.model.entity;

import game.model.ai.EnemyBehavior;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.world.World;
import java.util.Objects;

/**
 * Base commune de tous les ennemis et du boss.
 *
 * <p>Un ennemi délègue ses décisions à un {@link EnemyBehavior} (pattern Strategy) :
 * poursuivre, garder ses distances, charger. Ce contrat fixe la forme d'un ennemi ;
 * la réception des coups et les déplacements sont écrits en P1-B1.</p>
 */
public abstract class Enemy implements Damageable {

    private Vec2 position;
    private int hp;
    private final int maxHp;
    private final float radius;
    private final EnemyBehavior behavior;

    /**
     * Crée un ennemi.
     *
     * @param position position de départ
     * @param maxHp    points de vie maximum
     * @param radius   rayon de la zone de collision
     * @param behavior comportement propre à cet ennemi (une instance par ennemi)
     */
    protected Enemy(Vec2 position, int maxHp, float radius, EnemyBehavior behavior) {
        this.position = Objects.requireNonNull(position, "position");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.radius = radius;
    }

    /**
     * Fait avancer l'ennemi d'un pas de jeu : son comportement décide de ce qu'il fait.
     *
     * @param world le monde où il évolue
     * @param dt    durée du pas, en secondes
     */
    public void update(World world, float dt) {
        behavior.act(this, world, dt);
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
