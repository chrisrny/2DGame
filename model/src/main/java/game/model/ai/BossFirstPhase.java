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
 *       {@link #CHARGE_SPEED}, plus vite que lui : approche, annonce, charge, étourdissement.
 *       Contrairement au Chargeur, il anticipe le déplacement du joueur
 *       ({@link #CHARGE_LEAD}) : s'écarter en ligne droite ne suffit pas.</li>
 * </ul>
 * <p>Entre deux attaques, le boss marche vers le joueur pendant {@link #PAUSE_DURATION} seconde.</p>
 *
 * <p>Le cycle des attaques est fixé ici ; ses réglages passent par des méthodes que la phase 2
 * redéfinit ({@link BossSecondPhase}, pattern Template Method) : durée de la pause, taille et
 * annonce de la frappe, vitesse, annonce et anticipation de la charge, et ce qui suit une
 * frappe.</p>
 */
public class BossFirstPhase implements BossPhase {

    /** Rayon de la frappe au sol. */
    public static final float SLAM_RADIUS = 60f;

    /** Durée de l'annonce de la frappe au sol, en secondes. */
    public static final float SLAM_WIND_UP = 0.8f;

    /** Dégâts de la frappe au sol, avant le multiplicateur du boss. */
    public static final int SLAM_DAMAGE = 20;

    /** Vitesse de la charge du boss, en unités par seconde : plus rapide que celle du Chargeur. */
    public static final float CHARGE_SPEED = 400f;

    /** Durée de l'annonce de la charge, en secondes : la même que celle du Chargeur. */
    public static final float CHARGE_WIND_UP = ChargerBehavior.WIND_UP_DURATION;

    /**
     * Part de l'anticipation de la charge : il vise à 70 % du chemin entre le joueur et là où il
     * sera. Il laisse une marge à celui qui s'écarte vite.
     */
    public static final float CHARGE_LEAD = 0.7f;

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
                if (elapsed(pauseDuration())) {
                    startNextAttack(world);
                }
            }
            case SLAM -> {
                if (elapsed(slamWindUp())) {
                    slam(boss, world);
                    afterSlam(boss, world, slamZone);
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
            slamZone = new Circle(world.playerPosition(), slamRadius());
            enter(Step.SLAM);
        } else {
            charge = new ChargerBehavior(chargeSpeed(), chargeWindUp(), chargeLead());
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

    /** @return la pause entre deux attaques, en secondes */
    protected float pauseDuration() {
        return PAUSE_DURATION;
    }

    /** @return le rayon de la frappe au sol */
    protected float slamRadius() {
        return SLAM_RADIUS;
    }

    /** @return la durée de l'annonce de la frappe au sol, en secondes */
    protected float slamWindUp() {
        return SLAM_WIND_UP;
    }

    /** @return la vitesse de la charge, en unités par seconde */
    protected float chargeSpeed() {
        return CHARGE_SPEED;
    }

    /** @return la durée de l'annonce de la charge, en secondes */
    protected float chargeWindUp() {
        return CHARGE_WIND_UP;
    }

    /** @return la part de l'anticipation de la charge, de 0 à 1 */
    protected float chargeLead() {
        return CHARGE_LEAD;
    }

    /**
     * Ce qui suit une frappe au sol. En phase 1, rien.
     *
     * @param boss  le boss
     * @param world le monde où il se bat
     * @param zone  la zone qui vient d'être frappée
     */
    protected void afterSlam(Boss boss, World world, Circle zone) {
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
            return Optional.of(new Telegraph(slamZone, Math.min(1f, stepTime / slamWindUp())));
        }
        if (step == Step.CHARGE && charge.isWindingUp()) {
            // L'annonce de la charge est le boss lui-même, qui se remplit de rouge.
            float progress = Math.min(1f, charge.phaseTime() / charge.windUpDuration());
            return Optional.of(new Telegraph(boss.hitbox(), progress));
        }
        return Optional.empty();
    }

    @Override
    public boolean isCharging() {
        return step == Step.CHARGE && charge.isCharging();
    }
}
