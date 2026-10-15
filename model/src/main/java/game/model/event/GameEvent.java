package game.model.event;

import game.model.commun.Faction;
import game.model.commun.Vec2;

/**
 * Quelque chose qui s'est passé dans le jeu, annoncé par le modèle sur l'{@link EventBus}.
 *
 * <p>L'interface est scellée : tous les événements possibles sont déclarés ici. Un écouteur
 * peut donc faire un {@code switch} sur l'événement, et le compilateur vérifie qu'aucun cas
 * n'est oublié. Les événements copient les valeurs utiles (position, dégâts) au lieu de
 * garder une référence vers une entité qui peut être morte au moment où on les lit.</p>
 */
public sealed interface GameEvent {

    /**
     * Une cible a reçu un coup.
     *
     * @param position position de la cible au moment du coup
     * @param damage   dégâts infligés
     * @param critical {@code true} pour un coup critique
     * @param target   faction de la cible touchée
     */
    record EntityHit(Vec2 position, int damage, boolean critical, Faction target) implements GameEvent { }

    /**
     * Un ennemi ou le boss est mort.
     *
     * @param position position au moment de la mort
     * @param faction  faction de l'entité morte
     */
    record EntityDied(Vec2 position, Faction faction) implements GameEvent { }

    /** Le joueur est mort : la partie est perdue. */
    record PlayerDied() implements GameEvent { }

    /**
     * Le joueur a tué assez d'ennemis pour finir un stage : la partie peut passer au suivant.
     * Publié une seule fois par stage.
     *
     * @param stage numéro du stage terminé (1, 2 ou 3)
     */
    record StageCleared(int stage) implements GameEvent { }

    /**
     * Le joueur a choisi une relique.
     *
     * @param relicName nom de la relique choisie
     */
    record RelicChosen(String relicName) implements GameEvent { }
}
