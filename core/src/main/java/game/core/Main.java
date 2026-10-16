package game.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import game.core.screen.DefeatScreen;
import game.core.screen.GameScreen;
import game.core.screen.TitleScreen;
import game.core.screen.VictoryScreen;
import game.model.commun.Difficulty;
import game.model.commun.GameConfig;
import game.model.commun.Settings;
import game.model.world.Run;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Classe principale du jeu côté libGDX : elle passe d'un écran à l'autre (pattern State).
 *
 * <p>Chaque écran est un état : titre, partie, pause, options, victoire, défaite. Il décide
 * lui-même vers quel écran aller, et le demande à {@code Main}. Les écrans mis de côté le temps
 * d'en afficher un autre par-dessus (la partie derrière la pause, la pause derrière les options)
 * sont gardés dans une pile :</p>
 * <ul>
 *   <li>{@link #push(Screen)} affiche un écran par-dessus le courant, qui est gardé ;</li>
 *   <li>{@link #pop()} ferme l'écran courant, le libère et revient au précédent ;</li>
 *   <li>{@link #replaceAll(Screen)} affiche un écran et libère tous les autres.</li>
 * </ul>
 * <p>Un écran quitté est donc toujours libéré ({@code dispose}) : il n'y a pas de fuite.</p>
 *
 * <p>{@code Main} garde aussi les réglages du joueur ({@link #settings()}), partagés par l'écran
 * des options et la partie en cours, le temps que le jeu est ouvert.</p>
 */
public class Main extends Game {

    private final Deque<Screen> suspended = new ArrayDeque<>();
    private final Settings settings = new Settings(Difficulty.NORMAL);

    @Override
    public void create() {
        showTitle();
    }

    /** Affiche l'écran titre et libère tous les autres écrans. */
    public void showTitle() {
        replaceAll(new TitleScreen(this));
    }

    /** Lance une nouvelle partie, avec une nouvelle graine, et libère tous les autres écrans. */
    public void newGame() {
        replaceAll(new GameScreen(this, new Run(GameConfig.defaults(), settings)));
    }

    /** @return les réglages du joueur, modifiés par l'écran des options et lus par la partie */
    public Settings settings() {
        return settings;
    }

    /**
     * Affiche l'écran de fin qui correspond à la partie terminée.
     *
     * @param run la partie terminée
     */
    public void showEnd(Run run) {
        replaceAll(run.isWon() ? new VictoryScreen(this, run) : new DefeatScreen(this, run));
    }

    /**
     * Affiche un écran par-dessus l'écran courant, qui est gardé tel quel pour y revenir.
     *
     * @param next l'écran à afficher
     */
    public void push(Screen next) {
        Objects.requireNonNull(next, "next");
        suspended.push(getScreen());
        setScreen(next);
    }

    /**
     * Ferme l'écran courant, le libère, et revient à l'écran mis de côté juste avant.
     *
     * @throws IllegalStateException s'il n'y a aucun écran où revenir
     */
    public void pop() {
        if (suspended.isEmpty()) {
            throw new IllegalStateException("Aucun écran où revenir");
        }
        Screen finished = getScreen();
        setScreen(suspended.pop());
        finished.dispose();
    }

    /**
     * Affiche un écran et libère tous les autres : le courant et ceux mis de côté.
     *
     * @param next l'écran à afficher
     */
    public void replaceAll(Screen next) {
        Objects.requireNonNull(next, "next");
        Screen old = getScreen();
        setScreen(next);
        if (old != null) {
            old.dispose();
        }
        disposeSuspended();
    }

    @Override
    public void dispose() {
        // Libère l'écran courant et ceux mis de côté : chaque écran libère ses propres ressources.
        if (getScreen() != null) {
            getScreen().dispose();
        }
        disposeSuspended();
    }

    private void disposeSuspended() {
        while (!suspended.isEmpty()) {
            suspended.pop().dispose();
        }
    }
}
