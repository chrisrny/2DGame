package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/** Le joueur se déplace dans la direction demandée. */
public final class MovingState implements PlayerState {

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        if (player.canStartDodge()) {
            return player.startDodge(intent.move());
        }
        if (player.canStartAttack()) {
            return new AttackingState();
        }
        if (intent.move().length() == 0f) {
            return new IdleState();
        }
        return this;
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        player.walk(intent.move(), world, dt);
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.MOVING;
    }
}
