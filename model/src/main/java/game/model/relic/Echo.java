package game.model.relic;

import game.model.entity.Player;
import game.model.world.World;

/**
 * Écho : une attaque sur {@link #EVERY} frappe une deuxième fois, {@link #DELAY} seconde plus tard.
 */
public final class Echo extends AbstractRelic {

    /** Une attaque sur combien déclenche l'écho. */
    public static final int EVERY = 4;

    /** Délai entre l'attaque et son écho, en secondes. */
    public static final float DELAY = 0.15f;

    // Même marge que pour les autres durées décomptées pas à pas.
    private static final float EPSILON = 1e-4f;

    private int attacks;
    private boolean pending;
    private float echoIn;

    /** Crée la relique. */
    public Echo() {
        super("Écho", "1 attaque sur 4 frappe 2 fois");
    }

    @Override
    public void onAttack(Player player) {
        attacks = (attacks + 1) % EVERY;
        if (attacks == 0) {
            pending = true;
            echoIn = DELAY;
        }
    }

    // On vérifie avant de décompter : le pas de l'attaque ne compte pas, l'écho part donc
    // exactement DELAY après elle.
    @Override
    public void update(Player player, World world, float dt) {
        if (!pending) {
            return;
        }
        if (echoIn <= EPSILON) {
            pending = false;
            if (player.isAlive()) {
                player.echoStrike(world);
            }
            return;
        }
        echoIn -= dt;
    }
}
