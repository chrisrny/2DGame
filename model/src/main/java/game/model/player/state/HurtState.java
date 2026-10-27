package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.World;
import java.util.Objects;

/**
 * Le joueur vient d'être touché : il est repoussé et ne contrôle plus rien pendant {@link
 * Player#HURT_DURATION}.
 */
public final class HurtState implements PlayerState {

    private final Vec2 knockback;
    private float elapsed;

    /**
     * Crée l'état touché.
     *
     * @param knockback vitesse de recul au moment du coup, en unités par seconde
     */
    public HurtState(Vec2 knockback) {
        this.knockback = Objects.requireNonNull(knockback, "knockback");
    }

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        if (elapsed < Player.HURT_DURATION) {
            return this;
        }
        if (intent.move().length() > 0f) {
            return new MovingState();
        }
        return new IdleState();
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        if (elapsed < Player.HURT_KNOCKBACK_DURATION) {
            float remaining = 1f - elapsed / Player.HURT_KNOCKBACK_DURATION;
            player.moveBy(knockback.scale(remaining * dt), world);
        }
        elapsed += dt;
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.HURT;
    }
}
