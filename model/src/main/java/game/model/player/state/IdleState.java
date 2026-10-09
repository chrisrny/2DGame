package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/** Le joueur est immobile et attend une intention. */
public final class IdleState implements PlayerState {

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        if (player.canStartDodge()) {
            return player.startDodge(intent.move());
        }
        if (player.canStartAttack()) {
            return new AttackingState();
        }
        if (intent.move().length() > 0f) {
            return new MovingState();
        }
        return this;
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        // Au repos, le joueur ne fait rien.
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.IDLE;
    }
}
