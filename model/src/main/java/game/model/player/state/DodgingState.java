package game.model.player.state;

import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.World;
import java.util.Objects;

/**
 * Le joueur esquive : il file en ligne droite dans une direction figée au départ,
 * sur {@link PlayerStats#dodgeDistance()} en {@link PlayerStats#dodgeDuration()},
 * et ne peut pas être touché pendant tout ce temps.
 */
public final class DodgingState implements PlayerState {

    private final Vec2 direction;
    private float elapsed;
    private float travelled;

    /**
     * Crée une esquive.
     *
     * @param direction direction de l'esquive, de longueur 1
     */
    public DodgingState(Vec2 direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    @Override
    public PlayerState next(Player player, PlayerIntent intent) {
        PlayerStats stats = player.stats();
        boolean finished = elapsed >= stats.dodgeDuration() || travelled >= stats.dodgeDistance();
        if (!finished) {
            return this;
        }
        if (intent.move().length() > 0f) {
            return new MovingState();
        }
        return new IdleState();
    }

    @Override
    public void act(Player player, PlayerIntent intent, World world, float dt) {
        PlayerStats stats = player.stats();
        float speed = stats.dodgeDistance() / stats.dodgeDuration();
        // On ne dépasse jamais la distance prévue, même si les pas ne tombent pas juste.
        float step = Math.min(speed * dt, stats.dodgeDistance() - travelled);
        player.moveBy(direction.scale(step), world);
        travelled += step;
        elapsed += dt;
    }

    /** Pendant toute l'esquive, le joueur ignore les coups. */
    @Override
    public boolean invulnerable() {
        return true;
    }

    @Override
    public PlayerStateKind kind() {
        return PlayerStateKind.DODGING;
    }
}
