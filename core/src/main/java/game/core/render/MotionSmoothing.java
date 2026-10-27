package game.core.render;

import game.model.combat.Projectile;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.world.Run;
import java.util.IdentityHashMap;
import java.util.Map;

/** Lissage des déplacements entre deux pas du modèle (interpolation). */
public final class MotionSmoothing {

    /**
     * Au-delà de ce saut entre deux pas, en unités, on n'interpole pas : c'est une téléportation
     * (nouvel étage, objet réutilisé par un pool), pas un déplacement.
     */
    private static final float MAX_STEP_DISTANCE = 40f;

    private final Map<Object, Vec2> previous = new IdentityHashMap<>();
    private float progress = 1f;

    /**
     * Note la position de chaque entité qui bouge. À appeler juste avant chaque pas du modèle.
     *
     * @param run la partie
     */
    public void beforeStep(Run run) {
        previous.clear();
        previous.put(run.player(), run.player().position());
        for (Enemy enemy : run.map().enemies()) {
            previous.put(enemy, enemy.position());
        }
        for (Projectile projectile : run.map().projectiles()) {
            previous.put(projectile, projectile.position());
        }
    }

    /**
     * Règle l'avancement vers le pas suivant, à appeler une fois par image après les pas.
     *
     * @param value le temps en attente dans l'horloge divisé par la durée d'un pas, de 0 à 1
     */
    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
    }

    /**
     * Position à dessiner pour une entité.
     *
     * @param entity l'entité (le joueur, un ennemi, un projectile)
     * @param current sa position actuelle dans le modèle
     * @return la position lissée entre le pas précédent et l'actuel
     */
    public Vec2 of(Object entity, Vec2 current) {
        Vec2 before = previous.get(entity);
        if (before == null || before.distanceTo(current) > MAX_STEP_DISTANCE) {
            return current;
        }
        return before.add(current.sub(before).scale(progress));
    }
}
