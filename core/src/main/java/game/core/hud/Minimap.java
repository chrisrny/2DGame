package game.core.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Boss;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.world.GameMap;
import game.model.world.MapLayout;
import game.model.world.MapLayouts;
import game.model.world.Run;
import java.util.Objects;

/**
 * Mini-carte, en haut à droite sous la plaque de l'étage : toute la map en petit, le héros, les
 * ennemis et la partie de la map visible à l'écran.
 *
 * <p>Le décor (maisons, murs, canaux, ponts, torii) ne bouge pas : il est dessiné une seule fois,
 * à la création, dans une texture. À chaque image, il ne reste qu'à poser des points par-dessus.
 * Elle se masque et se réaffiche avec M au clavier, Back à la manette.</p>
 *
 * <p>Purement visuel : la mini-carte lit le modèle et ne décide de rien.</p>
 */
public final class Minimap implements Disposable {

    /** Taille de la mini-carte à l'écran, en unités : la map en 16:9, réduite 15 fois. */
    public static final float WIDTH = 128f;
    public static final float HEIGHT = 72f;

    private static final float MARGIN = 8f;
    /** Place laissée au-dessus pour la plaque de l'étage. */
    private static final float TOP_OFFSET = 34f;
    /** La texture du décor a deux pixels par unité : l'écran de jeu est en 2x. */
    private static final int TEXELS = 2;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final UiAtlas ui = new UiAtlas();
    private final Texture terrain;
    private final Rect arena;
    private final Rect bossArena;
    private final float viewW;
    private final float viewH;
    private boolean visible = true;
    private float time;

    /**
     * Prépare la mini-carte d'une map.
     *
     * @param map    la map de la partie
     * @param width  largeur de l'écran logique, en unités
     * @param height hauteur de l'écran logique, en unités
     */
    public Minimap(GameMap map, float width, float height) {
        Objects.requireNonNull(map, "map");
        this.viewport = new FitViewport(width, height, camera);
        this.viewW = width;
        this.viewH = height;
        MapLayout layout = map.layout();
        this.arena = layout.arena();
        this.bossArena = layout.bossArena();
        this.terrain = paintTerrain(layout);
    }

