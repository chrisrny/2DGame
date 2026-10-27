package game.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.commun.Circle;
import game.model.commun.FixedStep;
import game.core.Main;
import game.core.audio.GameAudio;
import game.core.hud.BossBar;
import game.core.hud.DamageNumbers;
import game.core.hud.Hud;
import game.core.hud.LevelUpOverlay;
import game.core.hud.Minimap;
import game.core.hud.RunTimer;
import game.core.hud.StageBanner;
import game.core.input.CombinedInput;
import game.core.input.Gamepad;
import game.core.input.GamepadInput;
import game.core.input.InputSource;
import game.core.input.KeyboardMouseInput;
import game.core.render.SceneRenderer;
import game.core.render.WorldRenderer;
import game.model.commun.PlayerIntent;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;
import game.model.world.Run;
import java.util.ArrayList;
import java.util.List;

/** Écran de partie. */
public class GameScreen extends ScreenAdapter {

    /** Largeur du monde logique, en unités (= pixels de l'écran logique). */
    public static final float WORLD_WIDTH = 640f;

    /** Hauteur du monde logique, en unités. */
    public static final float WORLD_HEIGHT = 360f;

    /** Temps laissé après la fin de la partie avant l'écran de fin, en secondes. */
    public static final float END_DELAY = 1.5f;

    /** Durée d'une phase de clignotement pendant l'invincibilité, en secondes. */
    private static final float BLINK_PERIOD = 0.1f;

    /** Opacité du joueur pendant la phase « cachée » du clignotement. */
    private static final float BLINK_ALPHA = 0.3f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FixedStep clock = new FixedStep();
    private final Main main;
    private final Run run;
    private final InputSource input;
    private final GamepadInput gamepadInput;
    private final WorldRenderer worldRenderer = new WorldRenderer();
    private final Hud hud = new Hud(WORLD_WIDTH, WORLD_HEIGHT);
    private final LevelUpOverlay levelUp;
    private final StageBanner stageBanner = new StageBanner(WORLD_WIDTH, WORLD_HEIGHT);
    private final BossBar bossBar = new BossBar(WORLD_WIDTH, WORLD_HEIGHT);
    private final RunTimer runTimer = new RunTimer(WORLD_WIDTH, WORLD_HEIGHT);
    private final Minimap minimap;
    private final GameAudio audio;
    private final DamageNumbers damageNumbers;
    private final SceneRenderer scene;
    private float overFor;

    /**
     * Crée l'écran de partie.
     *
     * @param main le jeu, pour ouvrir la pause et l'écran de fin
     * @param run la partie à jouer
     */
    public GameScreen(Main main, Run run) {
        this.main = main;
        this.run = run;
        this.damageNumbers = new DamageNumbers(run.events());
        this.scene = new SceneRenderer(run.events(), WORLD_WIDTH, WORLD_HEIGHT);
        this.audio = new GameAudio(run.events());
        // Clavier et manette en même temps : on passe de l'un à l'autre sans rien régler.
        this.gamepadInput = new GamepadInput(main.gamepad(), this::enemyPositions);
        this.input = new CombinedInput(new KeyboardMouseInput(viewport), gamepadInput, main::usingGamepad);
        this.levelUp = new LevelUpOverlay(WORLD_WIDTH, WORLD_HEIGHT, main.gamepad(), main::usingGamepad);
        this.minimap = new Minimap(run.map(), WORLD_WIDTH, WORLD_HEIGHT);
    }

    // Positions des ennemis en vie, pour l'aide à la visée de la manette.
    private List<Vec2> enemyPositions() {
        List<Vec2> positions = new ArrayList<>();
        for (Enemy enemy : run.map().enemies()) {
            if (enemy.isAlive()) {
                positions.add(enemy.position());
            }
        }
        return positions;
    }

    @Override
    public void show() {
        // La partie a sa propre musique : celle des menus s'efface.
        main.menuMusic().stop();
        // En revenant de la pause, le A qui a validé « Reprendre » ne doit pas faire esquiver.
        gamepadInput.ignoreUntilReleased();
    }

    @Override
    public void resize(int width, int height) {
        // true : la caméra est recentrée sur le monde à chaque redimensionnement.
        viewport.update(width, height, true);
        hud.resize(width, height);
        levelUp.resize(width, height);
        stageBanner.resize(width, height);
        bossBar.resize(width, height);
        runTimer.resize(width, height);
        minimap.resize(width, height);
    }

    @Override
    public void render(float delta) {
        // Pause : cet écran est gardé tel quel derrière l'écran de pause, et n'avance plus.
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || main.gamepad().justPressed(Gamepad.Button.START)) {
            main.push(new PauseScreen(main, this));
            return;
        }

        // Mini-carte : M au clavier, Back à la manette, pour la masquer ou la réafficher.
        if (Gdx.input.isKeyJustPressed(Keys.M) || main.gamepad().justPressed(Gamepad.Button.BACK)) {
            minimap.toggle();
        }

        // Triche de démo : F9 gagne, F10 perd tout de suite.
        if (Gdx.input.isKeyJustPressed(Keys.F9)) {
            run.forceWin();
        }
        if (Gdx.input.isKeyJustPressed(Keys.F10)) {
            run.forceLose();
        }

