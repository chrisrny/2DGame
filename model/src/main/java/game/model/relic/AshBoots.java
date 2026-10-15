package game.model.relic;

import game.model.commun.PlayerStats;

/** Bottes de cendre : +15 % de vitesse de déplacement. */
public final class AshBoots extends AbstractRelic {

    /** Multiplicateur de la vitesse. */
    public static final float SPEED_MULTIPLIER = 1.15f;

    /** Crée la relique. */
    public AshBoots() {
        super("Bottes de cendre", "+15 % de vitesse");
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats.withSpeed(stats.speed() * SPEED_MULTIPLIER);
    }
}
