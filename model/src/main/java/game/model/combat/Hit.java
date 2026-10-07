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
 */
public record Hit(int damage, Faction source, boolean critical, Vec2 knockback) {

    /**
     * Vérifie que le coup est valide.
     *
     * @throws NullPointerException     si la source ou le recul est {@code null}
     * @throws IllegalArgumentException si les dégâts sont négatifs
     */
    public Hit {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(knockback, "knockback");
        if (damage < 0) {
            throw new IllegalArgumentException("Les dégâts ne peuvent pas être négatifs");
        }
    }
}
