package game.model.entity;

import game.model.ai.ProwlerBehavior;
import game.model.commun.Vec2;

/**
 * Boss provisoire du stage 3 : un Rôdeur géant, lent et solide, qui fonce sur le joueur.
 *
 * <p>Il permet de jouer une partie complète, de la première vague à la victoire, avant que
 * le vrai boss et ses deux phases arrivent (P2-B1, P2-B2). Il sera alors retiré.</p>
 */
public final class GiantProwler extends Enemy {

    /** Points de vie maximum. */
    public static final int MAX_HP = 300;

    /** Rayon de la zone de collision : plus du double d'un Rôdeur. */
    public static final float RADIUS = 20f;

    /** Vitesse, en unités par seconde : un peu plus lent qu'un Rôdeur, on peut lui échapper. */
    public static final float SPEED = 55f;

    /** Dégâts infligés au joueur qui le touche. */
    public static final int CONTACT_DAMAGE = 20;

    /**
     * Crée le Rôdeur géant.
     *
     * @param position   position d'apparition
     * @param multiplier multiplicateur des points de vie et des dégâts
     */
    public GiantProwler(Vec2 position, float multiplier) {
        super(position, MAX_HP, RADIUS, SPEED, CONTACT_DAMAGE, new ProwlerBehavior(), multiplier);
    }
}
