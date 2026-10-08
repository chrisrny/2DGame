package game.core.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.commun.Circle;
import game.model.commun.FixedStep;
import game.model.commun.GameConfig;
import game.core.input.InputSource;
import game.core.input.KeyboardMouseInput;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;
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
        drawPlayer();
        shapes.end();
    }

    /**
     * Joueur : un disque de la taille de sa zone de collision, coloré selon son état,
     * et un trait qui montre la direction du regard. Pendant une attaque, la zone de coup
     * est dessinée devant lui.
     */
    private void drawPlayer() {
        Player player = run.player();
        Vec2 center = player.position();
        PlayerStateKind kind = player.stateKind();
        boolean dead = kind == PlayerStateKind.DEAD;
        if (kind == PlayerStateKind.ATTACKING) {
            Circle zone = player.attackZone();
            shapes.setColor(0.85f, 0.85f, 0.95f, 1f);
            shapes.circle(zone.center().x(), zone.center().y(), zone.radius(), 20);
        }
        if (dead) {
            shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
        } else if (kind == PlayerStateKind.ATTACKING) {
            shapes.setColor(1f, 1f, 1f, 1f);
        } else {
            shapes.setColor(0.30f, 0.55f, 0.95f, 1f);
        }
        shapes.circle(center.x(), center.y(), Player.RADIUS, 24);
        if (!dead) {
            Vec2 tip = center.add(player.facing().scale(Player.RADIUS + 6f));
            shapes.setColor(1f, 0.85f, 0.2f, 1f);
            shapes.rectLine(center.x(), center.y(), tip.x(), tip.y(), 2f);
        }
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
