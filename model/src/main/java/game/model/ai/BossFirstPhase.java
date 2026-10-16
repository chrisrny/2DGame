package game.model.ai;

import game.model.combat.Hit;
import game.model.combat.Telegraph;
import game.model.commun.Circle;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import game.model.entity.Damageable;
import game.model.event.GameEvent;
import game.model.world.World;
import java.util.Optional;

/**
 * Phase 1 du boss : il alterne une frappe au sol et une charge, avec une pause entre les deux.
 *
 * <p>Chaque attaque est annoncée avant de frapper :</p>
 * <ul>
 *   <li><b>frappe au sol</b> : un cercle de rayon {@link #SLAM_RADIUS} apparaît là où se trouve
 *       le joueur et reste {@link #SLAM_WIND_UP} secondes, sans bouger ; puis tout ce qui est
 *       dedans prend {@link #SLAM_DAMAGE} dégâts. Le joueur qui sort du cercle à temps n'a rien ;</li>
 *   <li><b>charge</b> : la logique du Chargeur ({@link ChargerBehavior}) à
 *       {@link #CHARGE_SPEED}, plus vite que lui : approche, annonce, charge, étourdissement.</li>
 * </ul>
 * <p>Entre deux attaques, le boss marche vers le joueur pendant {@link #PAUSE_DURATION} seconde.</p>
 */
public final class BossFirstPhase implements BossPhase {

    /** Rayon de la frappe au sol. */
    public static final float SLAM_RADIUS = 60f;

    /** Durée de l'annonce de la frappe au sol, en secondes. */
    public static final float SLAM_WIND_UP = 0.8f;

    /** Dégâts de la frappe au sol, avant le multiplicateur du boss. */
    public static final int SLAM_DAMAGE = 20;

    /** Vitesse de la charge du boss, en unités par seconde : plus rapide que celle du Chargeur. */
    public static final float CHARGE_SPEED = 400f;

    /** Pause entre deux attaques, en secondes : le moment de souffler et de contre-attaquer. */
    public static final float PAUSE_DURATION = 1f;

    // Même tolérance que le Chargeur : 48 pas de 1/60 s font 0,8 s à un arrondi de float près.
    private static final float EPSILON = 1e-4f;

    private enum Step { PAUSE, SLAM, CHARGE }

    private Step step = Step.PAUSE;
    private float stepTime;
    private boolean slamNext = true;
    private Circle slamZone;
    private ChargerBehavior charge;

    @Override
    public void update(Boss boss, World world, float dt) {
        stepTime += dt;
        switch (step) {
            case PAUSE -> {
                boss.chase(world, dt);
                if (elapsed(PAUSE_DURATION)) {
                    startNextAttack(world);
                }
            }
            case SLAM -> {
                if (elapsed(SLAM_WIND_UP)) {
                    slam(boss, world);
                    enter(Step.PAUSE);
                }
            }
            case CHARGE -> {
                // La charge est finie quand le boss sort de son étourdissement.
                boolean wasStunned = charge.isStunned();
                charge.act(boss, world, dt);
                if (wasStunned && !charge.isStunned()) {
                    enter(Step.PAUSE);
                }
            }
        }
    }

    private void startNextAttack(World world) {
        if (slamNext) {
            // La zone est figée sur la position du joueur maintenant : il a le temps d'en sortir.
            slamZone = new Circle(world.playerPosition(), SLAM_RADIUS);
            enter(Step.SLAM);
        } else {
            charge = new ChargerBehavior(CHARGE_SPEED);
            enter(Step.CHARGE);
        }
        slamNext = !slamNext;
    }

    private void slam(Boss boss, World world) {
        int damage = boss.scaleDamage(SLAM_DAMAGE);
        for (Damageable target : world.targetsIn(slamZone, boss.faction())) {
            Hit hit = new Hit(damage, boss.faction(), false, Vec2.ZERO);
            if (target.receive(hit)) {
                world.events().publish(new GameEvent.EntityHit(target.hitbox().center(), damage, false,
                        target.faction()));
            }
        }
    }

    private boolean elapsed(float duration) {
        return stepTime >= duration - EPSILON;
    }

    private void enter(Step next) {
        step = next;
        stepTime = 0f;
    }

    @Override
    public Optional<Telegraph> telegraph(Boss boss) {
        if (step == Step.SLAM) {
            return Optional.of(new Telegraph(slamZone, Math.min(1f, stepTime / SLAM_WIND_UP)));
        }
        if (step == Step.CHARGE && charge.isWindingUp()) {
            // L'annonce de la charge est le boss lui-même, qui se remplit de rouge.
            float progress = Math.min(1f, charge.phaseTime() / ChargerBehavior.WIND_UP_DURATION);
            return Optional.of(new Telegraph(boss.hitbox(), progress));
        }
        return Optional.empty();
    }

    @Override
    public boolean isCharging() {
        return step == Step.CHARGE && charge.isCharging();
    }
}
