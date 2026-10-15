package game.model.world;

import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import game.model.player.Experience;
import game.model.relic.Relic;
import game.model.relic.RelicFactory;
import java.util.List;
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
    private final Experience experience;
    private List<Relic> pendingChoice = List.of();
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
        this.experience = new Experience(events);
        // En attendant les vagues (P1-B6) : deux Rôdeurs, un Archer et un Chargeur posés à la main.
        map.addEnemy(new Prowler(new Vec2(480f, 600f)));
        map.addEnemy(new Prowler(new Vec2(1440f, 560f)));
        map.addEnemy(new Archer(new Vec2(1300f, 300f)));
        map.addEnemy(new Charger(new Vec2(1760f, 520f)));
    }

    /** Nombre de reliques proposées à chaque niveau gagné. */
    public static final int RELIC_CHOICES = 3;

    /**
     * Fait avancer la partie d'un pas de jeu.
     *
     * <p>Pendant un choix de relique, la partie est figée : rien ne bouge et le temps de jeu
     * n'avance pas, jusqu'à {@link #chooseRelic(int)}. Après chaque pas, si le joueur vient de
     * gagner un niveau, un choix de reliques est proposé.</p>
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param dt     durée du pas, en secondes (toujours {@code FixedStep.DT} dans le jeu)
     */
    public void update(PlayerIntent intent, float dt) {
        Objects.requireNonNull(intent, "intent");
        if (isChoosingRelic()) {
            return;
        }
        elapsed += dt;
        map.update(intent, dt);
        events.flush();
        offerNextChoice();
    }

    /**
     * Propose un choix de reliques pour le prochain niveau gagné, s'il y en a un. Un niveau
     * gagné alors qu'il ne reste aucune relique à proposer est simplement passé.
     */
    private void offerNextChoice() {
        while (pendingChoice.isEmpty() && player.isAlive() && experience.consumeLevelUp()) {
            pendingChoice = RelicFactory.draw(RELIC_CHOICES, random, player.relics());
        }
    }

    /**
     * Donne au joueur une des reliques proposées, publie {@link GameEvent.RelicChosen}, puis
     * propose le choix suivant si un autre niveau attend ; sinon, la partie reprend.
     *
     * @param index position de la relique choisie dans {@link #pendingChoice()}
     * @throws IllegalStateException     si aucun choix n'est en cours
     * @throws IndexOutOfBoundsException si {@code index} ne désigne aucune relique proposée
     */
    public void chooseRelic(int index) {
        if (!isChoosingRelic()) {
            throw new IllegalStateException("Aucun choix de relique en cours");
        }
        Relic chosen = pendingChoice.get(Objects.checkIndex(index, pendingChoice.size()));
        player.addRelic(chosen);
        events.publish(new GameEvent.RelicChosen(chosen.name()));
        events.flush();
        pendingChoice = List.of();
        offerNextChoice();
    }

    /** @return {@code true} si la partie attend que le joueur choisisse une relique */
    public boolean isChoosingRelic() {
        return !pendingChoice.isEmpty();
    }

    /** @return les reliques proposées au joueur, ou une liste vide s'il n'y a pas de choix en cours */
    public List<Relic> pendingChoice() {
        return pendingChoice;
    }

    /** @return l'expérience et le niveau du joueur */
    public Experience experience() {
        return experience;
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
