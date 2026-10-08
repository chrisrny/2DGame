package game.model.world;

import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.event.EventBus;
import java.util.Objects;
import java.util.Random;

/**
 * Une partie complète, du début à la victoire ou à la défaite.
 *
 * <p>C'est le point d'entrée du modèle : la vue l'appelle à chaque pas de jeu avec
 * l'intention du joueur, et lit son état pour dessiner. Pour l'instant, le joueur évolue
 * dans une salle vide provisoire ; les vraies salles (P1-B5) et leur enchaînement (P1-B7)
 * viendront la remplacer.</p>
 */
public final class Run {

    /** Limites de la salle provisoire : tout l'écran logique de 640x360. */
    private static final Rect ARENA = new Rect(0f, 0f, 640f, 360f);

    private final GameConfig config;
    private final EventBus events = new EventBus();
    private final Random random;
    private final Player player;
    private final World world;
    private float elapsed;

    /**
     * Crée une partie.
     *
     * @param config les réglages de la partie (graine du hasard, difficulté, statistiques)
     */
    public Run(GameConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        Vec2 center = new Vec2(ARENA.x() + ARENA.width() / 2f, ARENA.y() + ARENA.height() / 2f);
        // Une seule source de hasard pour toute la partie, initialisée avec la graine :
        // le joueur, les ennemis et les vagues doivent tous tirer leur hasard d'ici.
        this.random = new Random(config.seed());
        this.player = new Player(center, config.player(), random);
        this.world = new EmptyRoom(ARENA, events, player::position);
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
        player.update(intent, world, dt);
        // Ici viendra la mise à jour des ennemis de la salle courante.
        events.flush();
    }

    /** @return le temps de jeu écoulé depuis le début de la partie, en secondes */
    public float elapsed() {
        return elapsed;
    }

    /** @return le joueur */
    public Player player() {
        return player;
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
