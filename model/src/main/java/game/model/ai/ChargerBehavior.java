package game.model.ai;

import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.world.World;

/** Comportement du Chargeur : il prévient, charge en ligne droite, puis reste vulnérable. */
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
    private static final float EPSILON = 1e-4f;

    // En dessous de cette part du déplacement voulu, on considère que la charge a heurté un mur.
    private static final float BLOCKED_RATIO = 0.99f;

    // Temps de lissage de la vitesse du joueur, en secondes : un seul pas brusque (esquive, recul)
    // ne fausse pas la visée.
    private static final float VELOCITY_SMOOTHING = 0.1f;

    private final float chargeSpeed;
    private final float windUpDuration;
    private final float lead;
    private Phase phase = Phase.APPROACH;
    private float phaseTime;
    private Vec2 chargeDirection = Vec2.ZERO;
    private Vec2 lastPlayerPosition;
    private Vec2 playerVelocity = Vec2.ZERO;

    /** Crée le comportement du Chargeur, qui charge à {@link #CHARGE_SPEED}. */
    public ChargerBehavior() {
        this(CHARGE_SPEED);
    }

    /**
     * Crée un comportement de charge plus rapide ou plus lent : le boss réutilise ainsi la logique
     * du Chargeur avec sa propre vitesse.
     *
     * @param chargeSpeed vitesse de la charge, en unités par seconde
     */
    public ChargerBehavior(float chargeSpeed) {
        this(chargeSpeed, WIND_UP_DURATION, 0f);
    }

    /**
     * Crée un comportement de charge entièrement réglé : le boss s'en sert pour une charge plus
     * rapide, annoncée moins longtemps, et qui anticipe le déplacement du joueur.
     *
     * @param chargeSpeed vitesse de la charge, en unités par seconde
     * @param windUpDuration durée de l'annonce, en secondes
     * @param lead part de l'anticipation : 0, il fonce là où est le joueur ; 1, là où il sera s'il
     *     garde sa vitesse
     */
    public ChargerBehavior(float chargeSpeed, float windUpDuration, float lead) {
        this.chargeSpeed = chargeSpeed;
        this.windUpDuration = windUpDuration;
        this.lead = lead;
    }

    @Override
    public void act(Enemy self, World world, float dt) {
        phaseTime += dt;
        trackPlayer(world, dt);
        switch (phase) {
            case APPROACH -> approach(self, world, dt);
            case WIND_UP -> {
                if (elapsed(windUpDuration)) {
                    // La direction est figée maintenant : le joueur qui a vu l'annonce peut l'esquiver.
                    chargeDirection = aim(self, world);
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

    // Vitesse du joueur, lissée : la différence de sa position d'un pas à l'autre.
    private void trackPlayer(World world, float dt) {
        Vec2 player = world.playerPosition();
        if (lastPlayerPosition != null && dt > 0f) {
            Vec2 instant = player.sub(lastPlayerPosition).scale(1f / dt);
            float weight = Math.min(1f, dt / VELOCITY_SMOOTHING);
            playerVelocity = playerVelocity.add(instant.sub(playerVelocity).scale(weight));
        }
        lastPlayerPosition = player;
    }

    // Vers le joueur, décalé de là où il sera dans le temps que met la charge à l'atteindre.
    private Vec2 aim(Enemy self, World world) {
        Vec2 player = world.playerPosition();
        float travelTime = Math.min(self.position().distanceTo(player) / chargeSpeed, CHARGE_MAX_DURATION);
        Vec2 target = player.add(playerVelocity.scale(lead * travelTime));
        Vec2 direction = target.sub(self.position());
        return direction.length() > 0f ? direction.normalized() : player.sub(self.position()).normalized();
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

    /** @return la durée de l'annonce de la charge, en secondes */
    public float windUpDuration() {
        return windUpDuration;
    }

    /** @return la direction de la charge, figée à son départ (nulle avant la première charge) */
    public Vec2 chargeDirection() {
        return chargeDirection;
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
