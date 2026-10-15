package game.model.entity;

import game.model.ai.ChargerBehavior;
import game.model.commun.Vec2;

/**
 * Le Chargeur : il annonce sa charge, fonce en ligne droite, puis reste étourdi.
 *
 * <p>Il ne blesse au contact que pendant sa charge. Le reste du temps, le toucher est sans
 * danger : étourdi, il est fait pour être puni.</p>
 *
 * @see ChargerBehavior
 */
public final class Charger extends Enemy {

    /** Points de vie maximum. */
    public static final int MAX_HP = 40;

    /** Rayon de la zone de collision : un peu plus gros que les autres ennemis. */
    public static final float RADIUS = 10f;

    /** Vitesse d'approche, en unités par seconde. La charge, elle, va à {@link ChargerBehavior#CHARGE_SPEED}. */
    public static final float SPEED = 60f;

    /** Dégâts infligés au joueur touché pendant la charge. */
    public static final int CHARGE_DAMAGE = 15;

    private final ChargerBehavior behavior;

    /**
     * Crée un Chargeur.
     *
     * @param position position d'apparition
     */
    public Charger(Vec2 position) {
        this(position, new ChargerBehavior());
    }

    // Constructeur privé : il permet de garder une référence au comportement passé à Enemy,
    // pour que la vue et les dégâts de contact puissent connaître la phase du Chargeur.
    private Charger(Vec2 position, ChargerBehavior behavior) {
        super(position, MAX_HP, RADIUS, SPEED, CHARGE_DAMAGE, behavior);
        this.behavior = behavior;
    }

    /**
     * Dégâts de contact : ceux de la charge pendant qu'il charge, aucun le reste du temps.
     *
     * @return les dégâts infligés au joueur qui le touche maintenant
     */
    @Override
    public int contactDamage() {
        return behavior.isCharging() ? super.contactDamage() : 0;
    }

    /** @return la phase actuelle du Chargeur */
    public ChargerBehavior.Phase phase() {
        return behavior.phase();
    }

    /** @return le temps passé dans la phase actuelle, en secondes */
    public float phaseTime() {
        return behavior.phaseTime();
    }

    /** @return {@code true} pendant l'annonce : la vue le fait clignoter */
    public boolean isWindingUp() {
        return behavior.isWindingUp();
    }

    /** @return {@code true} pendant la charge */
    public boolean isCharging() {
        return behavior.isCharging();
    }

    /** @return {@code true} pendant l'étourdissement */
    public boolean isStunned() {
        return behavior.isStunned();
    }
}
