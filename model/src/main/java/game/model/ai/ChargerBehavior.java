package game.model.ai;

import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.world.World;

/**
 * Comportement du Chargeur : il prévient, charge en ligne droite, puis reste vulnérable.
 *
 * <p>Il passe en boucle par quatre phases, chacune avec son minuteur :</p>
 * <ol>
 *   <li>{@link Phase#APPROACH} : il marche vers le joueur à la vitesse de l'ennemi (60 u/s), en
 *       contournant les murs, jusqu'à être à moins de {@link #TRIGGER_DISTANCE} avec la voie
 *       libre jusqu'au joueur (il ne charge jamais dans un mur qu'il voit) ;</li>
 *   <li>{@link Phase#WIND_UP} : il reste immobile {@link #WIND_UP_DURATION} secondes. La vue
 *       le fait clignoter : sans annonce visible, la charge serait injuste ;</li>
 *   <li>{@link Phase#CHARGE} : il fonce à sa vitesse de charge ({@link #CHARGE_SPEED} pour le
 *       Chargeur, plus pour le boss) dans la direction du joueur,
 *       figée au début de la charge, pendant {@link #CHARGE_MAX_DURATION} secondes au plus.
 *       Il s'arrête plus tôt s'il heurte un mur ;</li>
 *   <li>{@link Phase#STUNNED} : il reste étourdi {@link #STUN_DURATION} seconde, sans bouger
 *       ni blesser : c'est le moment de le frapper.</li>
 * </ol>
 */
public final class ChargerBehavior implements EnemyBehavior {

    /** Les phases du Chargeur, dans l'ordre où elles s'enchaînent. */
    public enum Phase {
        /** Marche vers le joueur. */
        APPROACH,
        /** Annonce la charge, immobile. */
        WIND_UP,
        /** Fonce en ligne droite. */
        CHARGE,
        /** Étourdi après la charge, vulnérable. */
        STUNNED
    }

    /** Distance au joueur à partir de laquelle il annonce sa charge. */
    public static final float TRIGGER_DISTANCE = 160f;

    /** Durée de l'annonce, en secondes. */
    public static final float WIND_UP_DURATION = 0.6f;

    /** Vitesse de la charge, en unités par seconde. */
    public static final float CHARGE_SPEED = 300f;

    /** Durée maximale de la charge, en secondes : il parcourt au plus 240 u. */
    public static final float CHARGE_MAX_DURATION = 0.8f;

    /** Durée de l'étourdissement, en secondes. */
    public static final float STUN_DURATION = 1f;

    // Tolérance sur les minuteurs : 36 pas de 1/60 s font 0,6 s à un arrondi de float près.
    // Sans elle, une phase pourrait durer un pas de trop selon l'arrondi.
    private static final float EPSILON = 1e-4f;

    // En dessous de cette part du déplacement voulu, on considère que la charge a heurté un mur.
    private static final float BLOCKED_RATIO = 0.99f;

    private final float chargeSpeed;
    private Phase phase = Phase.APPROACH;
    private float phaseTime;
    private Vec2 chargeDirection = Vec2.ZERO;

    /** Crée le comportement du Chargeur, qui charge à {@link #CHARGE_SPEED}. */
    public ChargerBehavior() {
        this(CHARGE_SPEED);
    }

    /**
     * Crée un comportement de charge plus rapide ou plus lent : le boss réutilise ainsi la
     * logique du Chargeur avec sa propre vitesse.
     *
     * @param chargeSpeed vitesse de la charge, en unités par seconde
     */
    public ChargerBehavior(float chargeSpeed) {
        this.chargeSpeed = chargeSpeed;
    }

    @Override
    public void act(Enemy self, World world, float dt) {
        phaseTime += dt;
        switch (phase) {
            case APPROACH -> approach(self, world, dt);
            case WIND_UP -> {
                if (elapsed(WIND_UP_DURATION)) {
                    // La direction est figée maintenant : le joueur qui a vu l'annonce peut l'esquiver.
                    chargeDirection = world.playerPosition().sub(self.position()).normalized();
                    enter(Phase.CHARGE);
                }
            }
            case CHARGE -> charge(self, world, dt);
            case STUNNED -> {
                if (elapsed(STUN_DURATION)) {
                    enter(Phase.APPROACH);
                }
            }
        }
    }

    private void approach(Enemy self, World world, float dt) {
        Vec2 player = world.playerPosition();
        boolean inRange = self.position().distanceTo(player) <= TRIGGER_DISTANCE;
        if (inRange && world.hasClearPath(self.position(), player, self.hitbox().radius())) {
            enter(Phase.WIND_UP);
        } else {
            self.chase(world, dt);
        }
    }

    private void charge(Enemy self, World world, float dt) {
        Vec2 wanted = chargeDirection.scale(chargeSpeed * dt);
        Vec2 moved = self.moveBy(wanted, world);
        boolean blocked = moved.length() < wanted.length() * BLOCKED_RATIO;
        if (blocked || elapsed(CHARGE_MAX_DURATION)) {
            enter(Phase.STUNNED);
        }
    }

    private boolean elapsed(float duration) {
        return phaseTime >= duration - EPSILON;
    }

    private void enter(Phase next) {
        phase = next;
        phaseTime = 0f;
    }

    /** @return la phase actuelle */
    public Phase phase() {
        return phase;
    }

    /** @return le temps passé dans la phase actuelle, en secondes (sert au clignotement de la vue) */
    public float phaseTime() {
        return phaseTime;
    }

    /** @return {@code true} pendant l'annonce de la charge */
    public boolean isWindingUp() {
        return phase == Phase.WIND_UP;
    }

    /** @return {@code true} pendant la charge, le seul moment où le Chargeur blesse au contact */
    public boolean isCharging() {
        return phase == Phase.CHARGE;
    }

    /** @return {@code true} pendant l'étourdissement qui suit la charge */
    public boolean isStunned() {
        return phase == Phase.STUNNED;
    }
}
