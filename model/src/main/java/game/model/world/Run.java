package game.model.world;

import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.entity.Player;
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
 * de la démo, où les vagues du stage 1 arrivent. L'enchaînement des stages viendra en P1-B7.</p>
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
        this.map = new GameMap(layout, player, events, random);
        this.experience = new Experience(events);
        // La difficulté choisie renforce ou affaiblit les ennemis de tous les stages.
        map.startStage(Stages.demo().get(0).scaled(config.difficulty().enemyMultiplier()));
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
