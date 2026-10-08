package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/** Le joueur est mort : il ignore toutes les intentions et ne quitte plus cet état. */
public final class DeadState implements PlayerState {

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        return this;
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        // Un joueur mort ne fait plus rien.
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.DEAD;
    }
}
