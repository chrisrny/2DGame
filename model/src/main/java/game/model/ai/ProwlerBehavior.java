package game.model.ai;

import game.model.entity.Enemy;
import game.model.world.World;

/**
 * Comportement du Rôdeur : poursuivre le joueur, en contournant les murs.
 */
public final class ProwlerBehavior implements EnemyBehavior {

    @Override
    public void act(Enemy self, World world, float dt) {
        self.chase(world, dt);
    }
}