    // Le décor, peint une fois : sol, murs et maisons, eau, ponts, torii.
    private Texture paintTerrain(MapLayout layout) {
        int w = (int) (WIDTH * TEXELS);
        int h = (int) (HEIGHT * TEXELS);
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.27f, 0.21f, 0.23f, 1f);                       // pavés
        pixmap.fill();
        for (Rect wall : layout.walls()) {
            if (layout.water().contains(wall)) {
                pixmap.setColor(0.16f, 0.36f, 0.52f, 1f);              // eau
            } else if (MapLayouts.TORII.contains(wall)) {
                pixmap.setColor(0.78f, 0.2f, 0.16f, 1f);               // piliers de torii
            } else {
                pixmap.setColor(0.06f, 0.04f, 0.06f, 1f);              // maisons, murs, rochers
            }
            fill(pixmap, wall, w, h);
        }
        pixmap.setColor(0.42f, 0.28f, 0.18f, 1f);                      // ponts de bois
        for (Rect bridge : MapLayouts.BRIDGES) {
            fill(pixmap, bridge, w, h);
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    // Un rectangle du monde dans la texture (y vers le bas dans un Pixmap, vers le haut dans le monde).
    private void fill(Pixmap pixmap, Rect r, int w, int h) {
        float kx = w / arena.width();
        float ky = h / arena.height();
        int x0 = Math.round((r.x() - arena.x()) * kx);
        int x1 = Math.round((r.right() - arena.x()) * kx);
        int y0 = Math.round((r.top() - arena.y()) * ky);
        int y1 = Math.round((r.y() - arena.y()) * ky);
        pixmap.fillRectangle(x0, h - y0, Math.max(1, x1 - x0), Math.max(1, y0 - y1));
    }

    /** Masque ou réaffiche la mini-carte. */
    public void toggle() {
        visible = !visible;
    }

    /** @return {@code true} si la mini-carte est affichée */
    public boolean visible() {
        return visible;
    }

    /**
     * Adapte la mini-carte à la nouvelle taille de la fenêtre.
     *
     * @param width  largeur de la fenêtre, en pixels
     * @param height hauteur de la fenêtre, en pixels
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Dessine la mini-carte.
     *
     * @param run    la partie
     * @param camera le centre de la caméra du jeu, pour le cadre de ce qui est visible
     */
    public void draw(Run run, Vec2 camera) {
        time += Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        if (!visible) {
            return;
        }
        viewport.apply();
        float x = viewport.getWorldWidth() - MARGIN - WIDTH;
        float y = viewport.getWorldHeight() - TOP_OFFSET - HEIGHT;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setProjectionMatrix(this.camera.combined);
        batch.begin();

        // Cadre : un liseré doré autour d'un fond laqué, comme le reste du HUD.
        batch.setColor(0.02f, 0.01f, 0.02f, 0.85f);
        batch.draw(ui.white, x - 3f, y - 3f, WIDTH + 6f, HEIGHT + 6f);
        batch.setColor(0.86f, 0.66f, 0.3f, 1f);
        outline(x - 2f, y - 2f, WIDTH + 4f, HEIGHT + 4f, 1f);
        batch.setColor(1f, 1f, 1f, 0.92f);
        batch.draw(terrain, x, y, WIDTH, HEIGHT);

        // Étage du boss : son arène s'allume.
        if (run.isBossStage()) {
            float pulse = 0.55f + 0.35f * (float) Math.sin(time * 4.0);
            batch.setColor(1f, 0.3f, 0.2f, pulse);
            Vec2 a = toMap(new Vec2(bossArena.x(), bossArena.y()), x, y);
            outline(a.x(), a.y(), bossArena.width() * scaleX(), bossArena.height() * scaleY(), 1f);
        }

        // Ce qui est visible à l'écran.
        batch.setColor(1f, 1f, 1f, 0.45f);
        float vw = viewW * scaleX();
        float vh = viewH * scaleY();
        Vec2 c = toMap(camera, x, y);
        outline(c.x() - vw / 2f, c.y() - vh / 2f, vw, vh, 0.5f);

        for (Enemy enemy : run.map().enemies()) {
            if (enemy.isAlive()) {
                drawEnemy(enemy, x, y);
            }
        }
        drawHero(run.player(), x, y);

        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawEnemy(Enemy enemy, float x, float y) {
        Vec2 p = toMap(enemy.position(), x, y);
        if (enemy instanceof Boss) {
            float pulse = 1f + 0.3f * (float) Math.sin(time * 6.0);
            float size = 6f * pulse;
            batch.setColor(1f, 0.25f, 0.15f, 0.35f);
            batch.draw(ui.glow, p.x() - size * 1.5f, p.y() - size * 1.5f, size * 3f, size * 3f);
            batch.setColor(1f, 0.2f, 0.12f, 1f);
            dot(p, size);
            return;
        }
        if (enemy instanceof Archer) {
            batch.setColor(1f, 0.62f, 0.2f, 1f);
            dot(p, 2.5f);
        } else if (enemy instanceof Charger) {
            batch.setColor(1f, 0.38f, 0.1f, 1f);
            dot(p, 3.5f);
        } else {
            batch.setColor(0.92f, 0.22f, 0.2f, 1f);
            dot(p, 2.5f);
        }
    }

    // Le héros : un point doré cerclé de sombre, et un trait vers où il vise.
    private void drawHero(Player player, float x, float y) {
        Vec2 p = toMap(player.position(), x, y);
        Vec2 f = player.facing();
        float angle = (float) Math.toDegrees(Math.atan2(f.y(), f.x()));
        batch.setColor(1f, 0.9f, 0.55f, 0.9f);
        batch.draw(ui.white, p.x(), p.y() - 0.5f, 0f, 0.5f, 5f, 1f, 1f, 1f, angle);
        batch.setColor(0.02f, 0.01f, 0.02f, 1f);
        dot(p, 5f);
        batch.setColor(1f, 0.85f, 0.35f, 1f);
        dot(p, 3.5f);
    }

    private void dot(Vec2 p, float size) {
        batch.draw(ui.white, p.x() - size / 2f, p.y() - size / 2f, size, size);
    }

    private void outline(float x, float y, float w, float h, float t) {
        batch.draw(ui.white, x, y, w, t);
        batch.draw(ui.white, x, y + h - t, w, t);
        batch.draw(ui.white, x, y, t, h);
        batch.draw(ui.white, x + w - t, y, t, h);
    }

    private float scaleX() {
        return WIDTH / arena.width();
    }

    private float scaleY() {
        return HEIGHT / arena.height();
    }

    // Une position du monde sur la mini-carte, gardée dans le cadre.
    private Vec2 toMap(Vec2 world, float x, float y) {
        float mx = x + Math.clamp((world.x() - arena.x()) * scaleX(), 0f, WIDTH);
        float my = y + Math.clamp((world.y() - arena.y()) * scaleY(), 0f, HEIGHT);
        return new Vec2(mx, my);
    }

    @Override
    public void dispose() {
        batch.dispose();
        ui.dispose();
        terrain.dispose();
    }
}
