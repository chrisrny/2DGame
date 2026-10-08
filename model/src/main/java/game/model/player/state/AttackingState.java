package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.entity.Player;
import game.model.world.World;

/**
 * Le joueur attaque : le coup part dès le premier pas, puis le joueur reste immobile
 * jusqu'à la fin de l'attaque ({@link Player#ATTACK_DURATION}).
 */
public final class AttackingState implements PlayerState {

    private float elapsed;
    private boolean struck;

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        if (elapsed < Player.ATTACK_DURATION) {
            return this;
        }
        if (intent.move().length() > 0f) {
            return new MovingState();
        }
        return new IdleState();
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        if (!struck) {
            player.strike(world);
            struck = true;
        }
        elapsed += dt;
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.ATTACKING;
    }
}
