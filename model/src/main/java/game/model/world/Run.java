package game.model.world;

import game.model.commun.Faction;
import game.model.commun.GameConfig;
import game.model.commun.PlayerIntent;
import game.model.commun.Settings;
import game.model.entity.Boss;
import game.model.entity.Player;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import game.model.player.Experience;
import game.model.relic.Relic;
import game.model.relic.RelicFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Une partie complète, du début à la victoire ou à la défaite.
 *
 * <p>C'est le point d'entrée du modèle : la vue l'appelle à chaque pas de jeu avec
 * l'intention du joueur, et lit son état pour dessiner.</p>
 *
 * <p>Une partie enchaîne les trois stages de {@link Stages#demo()} sur la même map, sans
 * téléportation : le joueur garde sa position, ses points de vie et son expérience. Quand un
 * stage est fini ({@link GameEvent.StageCleared}), une pause de {@link #STAGE_INTRO} secondes
 * sans vague annonce le suivant. Au stage 3, le boss apparaît. La partie est perdue à la mort
 * du joueur et gagnée à la mort du boss.</p>
 *
 * <p>La difficulté vient des réglages ({@link Settings}) : elle est lue au début de chaque
 * stage, donc un changement en pleine partie vaut à partir du stage suivant.</p>
 */
public final class Run {

    private final GameConfig config;
    private final Settings settings;
    private final EventBus events = new EventBus();
    private final Random random;
    private final Player player;
    private final GameMap map;
    private final Experience experience;
    private final List<StageConfig> stages;
    private int stageIndex;
    private float stageIntroLeft = STAGE_INTRO;
    private boolean nextStagePending;
    private Boss boss;
    private int kills;
    private List<Relic> pendingChoice = List.of();
    private float elapsed;

    /** Durée de l'annonce d'un stage, en secondes : la vue affiche « Stage N » pendant ce temps. */
    public static final float STAGE_INTRO = 2f;

    /**
     * Crée une partie dont la difficulté reste celle de {@code config}.
     *
     * @param config les réglages de la partie (graine du hasard, difficulté, statistiques)
     */
    public Run(GameConfig config) {
        this(config, new Settings(config.difficulty()));
    }

    /**
     * Crée une partie qui suit les réglages du joueur : la difficulté de {@code settings} remplace
     * celle de {@code config}, et peut changer en cours de partie.
     *
     * @param config   la graine du hasard et les statistiques du joueur
     * @param settings les réglages du joueur, partagés avec l'écran des options
     */
    public Run(GameConfig config, Settings settings) {
        this.config = Objects.requireNonNull(config, "config");
        this.settings = Objects.requireNonNull(settings, "settings");
        MapLayout layout = MapLayouts.main();
        // Une seule source de hasard pour toute la partie, initialisée avec la graine :
        // le joueur, les ennemis et les vagues doivent tous tirer leur hasard d'ici.
        this.random = new Random(config.seed());
        this.player = new Player(layout.center(), config.player(), random);
        this.map = new GameMap(layout, player, events, random);
        this.experience = new Experience(events);
        this.stages = Stages.demo();
        events.subscribe(this::onEvent);
        // Le stage 1 commence tout de suite : sa première vague a déjà son propre délai.
        startStage();
    }

    private void onEvent(GameEvent event) {
        if (event instanceof GameEvent.EntityDied died && died.faction() == Faction.ENEMY) {
            kills++;
        } else if (event instanceof GameEvent.StageCleared cleared
                && cleared.stage() == currentStage().number() && stageIndex < stages.size() - 1) {
            // Pas de téléportation : on annonce le stage suivant, puis il commence après la pause.
            stageIndex++;
            stageIntroLeft = STAGE_INTRO;
            nextStagePending = true;
        }
    }

    // Pendant l'annonce d'un nouveau stage, la map n'envoie aucune vague : l'ancien stage est
    // fini et le nouveau n'a pas encore commencé.
    private void updateStageIntro(float dt) {
        stageIntroLeft = Math.max(0f, stageIntroLeft - dt);
        if (nextStagePending && stageIntroLeft == 0f) {
            nextStagePending = false;
            startStage();
        }
    }

    private void startStage() {
        // La difficulté du moment renforce ou affaiblit tous les ennemis de ce stage, boss compris.
        StageConfig stage = currentStage().scaled(settings.difficulty().enemyMultiplier());
        map.startStage(stage);
        if (isBossStage() && boss == null) {
            boss = new Boss(map.randomSpawnPoint(), stage.multiplier());
            map.addEnemy(boss);
        }
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
        updateStageIntro(dt);
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

    /** @return le numéro du stage en cours (ou annoncé), de 1 à 3 */
    public int stage() {
        return currentStage().number();
    }

    /** @return la description du stage en cours (ou annoncé), avant la difficulté */
    public StageConfig currentStage() {
        return stages.get(stageIndex);
    }

    /** @return {@code true} au dernier stage, celui du boss */
    public boolean isBossStage() {
        return stageIndex == stages.size() - 1;
    }

    /** @return les réglages du joueur que suit cette partie */
    public Settings settings() {
        return settings;
    }

    /** @return le nombre d'ennemis tués depuis le début de la partie */
    public int kills() {
        return kills;
    }

    /** @return le nombre d'ennemis tués dans le stage en cours (0 pendant l'annonce d'un nouveau stage) */
    public int stageKills() {
        return nextStagePending ? 0 : map.kills();
    }

    /**
     * Temps restant de l'annonce du stage. Tant qu'il est positif, la vue affiche « Stage N ».
     *
     * @return le temps restant, en secondes, ou 0 s'il n'y a pas d'annonce en cours
     */
    public float stageIntroLeft() {
        return stageIntroLeft;
    }

    /** @return le boss, s'il est déjà apparu (même mort) */
    public Optional<Boss> boss() {
        return Optional.ofNullable(boss);
    }

    /** @return {@code true} si la partie est finie : joueur mort ou boss vaincu */
    public boolean isOver() {
        return !player.isAlive() || isWon();
    }

    /** @return {@code true} si le boss est mort et le joueur encore en vie */
    public boolean isWon() {
        return boss != null && !boss.isAlive() && player.isAlive();
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
