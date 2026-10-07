package game.model.combat;

import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import java.util.Objects;

/**
 * Un projectile en vol : flèche d'Archer, Lame lancée du joueur, projectile du boss.
 *
 * <p>Ce contrat fixe ce qu'est un projectile et comment on le crée. Son déplacement,
 * sa disparition contre les murs et l'application des dégâts sont ajoutés en P1-B2.</p>
 */
public class Projectile {

    private Vec2 position;
    private final Vec2 velocity;
    private final float radius;
    private final Faction faction;
    private final int damage;
    private float lifetime;

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
        this.position = Objects.requireNonNull(position, "position");
        this.velocity = Objects.requireNonNull(velocity, "velocity");
        this.faction = Objects.requireNonNull(faction, "faction");
        this.radius = radius;
        this.damage = damage;
        this.lifetime = lifetime;
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
