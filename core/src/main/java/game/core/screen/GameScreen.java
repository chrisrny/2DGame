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
import game.core.hud.BossBar;
import game.core.hud.DamageNumbers;
import game.core.hud.Hud;
import game.core.hud.LevelUpOverlay;
import game.core.hud.StageBanner;
import game.core.input.InputSource;
import game.core.input.KeyboardMouseInput;
import game.core.render.SceneRenderer;
import game.core.render.WorldRenderer;
import game.model.commun.PlayerIntent;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.player.state.PlayerStateKind;
import game.model.skill.Shockwave;
import game.model.world.Run;
import java.util.List;

/**
 * Écran de partie.
 *
 * <p>À chaque image, il fait trois choses dans cet ordre : lire les entrées, faire avancer
 * le modèle à pas fixe, dessiner. L'écran ne décide jamais des règles : il transmet
 * l'intention du joueur au modèle et dessine ce que le modèle contient.</p>
 *
 * <p>Échap ouvre la pause, F9 et F10 gagnent ou perdent tout de suite (triche de démo pour la
 * soutenance). Quand la partie est finie, l'écran laisse {@link #END_DELAY}
 * secondes pour voir ce qui s'est passé, puis passe à l'écran de victoire ou de défaite.</p>
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

    /** Temps laissé après la fin de la partie avant l'écran de fin, en secondes. */
    public static final float END_DELAY = 1.5f;

    /** Durée de l'anneau dessiné quand l'Onde de choc part, en secondes (purement visuel). */
    private static final float SHOCKWAVE_VISUAL_DURATION = 0.2f;

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
    private final InputSource input = new KeyboardMouseInput(viewport);
    private final WorldRenderer worldRenderer = new WorldRenderer();
    private final Hud hud = new Hud(WORLD_WIDTH, WORLD_HEIGHT);
    private final LevelUpOverlay levelUp = new LevelUpOverlay(WORLD_WIDTH, WORLD_HEIGHT);
    private final StageBanner stageBanner = new StageBanner(WORLD_WIDTH, WORLD_HEIGHT);
    private final BossBar bossBar = new BossBar(WORLD_WIDTH, WORLD_HEIGHT);
    private final DamageNumbers damageNumbers;
    private final SceneRenderer scene;
    private float overFor;

    /**
     * Crée l'écran de partie.
     *
     * @param main le jeu, pour ouvrir la pause et l'écran de fin
     * @param run  la partie à jouer
     */
    public GameScreen(Main main, Run run) {
        this.main = main;
        this.run = run;
        this.damageNumbers = new DamageNumbers(run.events());
        this.scene = new SceneRenderer(run.events());
    }

    @Override
    public void resize(int width, int height) {
        // true : la caméra est recentrée sur le monde à chaque redimensionnement.
        viewport.update(width, height, true);
        hud.resize(width, height);
        levelUp.resize(width, height);
        stageBanner.resize(width, height);
        bossBar.resize(width, height);
    }

    @Override
    public void render(float delta) {
        // Pause : cet écran est gardé tel quel derrière l'écran de pause, et n'avance plus.
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            main.push(new PauseScreen(main, this));
            return;
        }

        // Triche de démo : F9 gagne, F10 perd tout de suite. C'est le modèle qui décide ; l'écran
        // de fin arrive ensuite après END_DELAY, comme pour une vraie fin de partie.
        if (Gdx.input.isKeyJustPressed(Keys.F9)) {
            run.forceWin();
        }
        if (Gdx.input.isKeyJustPressed(Keys.F10)) {
            run.forceLose();
        }

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
        worldRenderer.drawTelegraphs(shapes, run.map());
        drawShockwave();
        // Réglage des options : lu à chaque image, il prend effet dès la reprise de la partie.
        if (run.settings().showDamageNumbers()) {
            damageNumbers.draw(camera.combined);
        }

        // Le HUD en dernier, par-dessus le monde, avec sa propre caméra fixe.
        hud.draw(run.player(), run.experience());
        stageBanner.draw(run);
        bossBar.draw(run);
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
     * Zones de feu des reliques (Pas de braise) : des anneaux rouge orangé. On dessine des
     * contours et non des disques pleins, pour ne pas cacher les ennemis qui brûlent dedans.
     */
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
     * Clignotement pendant l'invincibilité après un coup : le joueur est caché une période
     * sur deux. La période est calculée à partir du temps d'invincibilité restant, donnée
     * par le modèle : la vue ne garde aucun minuteur à elle.
     *
     * @param player le joueur à dessiner
     * @return {@code true} si le joueur doit être dessiné estompé à cette image
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
        scene.dispose();
        hud.dispose();
        levelUp.dispose();
        damageNumbers.dispose();
        stageBanner.dispose();
        bossBar.dispose();
    }
}
