package game.model.entity;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;

/** Tout ce qui peut recevoir un coup : le joueur, les ennemis, le boss. */
public interface Damageable {

    /**
     * Applique un coup à cette cible, si elle l'accepte.
     *
     * @param hit le coup reçu
     * @return {@code true} si le coup a été appliqué, {@code false} s'il a été ignoré (tir ami,
     *     invincibilité, cible déjà morte)
     */
    boolean receive(Hit hit);

    /** @return {@code true} tant que la cible a des points de vie */
    boolean isAlive();

    /** @return la zone de collision actuelle */
    Circle hitbox();

    /** @return la faction de la cible */
    Faction faction();
}
