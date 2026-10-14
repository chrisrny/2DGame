package game.model.world;

import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.EventBus;
import java.util.Objects;
import java.util.Random;

/**
 * Une partie complète, du début à la victoire ou à la défaite.
 *
 * <p>C'est le point d'entrée du modèle : la vue l'appelle à chaque pas de jeu avec
 * l'intention du joueur, et lit son état pour dessiner. Le joueur évolue sur la grande map
 * de la démo ; les stages et leurs vagues (P1-B6, P1-B7) viendront s'y brancher.</p>
 */
public final class Run {

    private final GameConfig config;
    private final EventBus events = new EventBus();
    private final Random random;
    private final Player player;
    private final GameMap map;
    private float elapsed;

    /**
     * Crée une partie.
     *
     * @param config les réglages de la partie (graine du hasard, difficulté, statistiques)
     */
    public Run(GameConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        MapLayout layout = MapLayouts.main();
        // Une seule source de hasard pour toute la partie, initialisée avec la graine :
        // le joueur, les ennemis et les vagues doivent tous tirer leur hasard d'ici.
        this.random = new Random(config.seed());
        this.player = new Player(layout.center(), config.player(), random);
        this.map = new GameMap(layout, player, events);
        // En attendant les vagues (P1-B6) : deux Rôdeurs et un Archer posés à la main.
        map.addEnemy(new Prowler(new Vec2(480f, 600f)));
        map.addEnemy(new Prowler(new Vec2(1440f, 560f)));
        map.addEnemy(new Archer(new Vec2(1300f, 300f)));
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
        map.update(intent, dt);
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

    /** @return la map de la partie */
    public GameMap map() {
        return map;
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
