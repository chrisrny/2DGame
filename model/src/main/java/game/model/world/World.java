package game.model.world;

import game.model.combat.Collisions;
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

    /**
     * Le point qu'un ennemi doit viser pour rejoindre le joueur en contournant les murs.
     *
     * <p>Par défaut, le joueur lui-même : c'est une ligne droite, suffisante pour un monde sans
     * obstacle. {@link GameMap} la remplace par un vrai calcul de chemin ({@link NavGrid}).</p>
     *
     * @param from   position de l'ennemi
     * @param radius rayon de l'ennemi
     * @return le point à viser maintenant
     */
    default Vec2 pathTowardPlayer(Vec2 from, float radius) {
        return playerPosition();
    }

    /**
     * Indique si aucun mur ne se trouve entre deux points : un Archer ne tire pas à travers
     * un mur, un Chargeur ne charge pas dans un mur.
     *
     * @param from le premier point
     * @param to   le second point
     * @return {@code true} si la ligne entre les deux points ne traverse aucun mur
     */
    default boolean hasLineOfSight(Vec2 from, Vec2 to) {
        return Collisions.pathIsClear(from, to, 0f, walls());
    }

    /**
     * Indique si un corps de rayon donné peut aller en ligne droite d'un point à un autre sans
     * toucher de mur : un Chargeur ne lance sa charge que si elle ne finira pas contre un coin.
     *
     * @param from   point de départ
     * @param to     point d'arrivée
     * @param radius rayon du corps qui se déplace
     * @return {@code true} si aucun mur n'est sur le chemin
     */
    default boolean hasClearPath(Vec2 from, Vec2 to, float radius) {
        return Collisions.pathIsClear(from, to, radius, walls());
    }

    /** @return le bus sur lequel publier les événements du jeu */
    EventBus events();
}
