package game.model.entity;

import game.model.ai.ChargerBehavior;
import game.model.commun.Vec2;

/**
 * Le Chargeur : il annonce sa charge, fonce en ligne droite, puis reste étourdi.
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
     * Crée un Chargeur aux valeurs de référence.
     *
     * @param position position d'apparition
     */
    public Charger(Vec2 position) {
        this(position, 1f);
    }

    /**
     * Crée un Chargeur renforcé.
     *
     * @param position position d'apparition
     * @param multiplier multiplicateur des points de vie et des dégâts
     */
    public Charger(Vec2 position, float multiplier) {
        this(position, multiplier, new ChargerBehavior());
    }

    // Constructeur privé : il permet de garder une référence au comportement passé à Enemy, pour
    // que la vue et les dégâts de contact puissent connaître la phase du Chargeur.
    private Charger(Vec2 position, float multiplier, ChargerBehavior behavior) {
        super(position, MAX_HP, RADIUS, SPEED, CHARGE_DAMAGE, behavior, multiplier);
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

    /**
     * Étourdi après une charge (sonné par son élan) ou par un coup qui étourdit.
     *
     * @return {@code true} pendant l'un ou l'autre étourdissement
     */
    @Override
    public boolean isStunned() {
        return super.isStunned() || behavior.isStunned();
    }

    /** Résistance à la poussée pendant la charge : il bouscule les autres et reste sur sa ligne. */
    public static final float CHARGE_PUSH_RESISTANCE = 25f;

    /**
     * En pleine charge, le Chargeur bouscule les autres ennemis et n'est presque pas dévié.
     *
     * @return {@link #CHARGE_PUSH_RESISTANCE} pendant la charge, 1 sinon
     */
    @Override
    public float pushResistance() {
        return behavior.isCharging() ? CHARGE_PUSH_RESISTANCE : 1f;
    }
}
