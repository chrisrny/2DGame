package game.model.relic;

import game.model.commun.PlayerStats;

/**
 * Cœur de braise : +20 points de vie maximum. Le joueur gagne aussi ces 20 points de vie
 * au moment où il prend la relique (géré par le joueur, qui compare l'ancien et le nouveau
 * maximum).
 */
public final class EmberHeart extends AbstractRelic {

    /** Points de vie maximum ajoutés. */
    public static final int BONUS_HP = 20;

    /** Crée la relique. */
    public EmberHeart() {
        super("Coeur de braise", "+20 PV max");
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats.withMaxHp(stats.maxHp() + BONUS_HP);
    }
}
