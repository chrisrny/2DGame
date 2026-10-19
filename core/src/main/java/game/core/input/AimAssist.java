package game.core.input;

import game.model.commun.Vec2;
import java.util.List;
import java.util.Objects;

/**
 * Aide à la visée pour la manette : un stick est moins précis qu'une souris, alors une visée qui
 * passe près d'un ennemi est ramenée sur lui.
 *
 * <p>Seuls comptent les ennemis assez proches ({@link #RANGE}) et dans un cône étroit autour de la
 * visée ({@link #CONE_DEGREES}) : on corrige un écart, on ne vise pas à la place du joueur. Entre
 * plusieurs candidats, le plus aligné l'emporte, le plus proche à égalité.</p>
 */
public final class AimAssist {

    /** Distance maximale d'un ennemi pris en compte, en unités. */
    public static final float RANGE = 260f;

    /** Demi-ouverture du cône autour de la visée, en degrés. */
    public static final float CONE_DEGREES = 28f;

    private static final float MIN_COS = (float) Math.cos(Math.toRadians(CONE_DEGREES));

    private AimAssist() {
    }

    /**
     * Corrige une direction de visée vers l'ennemi le mieux placé, s'il y en a un.
     *
     * @param aim     la visée du joueur, non nulle
     * @param from    la position du joueur
     * @param targets les positions des ennemis
     * @return la direction vers l'ennemi retenu, normalisée, ou {@code aim} normalisée si aucun
     *         ennemi n'est dans le cône
     */
    public static Vec2 adjust(Vec2 aim, Vec2 from, List<Vec2> targets) {
        Objects.requireNonNull(aim, "aim");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(targets, "targets");
        if (aim.length() == 0f) {
            return aim;
        }
        Vec2 dir = aim.normalized();
        Vec2 best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Vec2 target : targets) {
            Vec2 to = target.sub(from);
            float distance = to.length();
            if (distance < 1f || distance > RANGE) {
                continue;
            }
            Vec2 toDir = to.scale(1f / distance);
            float cos = dir.dot(toDir);
            if (cos < MIN_COS) {
                continue;
            }
            // Le mieux aligné d'abord ; la distance ne départage que de près.
            float score = cos - distance / RANGE * 0.02f;
            if (score > bestScore) {
                bestScore = score;
                best = toDir;
            }
        }
        return best != null ? best : dir;
    }
}
