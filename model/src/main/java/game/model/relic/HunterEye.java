package game.model.relic;

import game.model.commun.PlayerStats;

/** Œil du chasseur : +10 points de chance de critique, sans jamais dépasser 100 %. */
public final class HunterEye extends AbstractRelic {

    /** Chance de critique ajoutée (0,10 = 10 points). */
    public static final float BONUS_CRIT = 0.10f;

    /** Crée la relique. */
    public HunterEye() {
        super("Œil du chasseur", "+10 % de chance de critique");
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats.withCritChance(Math.min(1f, stats.critChance() + BONUS_CRIT));
    }
}
