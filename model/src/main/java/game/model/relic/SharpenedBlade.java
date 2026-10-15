package game.model.relic;

import game.model.commun.PlayerStats;

/** Lame affûtée : +20 % de dégâts d'attaque, arrondis à l'entier le plus proche. */
public final class SharpenedBlade extends AbstractRelic {

    /** Multiplicateur des dégâts. */
    public static final float DAMAGE_MULTIPLIER = 1.2f;

    /** Crée la relique. */
    public SharpenedBlade() {
        super("Lame affûtée", "+20 % de dégâts");
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats.withAttackDamage(Math.round(stats.attackDamage() * DAMAGE_MULTIPLIER));
    }
}
