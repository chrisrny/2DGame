package game.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;

/**
 * La musique des menus (« feu de veille ») : écran titre, options ouvertes depuis le titre, et
 * écrans de fin, après le jingle de victoire ou de défaite.
 */
public final class MenuMusic implements Disposable {

    /** Volume de la musique des menus, un peu plus bas que celle de la partie. */
    private static final float VOLUME = 0.4f;

    /** Durées des fondus d'entrée et de sortie, en secondes. */
    private static final float FADE_IN = 2f;
    private static final float FADE_OUT = 0.6f;

    private final Music music;
    private boolean wanted;
    private float delay;
    private float level;

    /** Charge la musique des menus. */
    public MenuMusic() {
        music = Gdx.audio.newMusic(Gdx.files.internal("music/menu.ogg"));
        music.setLooping(true);
    }

    /**
     * Lance la musique, en fondu. Sans effet si elle joue déjà ou va jouer.
     *
     * @param after attente avant de commencer, en secondes (laisser finir un jingle)
     */
    public void play(float after) {
        if (wanted) {
            return;
        }
        wanted = true;
        delay = after;
    }

    /** Coupe la musique, en fondu rapide. */
    public void stop() {
        wanted = false;
        delay = 0f;
    }

    /**
     * Fait avancer les fondus. À appeler une fois par image.
     *
     * @param delta temps écoulé depuis l'image précédente, en secondes
     */
    public void update(float delta) {
        if (wanted && delay > 0f) {
            delay -= delta;
            return;
        }
        if (wanted && !music.isPlaying()) {
            level = 0f;
            music.setVolume(0f);
            music.play();
        }
        level = wanted ? Math.min(1f, level + delta / FADE_IN) : Math.max(0f, level - delta / FADE_OUT);
        if (music.isPlaying()) {
            music.setVolume(VOLUME * level);
            if (!wanted && level <= 0f) {
                music.stop();
            }
        }
    }

    @Override
    public void dispose() {
        music.stop();
        music.dispose();
    }
}
