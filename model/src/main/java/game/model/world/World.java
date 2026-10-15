package game.model.world;

import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.event.EventBus;
import java.util.List;

/**
 * Ce que le joueur et les ennemis peuvent savoir et faire dans la salle où ils se trouvent.
 *
 * <p>Implémentée par {@link GameMap}. Le joueur et les ennemis passent par cette interface
 * plutôt que par {@code GameMap} directement : les tests peuvent fournir un faux monde simple.</p>
 */
public interface World {

    /** @return la position actuelle du joueur */
    Vec2 playerPosition();

    /** @return les limites de l'arène */
    Rect arena();

    /** @return tous les rectangles infranchissables : murs, obstacles et portes fermées */
    List<Rect> walls();

    /**
     * Les cibles qu'un attaquant peut toucher dans une zone.
     *
     * @param zone     la zone de l'attaque
     * @param attacker la faction de l'attaquant
     * @return les {@link Damageable} vivants de l'autre faction qui touchent la zone
     */
    List<Damageable> targetsIn(Circle zone, Faction attacker);

    /**
     * Ajoute un projectile dans la salle.
     *
     * @param projectile le projectile tiré
     */
    void spawnProjectile(Projectile projectile);

    /** @return le bus sur lequel publier les événements du jeu */
    EventBus events();
}
