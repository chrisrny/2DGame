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
import game.core.hud.DamageNumbers;
import game.core.hud.Hud;
import game.core.hud.LevelUpOverlay;
import game.core.input.InputSource;
import game.core.input.KeyboardMouseInput;
import game.core.render.WorldRenderer;
import game.model.commun.PlayerIntent;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;
import game.model.skill.Shockwave;
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

    /** Durée de l'anneau dessiné quand l'Onde de choc part, en secondes (purement visuel). */
    private static final float SHOCKWAVE_VISUAL_DURATION = 0.2f;

    /** Durée d'une phase de clignotement pendant l'invincibilité, en secondes. */
    private static final float BLINK_PERIOD = 0.1f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FixedStep clock = new FixedStep();
    private final Run run = new Run(GameConfig.defaults());
    private final InputSource input = new KeyboardMouseInput(viewport);
    private final WorldRenderer worldRenderer = new WorldRenderer();
    private final Hud hud = new Hud(WORLD_WIDTH, WORLD_HEIGHT);
    private final LevelUpOverlay levelUp = new LevelUpOverlay(WORLD_WIDTH, WORLD_HEIGHT);
    private final DamageNumbers damageNumbers = new DamageNumbers(run.events());


    @Override
    public void resize(int width, int height) {
        // true : la caméra est recentrée sur le monde à chaque redimensionnement.
        viewport.update(width, height, true);
        hud.resize(width, height);
        levelUp.resize(width, height);
    }

    @Override
    public void render(float delta) {
        // Montée de niveau : la partie est figée tant que le joueur n'a pas choisi sa relique.
        // On ne fait pas avancer l'horloge, pour ne pas rattraper ce temps d'un coup ensuite.
        if (run.isChoosingRelic()) {
            int choice = levelUp.readChoice(run.pendingChoice().size());
            if (choice >= 0) {
                run.chooseRelic(choice);
            }
            draw();
            return;
        }

        // 1. Entrées : lues une fois par image, puis données à chaque pas de modèle.
        // La visée part de la position du joueur.
        PlayerIntent intent = input.poll(run.player().position());

        // 2. Modèle : 0, 1 ou plusieurs pas de durée fixe selon le temps écoulé.
        int steps = clock.advance(delta);
        for (int i = 0; i < steps; i++) {
            run.update(intent, FixedStep.DT);
        }
        // Les chiffres de dégâts avancent avec le temps réel : c'est purement visuel.
        // Ils sont figés pendant le choix de relique, comme le reste (retour plus haut).
        damageNumbers.update(delta);

        // 3. Dessin.
        draw();
    }

    private void draw() {
        // Noir partout : ce qui reste visible hors du monde forme les bandes noires.
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        followPlayer();
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        worldRenderer.draw(shapes, run.map());
        drawPlayer();
        shapes.end();
        drawShockwave();
        damageNumbers.draw(camera.combined);

        // Le HUD en dernier, par-dessus le monde, avec sa propre caméra fixe.
        hud.draw(run.player(), run.experience());
        if (run.isChoosingRelic()) {
            levelUp.draw(run.pendingChoice());
        }
    }

    /**
     * Centre la caméra sur le joueur, sans jamais montrer l'extérieur de la map.
     */
    private void followPlayer() {
        Rect arena = run.map().arena();
        Vec2 target = run.player().position();
        float x = Math.clamp(target.x(), arena.x() + WORLD_WIDTH / 2f, arena.right() - WORLD_WIDTH / 2f);
        float y = Math.clamp(target.y(), arena.y() + WORLD_HEIGHT / 2f, arena.top() - WORLD_HEIGHT / 2f);
        camera.position.set(x, y, 0f);
        camera.update();
    }

    /**
     * Joueur : un disque de la taille de sa zone de collision, coloré selon son état,
     * et un trait qui montre la direction du regard. Pendant une attaque, la zone de coup
     * est dessinée devant lui. Juste après un coup reçu, il est rouge, puis il clignote
     * tant qu'il est invincible.
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
        } else if (kind == PlayerStateKind.HURT) {
            shapes.setColor(0.95f, 0.25f, 0.25f, 1f);
        } else if (kind == PlayerStateKind.ATTACKING) {
            shapes.setColor(1f, 1f, 1f, 1f);
        } else if (kind == PlayerStateKind.DODGING) {
            shapes.setColor(0.35f, 0.95f, 0.95f, 1f);
        } else {
            shapes.setColor(0.30f, 0.55f, 0.95f, 1f);
        }
        if (!blinkHidden(player)) {
            shapes.circle(center.x(), center.y(), Player.RADIUS, 24);
        }
        if (!dead) {
            Vec2 tip = center.add(player.facing().scale(Player.RADIUS + 6f));
            shapes.setColor(1f, 0.85f, 0.2f, 1f);
            shapes.rectLine(center.x(), center.y(), tip.x(), tip.y(), 2f);
        }
    }

    /**
     * Clignotement pendant l'invincibilité après un coup : le joueur est caché une période
     * sur deux. La période est calculée à partir du temps d'invincibilité restant, donnée
     * par le modèle : la vue ne garde aucun minuteur à elle.
     *
     * @param player le joueur à dessiner
     * @return {@code true} si le corps du joueur ne doit pas être dessiné à cette image
     */
    private static boolean blinkHidden(Player player) {
        float left = player.invulnerableLeft();
        if (left <= 0f || player.stateKind() == PlayerStateKind.HURT) {
            return false;
        }
        return (int) (left / BLINK_PERIOD) % 2 == 1;
    }

    /**
     * Anneau de l'Onde de choc : il grandit de 0 au rayon de l'onde juste après son utilisation.
     * La vue lit le temps écoulé depuis l'utilisation, gardé par la compétence : le modèle n'a
     * pas besoin d'un événement de plus.
     */
    private void drawShockwave() {
        float age = run.player().skill1().timeSinceActivation();
        if (age >= SHOCKWAVE_VISUAL_DURATION) {
            return;
        }
        Vec2 center = run.player().position();
        float radius = Shockwave.RADIUS * (age / SHOCKWAVE_VISUAL_DURATION);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.45f, 0.75f, 1f, 1f);
        shapes.circle(center.x(), center.y(), Math.max(1f, radius), 40);
        shapes.circle(center.x(), center.y(), Math.max(1f, radius - 2f), 40);
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        hud.dispose();
        levelUp.dispose();
        damageNumbers.dispose();
    }
}
