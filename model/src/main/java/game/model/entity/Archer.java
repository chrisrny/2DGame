package game.model.entity;

import game.model.ai.ArcherBehavior;
import game.model.commun.Vec2;

/**
 * L'Archer : un ennemi à distance, qui garde ses distances avec le joueur et lui tire dessus.
 *
 * @see ArcherBehavior
 */
public final class Archer extends Enemy {

    /** Points de vie maximum. */
    public static final int MAX_HP = 20;

    /** Rayon de la zone de collision. */
    public static final float RADIUS = 8f;

    /** Vitesse, en unités par seconde : plus lent que le Rôdeur, on peut le rattraper. */
    public static final float SPEED = 55f;

    /** Dégâts infligés au joueur qui le touche : faibles, son attaque est le tir. */
    public static final int CONTACT_DAMAGE = 4;

    /**
     * Crée un Archer aux valeurs de référence.
     *
     * @param position position d'apparition
     */
    public Archer(Vec2 position) {
        this(position, 1f);
    }

    /**
     * Crée un Archer renforcé : le multiplicateur s'applique aussi aux dégâts de ses tirs.
     *
     * @param position   position d'apparition
     * @param multiplier multiplicateur des points de vie et des dégâts
     */
    public Archer(Vec2 position, float multiplier) {
        super(position, MAX_HP, RADIUS, SPEED, CONTACT_DAMAGE, new ArcherBehavior(), multiplier);
    }
}
