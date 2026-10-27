package game.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import game.core.audio.MenuMusic;
import game.core.input.Gamepad;
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

/** Classe principale du jeu côté libGDX : elle passe d'un écran à l'autre (pattern State). */
public class Main extends Game {

    private final Deque<Screen> suspended = new ArrayDeque<>();
    private final Settings settings = new Settings(Difficulty.NORMAL);
    private final Gamepad gamepad;
    private boolean usingGamepad;
    private MenuMusic menuMusic;

    /** Crée le jeu sans manette. */
    public Main() {
        this(Gamepad.NONE);
    }

    /**
     * Crée le jeu avec une manette, fournie par le lanceur.
     *
     * @param gamepad la manette ({@link Gamepad#NONE} s'il n'y en a pas)
     */
    public Main(Gamepad gamepad) {
        this.gamepad = Objects.requireNonNull(gamepad, "gamepad");
    }

    @Override
    public void render() {
        gamepad.update();
        trackDevice();
        if (menuMusic != null) {
            menuMusic.update(Gdx.graphics.getDeltaTime());
        }
        super.render();
    }

    // Le joueur joue à la manette dès qu'il s'en sert, au clavier dès qu'il touche le clavier ou la
    // souris : on suit la dernière source utilisée.
    private void trackDevice() {
        if (!gamepad.connected()) {
            usingGamepad = false;
        } else if (gamepad.active()) {
            usingGamepad = true;
        } else if (Gdx.input.isKeyJustPressed(Keys.ANY_KEY) || Gdx.input.justTouched()
                || Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0) {
            usingGamepad = false;
        }
    }

    /**
     * La musique des menus, gardée d'un écran à l'autre. Chargée à la première demande : il faut
     * que le son de libGDX soit prêt.
     */
    public MenuMusic menuMusic() {
        if (menuMusic == null) {
            menuMusic = new MenuMusic();
        }
        return menuMusic;
    }

    /** @return la manette, lue une fois par image */
    public Gamepad gamepad() {
        return gamepad;
    }

    /** @return {@code true} si la dernière source utilisée par le joueur est la manette */
    public boolean usingGamepad() {
        return usingGamepad;
    }

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
        if (menuMusic != null) {
            menuMusic.dispose();
        }
    }

    private void disposeSuspended() {
        while (!suspended.isEmpty()) {
            suspended.pop().dispose();
        }
    }
}
