package game.model.relic;

import game.model.commun.PlayerStats;

/** Plume d'ombre : la recharge de l'esquive est 20 % plus courte. */
public final class ShadowFeather extends AbstractRelic {

    /** Multiplicateur de la recharge d'esquive. */
    public static final float COOLDOWN_MULTIPLIER = 0.8f;

    /** Crée la relique. */
    public ShadowFeather() {
        super("Plume d'ombre", "−20 % de recharge d'esquive");
    }

    @Override
    public PlayerStats apply(PlayerStats stats) {
        return stats.withDodgeCooldown(stats.dodgeCooldown() * COOLDOWN_MULTIPLIER);
    }
}
