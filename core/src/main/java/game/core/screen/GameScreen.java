package game.core.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.core.input.InputSource;
import game.core.input.KeyboardMouseInput;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.Run;

/**
 * Écran de partie.
 *
 * <p>À chaque image, il fait trois choses dans cet ordre : lire les entrées, faire avancer
 * le modèle à pas fixe, dessiner. L'écran ne décide jamais des règles : il transmet
 * l'intention du joueur au modèle et dessine ce que le modèle contient.</p>
 *
 * <p>Le jeu est dessiné dans un monde logique de 640x360. Le {@link FitViewport} l'agrandit
 * à la taille de la fenêtre en gardant le format 16:9, avec des bandes noires si la fenêtre
 * n'a pas les bonnes proportions.</p>
 */
public class GameScreen extends ScreenAdapter {

    /** Largeur du monde logique, en unités (= pixels de l'écran logique). */
    public static final float WORLD_WIDTH = 640f;

    /** Hauteur du monde logique, en unités. */
    public static final float WORLD_HEIGHT = 360f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FixedStep clock = new FixedStep();
    private final Run run = new Run(GameConfig.defaults());
    private final InputSource input = new KeyboardMouseInput(viewport);

    private PlayerIntent lastIntent = PlayerIntent.NONE;

    @Override
    public void resize(int width, int height) {
        // true : la caméra est recentrée sur le monde à chaque redimensionnement.
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        // 1. Entrées : lues une fois par image, puis données à chaque pas de modèle.
        // La visée part de la position du joueur.
        PlayerIntent intent = input.poll(run.player().position());
        lastIntent = intent;

        // 2. Modèle : 0, 1 ou plusieurs pas de durée fixe selon le temps écoulé.
        int steps = clock.advance(delta);
        for (int i = 0; i < steps; i++) {
            run.update(intent, FixedStep.DT);
        }

        // 3. Dessin.
        draw();
    }

    private void draw() {
        // Noir partout : ce qui reste visible hors du monde forme les bandes noires.
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Fond sombre du donjon, sur toute la surface du monde logique.
        shapes.setColor(0.07f, 0.06f, 0.09f, 1f);
        shapes.rect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        // Joueur : un disque bleu de la taille de sa zone de collision. Il devient blanc pendant l'attaque.
        Vec2 player = run.player().position();
        if (lastIntent.attack()) {
            shapes.setColor(1f, 1f, 1f, 1f);
        } else {
            shapes.setColor(0.30f, 0.55f, 0.95f, 1f);
        }
        shapes.circle(player.x(), player.y(), Player.RADIUS, 24);
        shapes.end();

        drawIntentDebug();
    }

    /**
     * Témoin temporaire des entrées, depuis le joueur : un trait jaune vers la souris (visée)
     * et un trait vert dans la direction de déplacement. À retirer avec les états du joueur (P1-A2).
     */
    private void drawIntentDebug() {
        Vec2 player = run.player().position();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        Vec2 aimEnd = player.add(lastIntent.aim().scale(28f));
        shapes.setColor(1f, 0.85f, 0.2f, 1f);
        shapes.line(player.x(), player.y(), aimEnd.x(), aimEnd.y());
        Vec2 moveEnd = player.add(lastIntent.move().scale(20f));
        shapes.setColor(0.3f, 0.9f, 0.4f, 1f);
        shapes.line(player.x(), player.y(), moveEnd.x(), moveEnd.y());
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
