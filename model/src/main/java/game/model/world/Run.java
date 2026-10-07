package game.model.world;

import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.event.EventBus;
import java.util.Objects;

/**
 * Une partie complète, du début à la victoire ou à la défaite.
 *
 * <p>C'est le point d'entrée du modèle : la vue l'appelle à chaque pas de jeu avec
 * l'intention du joueur, et lit son état pour dessiner. Pour l'instant, la partie ne fait
 * que compter le temps ; le joueur (P1-A1) et l'enchaînement des salles (P1-B7) viendront
 * s'y brancher.</p>
 */
public final class Run {

    private final GameConfig config;
    private final EventBus events = new EventBus();
    private float elapsed;

    /**
     * Crée une partie.
     *
     * @param config les réglages de la partie (graine du hasard, difficulté, statistiques)
     */
    public Run(GameConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Fait avancer la partie d'un pas de jeu.
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param dt     durée du pas, en secondes (toujours {@code FixedStep.DT} dans le jeu)
     */
    public void update(PlayerIntent intent, float dt) {
        Objects.requireNonNull(intent, "intent");
        elapsed += dt;
        // Ici viendront la mise à jour du joueur et de la salle courante.
        events.flush();
    }

    /** @return le temps de jeu écoulé depuis le début de la partie, en secondes */
    public float elapsed() {
        return elapsed;
    }

    /** @return les réglages de la partie */
    public GameConfig config() {
        return config;
    }

    /** @return le bus d'événements de la partie, auquel la vue peut s'abonner */
    public EventBus events() {
        return events;
    }
}