        // Le son suit la partie à chaque image, même figée (montée de niveau, fin de partie).
        audio.update(run, delta);

        // Montée de niveau : la partie est figée tant que le joueur n'a pas choisi sa relique.
        if (run.isChoosingRelic()) {
            int choice = levelUp.readChoice(run.pendingChoice().size());
            if (choice >= 0) {
                run.chooseRelic(choice);
                // Le A qui a validé la carte ne doit pas faire esquiver à la reprise.
                gamepadInput.ignoreUntilReleased();
            }
            draw();
            return;
        }

        // 1.
        PlayerIntent intent = input.poll(run.player().position());

        // 2.
        int steps = scene.frozen() ? 0 : clock.advance(delta);
        for (int i = 0; i < steps; i++) {
            // Positions avant le pas : la vue dessine entre elles et les nouvelles (interpolation),
            // pour que le mouvement reste fluide quand l'écran affiche plus de 60 images par
            // seconde.
            scene.beforeStep(run);
            run.update(intent, FixedStep.DT);
        }
        scene.setStepProgress(clock.pending() / FixedStep.DT);
        // Les chiffres de dégâts avancent avec le temps réel : c'est purement visuel.
        damageNumbers.update(delta);
        scene.update(run, delta);

        // Fin de partie : on laisse un instant pour voir la mort du joueur ou du boss.
        if (run.isOver()) {
            overFor += delta;
            if (overFor >= END_DELAY) {
                main.showEnd(run);
                return;
            }
        }

        // 3. Dessin.
        draw();
    }

    /**
     * Dessine la partie sans la faire avancer : sert à l'écran de pause, qui l'affiche figée
     * derrière son menu.
     */
    void drawFrozen() {
        draw();
    }

    private void draw() {
        // Noir partout : ce qui reste visible hors du monde forme les bandes noires.
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        followPlayer();
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);

        // Fausse 3D : le sol, le feu posé au sol, puis murs et personnages triés par profondeur.
        scene.drawGround(camera, run, WORLD_WIDTH, WORLD_HEIGHT);
        drawFireZones();
        Player player = run.player();
        scene.drawObjects(camera, run, blinkHidden(player) ? BLINK_ALPHA : 1f);
        // La nuit et ses lumières, puis ce qui reste vif : annonces, chiffres, HUD.
        scene.drawLight(camera, viewport, run);
        worldRenderer.drawTelegraphs(shapes, run.map());
        // Réglage des options : lu à chaque image, il prend effet dès la reprise de la partie.
        if (run.settings().showDamageNumbers()) {
            damageNumbers.draw(camera.combined);
        }

        // Le HUD en dernier, par-dessus le monde, avec sa propre caméra fixe.
        hud.setGamepadLabels(main.usingGamepad());
        hud.draw(run.player(), run.experience());
        stageBanner.draw(run);
        minimap.draw(run, new Vec2(camera.position.x, camera.position.y));
        bossBar.draw(run);
        runTimer.draw(run);
        if (run.isChoosingRelic()) {
            levelUp.draw(run.pendingChoice());
        }
    }

    /** Centre la caméra sur le joueur, sans jamais montrer l'extérieur de la map. */
    private void followPlayer() {
        Rect arena = run.map().arena();
        Vec2 target = scene.playerOnScreen(run);
        float x = Math.clamp(target.x(), arena.x() + WORLD_WIDTH / 2f, arena.right() - WORLD_WIDTH / 2f);
        float y = Math.clamp(target.y(), arena.y() + WORLD_HEIGHT / 2f, arena.top() - WORLD_HEIGHT / 2f);
        // Tremblement d'écran des gros coups du boss : purement visuel, ajouté après le cadrage.
        Vec2 shake = scene.cameraShake();
        camera.position.set(x + shake.x(), y + shake.y(), 0f);
        camera.update();
    }

    /** Zones de feu des reliques (Pas de braise) : des anneaux rouge orangé. */
    private void drawFireZones() {
        List<Circle> zones = run.player().fireZones();
        if (zones.isEmpty()) {
            return;
        }
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.30f, 0.10f, 1f);
        for (Circle zone : zones) {
            shapes.circle(zone.center().x(), zone.center().y(), zone.radius(), 20);
            shapes.circle(zone.center().x(), zone.center().y(), zone.radius() - 2f, 20);
        }
        shapes.end();
    }

    /**
     * Clignotement pendant l'invincibilité après un coup : le joueur est caché une période sur
     * deux.
     */
    private static boolean blinkHidden(Player player) {
        float left = player.invulnerableLeft();
        if (left <= 0f || player.stateKind() == PlayerStateKind.HURT) {
            return false;
        }
        return (int) (left / BLINK_PERIOD) % 2 == 1;
    }

    @Override
    public void dispose() {
        shapes.dispose();
        scene.dispose();
        hud.dispose();
        levelUp.dispose();
        damageNumbers.dispose();
        stageBanner.dispose();
        bossBar.dispose();
        runTimer.dispose();
        minimap.dispose();
        audio.dispose();
    }
}
