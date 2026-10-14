package game.model.ai;

import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.world.World;

/**
 * Comportement de l'Archer : garder ses distances et tirer.
 *
 * <p>Il reste entre {@link #MIN_DISTANCE} et {@link #MAX_DISTANCE} du joueur : il recule
 * s'il est trop près, avance s'il est trop loin, et reste sur place entre les deux.
 * Quand le joueur est à portée, il tire un projectile vers lui toutes les
 * {@link #FIRE_INTERVAL} secondes.</p>
 */
public final class ArcherBehavior implements EnemyBehavior {

    /** En dessous de cette distance au joueur, l'Archer recule. */
    public static final float MIN_DISTANCE = 120f;

    /** Au-delà de cette distance au joueur, l'Archer avance. */
    public static final float MAX_DISTANCE = 180f;

    /** Distance maximale à laquelle l'Archer tire, pour qu'il ne tire pas depuis l'autre bout de la map. */
    public static final float FIRE_RANGE = 260f;

    /** Délai entre deux tirs, en secondes. */
    public static final float FIRE_INTERVAL = 1.5f;

    /** Vitesse des projectiles, en unités par seconde. */
    public static final float PROJECTILE_SPEED = 200f;

    /** Dégâts d'un projectile. */
    public static final int PROJECTILE_DAMAGE = 8;

    /** Rayon d'un projectile. */
    public static final float PROJECTILE_RADIUS = 3f;

    /** Durée de vie d'un projectile, en secondes : il parcourt au plus 600 u. */
    public static final float PROJECTILE_LIFETIME = 3f;

    // Le premier tir part après un délai complet : le joueur a le temps de voir l'Archer.
    private float fireCooldown = FIRE_INTERVAL;

    @Override
    public void act(Enemy self, World world, float dt) {
        Vec2 player = world.playerPosition();
        float distance = self.position().distanceTo(player);
        if (distance < MIN_DISTANCE) {
            Vec2 away = self.position().sub(player).normalized();
            self.moveToward(self.position().add(away.scale(MIN_DISTANCE)), world, dt);
        } else if (distance > MAX_DISTANCE) {
            self.moveToward(player, world, dt);
        }
        fireCooldown = Math.max(0f, fireCooldown - dt);
        if (fireCooldown == 0f && distance <= FIRE_RANGE) {
            shoot(self, player, world);
            fireCooldown = FIRE_INTERVAL;
        }
    }

    private static void shoot(Enemy self, Vec2 target, World world) {
        Vec2 velocity = target.sub(self.position()).normalized().scale(PROJECTILE_SPEED);
        world.spawnProjectile(new Projectile(self.position(), velocity, PROJECTILE_RADIUS,
                Faction.ENEMY, PROJECTILE_DAMAGE, PROJECTILE_LIFETIME));
    }
}
