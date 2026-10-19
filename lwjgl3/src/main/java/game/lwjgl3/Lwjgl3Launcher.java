package game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import game.core.Main;

/**
 * Point d'entrée desktop : crée la fenêtre et lance le jeu.
 */
public final class Lwjgl3Launcher {

    private Lwjgl3Launcher() {
    }

    /**
     * Lance le jeu.
     *
     * @param args arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        // Sur macOS, relance la JVM avec -XstartOnFirstThread si nécessaire.
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        // La manette est lue avec GLFW, déjà présent dans le lanceur : le jeu ne dépend que de
        // l'abstraction Gamepad (module core).
        new Lwjgl3Application(new Main(new GlfwGamepad()), createConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration createConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Kagaribi");
        // Vsync : limite les images par seconde au rafraîchissement de l'écran, évite le déchirement.
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        // 1280x720 = 2 fois la résolution logique du jeu (640x360), en 16:9.
        configuration.setWindowedMode(1280, 720);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
