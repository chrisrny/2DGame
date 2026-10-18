package game.model.ai;

import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import game.model.world.World;

/**
 * Phase 2 du boss, à partir de la moitié de ses points de vie : le même cycle que la phase 1
 * ({@link BossFirstPhase}), en plus dangereux.
 *
 * <ul>
 *   <li>pauses plus courtes ({@link #PAUSE_DURATION}) : moins de temps pour souffler ;</li>
 *   <li>frappe plus large ({@link #SLAM_RADIUS}) et annoncée un peu moins longtemps
 *       ({@link #SLAM_WIND_UP}) ;</li>
 *   <li>charge plus rapide ({@link #CHARGE_SPEED}) ;</li>
 *   <li>chaque frappe projette un anneau de {@link #EMBER_COUNT} braises depuis le point
 *       d'impact : il faut sortir du cercle, puis passer entre les braises.</li>
 * </ul>
 *
 * <p>Seuls les réglages changent : le cycle lui-même reste celui de la phase 1 (pattern
 * Template Method).</p>
 */
public final class BossSecondPhase extends BossFirstPhase {

    /** Pause entre deux attaques, en secondes. */
    public static final float PAUSE_DURATION = 0.6f;

    /** Rayon de la frappe au sol. */
    public static final float SLAM_RADIUS = 72f;

    /** Durée de l'annonce de la frappe au sol, en secondes. */
    public static final float SLAM_WIND_UP = 0.7f;

    /** Vitesse de la charge, en unités par seconde. */
    public static final float CHARGE_SPEED = 470f;

    /** Nombre de braises projetées par chaque frappe. */
    public static final int EMBER_COUNT = 8;

    /** Vitesse des braises, en unités par seconde : assez lentes pour passer entre elles. */
    public static final float EMBER_SPEED = 140f;

    /** Dégâts d'une braise, avant le multiplicateur du boss. */
    public static final int EMBER_DAMAGE = 6;

    /** Rayon d'une braise. */
    public static final float EMBER_RADIUS = 4f;

    /** Durée de vie d'une braise, en secondes. */
    public static final float EMBER_LIFETIME = 1.6f;

    @Override
    protected float pauseDuration() {
        return PAUSE_DURATION;
    }

    @Override
    protected float slamRadius() {
        return SLAM_RADIUS;
    }

    @Override
    protected float slamWindUp() {
        return SLAM_WIND_UP;
    }

    @Override
    protected float chargeSpeed() {
        return CHARGE_SPEED;
    }

    @Override
    protected void afterSlam(Boss boss, World world, Circle zone) {
        int damage = boss.scaleDamage(EMBER_DAMAGE);
        for (int i = 0; i < EMBER_COUNT; i++) {
            double angle = 2 * Math.PI * i / EMBER_COUNT;
            Vec2 velocity = new Vec2((float) Math.cos(angle), (float) Math.sin(angle)).scale(EMBER_SPEED);
            world.spawnProjectile(new Projectile(zone.center(), velocity, EMBER_RADIUS, Faction.ENEMY, damage,
                    EMBER_LIFETIME));
        }
    }
}
