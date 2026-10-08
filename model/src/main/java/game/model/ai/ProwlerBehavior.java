package game.model.ai;

import game.model.entity.Enemy;
import game.model.world.World;

/**
 * Comportement du Rôdeur : foncer droit sur le joueur.
 */
public final class ProwlerBehavior implements EnemyBehavior {

    @Override
    public void act(Enemy self, World world, float dt) {
        self.moveToward(world.playerPosition(), world, dt);
    }
}
