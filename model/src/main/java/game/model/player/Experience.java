package game.model.player;

import game.model.commun.Faction;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.Objects;

/**
 * Expérience et niveau du joueur (pattern Observer).
 *
 * <p>L'expérience s'abonne au bus d'événements et gagne 1 point à chaque ennemi tué
 * ({@link GameEvent.EntityDied} de faction {@link Faction#ENEMY}) ; la mort du joueur ou d'un
 * allié ne rapporte rien. Personne n'appelle l'expérience directement quand un ennemi meurt :
 * la map publie l'événement sans savoir qui l'écoute.</p>
 *
 * <p>Passer du niveau {@code n} au niveau {@code n + 1} demande {@code 5 + 3 × (n − 1)} points :
 * 5, puis 8, puis 11… Les niveaux gagnés sont mis en file : chacun donnera droit à un choix
 * d'amélioration, l'un après l'autre.</p>
 */
public final class Experience {

    /** Points nécessaires pour passer du niveau 1 au niveau 2. */
    public static final int FIRST_LEVEL_XP = 5;

    /** Points supplémentaires demandés à chaque niveau. */
    public static final int XP_STEP = 3;

    private int level = 1;
    private int xp;
    private int pendingLevelUps;

    /**
     * Crée l'expérience d'un joueur au niveau 1 et l'abonne au bus.
     *
     * @param events le bus d'événements de la partie
     */
    public Experience(EventBus events) {
        Objects.requireNonNull(events, "events").subscribe(this::onEvent);
    }

    private void onEvent(GameEvent event) {
        if (event instanceof GameEvent.EntityDied died && died.faction() == Faction.ENEMY) {
            gain(1);
        }
    }

    /**
     * Ajoute des points d'expérience et fait monter de niveau autant de fois que nécessaire.
     *
     * @param amount points gagnés ; une valeur nulle ou négative ne fait rien
     */
    public void gain(int amount) {
        if (amount <= 0) {
            return;
        }
        xp += amount;
        while (xp >= xpToNextLevel()) {
            xp -= xpToNextLevel();
            level++;
            pendingLevelUps++;
        }
    }

    /**
     * Points nécessaires pour passer d'un niveau au suivant.
     *
     * @param fromLevel le niveau de départ, à partir de 1
     * @return les points pour passer au niveau {@code fromLevel + 1}
     */
    public static int xpForLevel(int fromLevel) {
        return FIRST_LEVEL_XP + XP_STEP * (fromLevel - 1);
    }

    /**
     * Retire un niveau gagné de la file, quand le choix d'amélioration correspondant est proposé.
     *
     * @return {@code true} si un niveau gagné attendait son choix
     */
    public boolean consumeLevelUp() {
        if (pendingLevelUps == 0) {
            return false;
        }
        pendingLevelUps--;
        return true;
    }

    /** @return le niveau actuel, à partir de 1 */
    public int level() {
        return level;
    }

    /** @return les points gagnés depuis le dernier niveau */
    public int xp() {
        return xp;
    }

    /** @return les points nécessaires pour passer au niveau suivant */
    public int xpToNextLevel() {
        return xpForLevel(level);
    }

    /** @return le nombre de niveaux gagnés dont le choix d'amélioration n'a pas encore été proposé */
    public int pendingLevelUps() {
        return pendingLevelUps;
    }
}
