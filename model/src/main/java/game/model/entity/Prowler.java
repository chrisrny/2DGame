package game.model.entity;

import game.model.ai.ProwlerBehavior;
import game.model.commun.Vec2;

/**
 * Le Rôdeur : l'ennemi de base, qui fonce sur le joueur et le frappe au contact.
 */
public final class Prowler extends Enemy {

    /** Points de vie maximum. */
    public static final int MAX_HP = 30;

    /** Rayon de la zone de collision. */
    public static final float RADIUS = 8f;

    /** Vitesse, en unités par seconde. */
    public static final float SPEED = 70f;

    /** Dégâts infligés au joueur qui le touche. */
    public static final int CONTACT_DAMAGE = 10;

    /**
     * Crée un Rôdeur aux valeurs de référence.
     *
     * @param position position d'apparition
     */
    public Prowler(Vec2 position) {
        this(position, 1f);
    }

    /**
     * Crée un Rôdeur renforcé.
     *
     * @param position   position d'apparition
     * @param multiplier multiplicateur des points de vie et des dégâts
     */
    public Prowler(Vec2 position, float multiplier) {
        super(position, MAX_HP, RADIUS, SPEED, CONTACT_DAMAGE, new ProwlerBehavior(), multiplier);
    }
}
