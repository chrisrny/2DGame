package game.model.relic;

import game.model.commun.PlayerStats;

/** Cœur de braise : +20 points de vie maximum. */
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
