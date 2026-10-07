package game.core.input;

import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;

/**
 * Une source d'entrées (clavier et souris, manette) qui produit l'intention du joueur
 * à chaque image (pattern Command).
 *
 * <p>Le modèle ne connaît que {@link PlayerIntent} : on peut changer de source, ou en
 * ajouter une, sans toucher à une seule ligne du modèle.</p>
 */
public interface InputSource {

    /**
     * Lit l'état actuel des entrées et le traduit en intention.
     *
     * @param playerPosition position du joueur dans le monde, pour calculer la direction de visée
     * @return l'intention du joueur pour cette image
     */
    PlayerIntent poll(Vec2 playerPosition);
}
