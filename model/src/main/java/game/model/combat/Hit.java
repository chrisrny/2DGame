package game.model.combat;

import game.model.commun.Faction;
import game.model.commun.Vec2;
import java.util.Objects;

/**
 * Un coup porté à une cible : attaque, compétence, projectile ou contact d'un ennemi.
 *
 * @param damage    dégâts infligés, positifs ou nuls
 * @param source    faction de celui qui frappe (sert à ignorer le tir ami)
 * @param critical  {@code true} pour un coup critique (affiché plus gros)
 * @param knockback recul appliqué à la cible : direction multipliée par la force,
 *                  {@link Vec2#ZERO} pour un coup sans recul
 * @param stun      durée d'étourdissement infligée, en secondes ({@code 0} pour aucun) ;
 *                  c'est la cible qui décide si elle y est sensible
 */
public record Hit(int damage, Faction source, boolean critical, Vec2 knockback, float stun) {

    /**
     * Vérifie que le coup est valide.
     *
     * @throws NullPointerException     si la source ou le recul est {@code null}
     * @throws IllegalArgumentException si les dégâts ou l'étourdissement sont négatifs
     */
    public Hit {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(knockback, "knockback");
        if (damage < 0) {
            throw new IllegalArgumentException("Les dégâts ne peuvent pas être négatifs");
        }
        if (!(stun >= 0f)) {
            throw new IllegalArgumentException("L'étourdissement ne peut pas être négatif");
        }
    }

    /**
     * Crée un coup sans étourdissement : le cas de presque tous les coups du jeu.
     *
     * @param damage    dégâts infligés, positifs ou nuls
     * @param source    faction de celui qui frappe
     * @param critical  {@code true} pour un coup critique
     * @param knockback recul appliqué à la cible
     */
    public Hit(int damage, Faction source, boolean critical, Vec2 knockback) {
        this(damage, source, critical, knockback, 0f);
    }
}
