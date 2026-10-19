package game.model.relic;

import game.model.entity.Player;

/**
 * Croc vampire : chaque cible touchée soigne le joueur, sans dépasser ses points de vie
 * maximum. Les sorts comptent aussi : chaque ennemi pris dans l'Onde de choc, chaque coup de lame
 * de l'Éventail. Ne change aucune statistique.
 */
public final class VampireFang extends AbstractRelic {

    /** Points de vie rendus par cible touchée. */
    public static final int HEAL_PER_HIT = 1;

    /** Crée la relique. */
    public VampireFang() {
        super("Croc vampire", "Chaque coup soigne 1 PV, sorts compris");
    }

    @Override
    public void onHit(Player player) {
        player.heal(HEAL_PER_HIT);
    }
}
