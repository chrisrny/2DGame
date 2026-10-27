package game.model.ai;

import game.model.entity.Enemy;
import game.model.world.World;

/** La décision d'un ennemi à chaque pas de jeu (pattern Strategy). */
@FunctionalInterface
public interface EnemyBehavior {

    /**
     * Fait agir l'ennemi pendant un pas de jeu : se déplacer, attaquer, tirer.
     *
     * @param self l'ennemi qui agit
     * @param world le monde, pour connaître la position du joueur, les murs et créer des
     *     projectiles
     * @param dt durée du pas, en secondes
     */
    void act(Enemy self, World world, float dt);
}
