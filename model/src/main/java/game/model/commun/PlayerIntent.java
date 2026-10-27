package game.model.commun;

import java.util.Objects;

/**
 * Ce que le joueur veut faire pendant ce pas de jeu (pattern Command).
 *
 * @param move direction de déplacement, de longueur entre 0 et 1 (un stick à moitié penché donne
 *     0,5)
 * @param aim direction de visée, normalisée
 * @param attack bouton d'attaque maintenu
 * @param dodge bouton d'esquive maintenu
 * @param skill1 bouton de la compétence 1 maintenu
 * @param skill2 bouton de la compétence 2 maintenu
 */
public record PlayerIntent(Vec2 move, Vec2 aim, boolean attack, boolean dodge, boolean skill1, boolean skill2) {

    /** Aucune action : immobile, visée vers la droite. */
    public static final PlayerIntent NONE = new PlayerIntent(Vec2.ZERO, new Vec2(1f, 0f), false, false, false, false);

    /**
     * Garantit les règles de l'intention : {@code move} ne dépasse pas une longueur de 1 et {@code
     * aim} est normalisée (vers la droite si elle est nulle).
     *
     * @throws NullPointerException si {@code move} ou {@code aim} est {@code null}
     */
    public PlayerIntent {
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(aim, "aim");
        if (move.length() > 1f) {
            move = move.normalized();
        }
        aim = aim.length() == 0f ? new Vec2(1f, 0f) : aim.normalized();
    }
}
