package game.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.MapLayouts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Dessine le décor du quartier des plaisirs en flammes, en fausse 3D (vue 3/4) : dalles de pierre,
 * maisons de thé, murs de clôture, lanternes de pierre, feux et traces de brûlure
 * ({@code assets/sprites/floor.png} et {@code assets/sprites/tiles.png}).
 *
 * <p>La forme de la map ne change pas : chaque mur du modèle garde son emprise de collision, et la
 * vue l'habille selon sa forme. Un petit carré devient une lanterne de pierre, un mur fin (24
 * unités ou moins, dont les bordures sud, est et ouest) un mur de clôture coiffé de tuiles, et le
 * reste (gros blocs, bordure nord) une maison à façade haute, à claire-voie et guirlande de
 * lanternes rouges.</p>
 *
 * <p>À l'écran, le dessus d'un mur est remonté de la hauteur de sa façade : il paraît avoir un
 * volume. Le tri par profondeur ({@link SceneRenderer}) fait le reste. Quand le héros passe
 * derrière un mur qui le cacherait, ce mur devient transparent.</p>
 *
 * <p>Quelques obstacles ont un rôle nommé dans {@link MapLayouts} : les canaux (de l'eau, au
 * niveau du sol, enjambée par des ponts) et les piliers des torii (un portique vermillon, dont le
 * linteau passe au-dessus des personnages).</p>
 *
 * <p>Les feux, lanternes et fenêtres sont aussi des sources de lumière pour
 * {@link LightingRenderer} ({@link #lights()}).</p>
 */
public final class MapRenderer implements Disposable {

    /** Hauteur visible de la façade d'un mur de clôture, en unités. */
    public static final float WALL_FRONT = 12f;

    /** Hauteur visible de la façade d'une maison, en unités. */
    public static final float HOUSE_FRONT = 30f;

    /** Taille de la texture de sol, qui se répète, en pixels (et en unités). */
    private static final int FLOOR_SIZE = 256;

    /** Épaisseur de l'ombre que projette un mur au sol, en unités. */
    private static final float WALL_SHADOW = 6f;

    /** Opacité d'un mur qui cache le héros. */
    private static final float XRAY_ALPHA = 0.45f;

    /** Durée d'une image des flammes, en secondes. */
    private static final float FIRE_FRAME = 0.1f;

    // Largeur d'une travée de façade et espacement des lanternes rouges.
    private static final int BAY = 16;
    private static final int GARLAND_STEP = 15;

    // Nombre de feux de débris et de traces de brûlure posés au hasard (toujours les mêmes).
    private static final int DEBRIS_FIRES = 14;
    private static final int SCORCHES = 60;

    /** Les façons d'habiller un mur. */
    enum Kind {
        HOUSE, FENCE, LANTERN, CANAL, TORII
    }

    /** Durée d'une image de l'eau des canaux, en secondes. */
    private static final float WATER_FRAME = 0.15f;

    // Débord du linteau du torii de chaque côté de ses piliers, en unités.
    private static final float BEAM_OVERHANG = 14f;

    private final Texture tiles;
    private final Texture floor;
    private final Texture fx;
    private final TextureRegion[] bays = new TextureRegion[6];
    private final TextureRegion roof;
    private final TextureRegion plaster;
    private final TextureRegion fenceFace;
    private final TextureRegion capH;
    private final TextureRegion capV;
    private final TextureRegion redLantern;
    private final TextureRegion stoneLantern;
    private final TextureRegion[] fire = new TextureRegion[4];
    private final TextureRegion scorch;
    private final TextureRegion[] water = new TextureRegion[4];
    private final TextureRegion bank;
    private final TextureRegion planks;
    private final TextureRegion railing;
    private final TextureRegion beam;
    private final TextureRegion pillar;
    private final TextureRegion white;
    private final TextureRegion shadow;

    // Décor calculé une fois par map.
    private List<Rect> preparedFor;
    private final Map<Rect, Kind> kinds = new HashMap<>();
    private final List<Vec2> groundFires = new ArrayList<>();
    private final Map<Rect, List<Vec2>> roofFires = new HashMap<>();
    private final List<Vec2> scorches = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    // Pour chaque pilier gauche d'un torii, son pilier droit.
    private final Map<Rect, Rect> toriiPairs = new HashMap<>();
    private final List<Rect> canals = new ArrayList<>();

    /**
     * Charge le sol et l'atlas du décor.
     *
     * @param fx la planche d'effets, pour son pixel blanc et son ombre ronde
     */
    public MapRenderer(Texture fx) {
        this.fx = fx;
        tiles = new Texture(Gdx.files.internal("sprites/tiles.png"));
        tiles.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        floor = new Texture(Gdx.files.internal("sprites/floor.png"));
        floor.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        // Le sol se répète à l'infini : une seule texture couvre toute la map.
        floor.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        for (int i = 0; i < bays.length; i++) {
            bays[i] = new TextureRegion(tiles, i * BAY, 0, BAY, (int) HOUSE_FRONT);
        }
        roof = new TextureRegion(tiles, 96, 0, 30, 30);
        plaster = new TextureRegion(tiles, 128, 0, 16, 16);
        fenceFace = new TextureRegion(tiles, 0, 32, 24, 12);
        capH = new TextureRegion(tiles, 24, 32, 16, 13);
        capV = new TextureRegion(tiles, 40, 32, 13, 16);
        redLantern = new TextureRegion(tiles, 64, 32, 7, 9);
        stoneLantern = new TextureRegion(tiles, 0, 64, 24, 44);
        for (int i = 0; i < fire.length; i++) {
            fire[i] = new TextureRegion(tiles, 32 + i * 32, 64, 32, 24);
        }
        scorch = new TextureRegion(tiles, 0, 112, 32, 16);
        for (int i = 0; i < water.length; i++) {
            water[i] = new TextureRegion(tiles, 160 + i * 16, 64, 16, 16);
        }
        bank = new TextureRegion(tiles, 224, 64, 16, 8);
        planks = new TextureRegion(tiles, 160, 80, 16, 16);
        railing = new TextureRegion(tiles, 176, 80, 16, 8);
        beam = new TextureRegion(tiles, 192, 80, 16, 14);
        pillar = new TextureRegion(tiles, 240, 64, 12, 48);
        white = new TextureRegion(fx, 62, 0, 1, 1);
        shadow = new TextureRegion(fx, 0, 4, 16, 8);
    }

    // ------------------------------------------------------------------ préparation

    /**
     * Classe les murs et pose le décor (feux, brûlures, lumières), une seule fois par map.
     *
     * @param arena les limites de la map
     * @param walls les murs du modèle
     */
    public void prepare(Rect arena, List<Rect> walls) {
        if (walls == preparedFor) {
            return;
        }
        preparedFor = walls;
        kinds.clear();
        groundFires.clear();
        roofFires.clear();
        scorches.clear();
        lights.clear();
        toriiPairs.clear();
        canals.clear();
        for (Rect wall : walls) {
            Kind kind = classify(wall, arena);
            kinds.put(wall, kind);
            switch (kind) {
                case LANTERN -> lights.add(Light.flame(wall.x() + wall.width() / 2f, lanternBase(wall) + 28f, 95f, 1f));
                case HOUSE -> decorateHouse(wall, arena, walls);
                case CANAL -> canals.add(wall);
                case FENCE, TORII -> { }
            }
        }
        pairTorii(walls);
        // Petites lanternes rouges aux quatre coins de chaque pont.
        if (!canals.isEmpty()) {
            for (Rect bridge : MapLayouts.BRIDGES) {
                for (float x : new float[] {bridge.x(), bridge.right()}) {
                    for (float y : new float[] {bridge.y(), bridge.top()}) {
                        lights.add(new Light(x, y + 6f, 34f, 1f, 0.35f, 0.25f, 0.55f, 0.06f, x + y));
                    }
                }
            }
        }
        Random random = new Random(7L);
        int target = groundFires.size() + DEBRIS_FIRES;
        for (int tries = 0; groundFires.size() < target && tries < 500; tries++) {
            Vec2 p = new Vec2(arena.x() + random.nextFloat() * arena.width(), arena.y() + random.nextFloat() * arena.height());
            if (clear(p, walls, 24f)) {
                addGroundFire(p);
            }
        }
        for (int tries = 0; scorches.size() < SCORCHES && tries < 500; tries++) {
            Vec2 p = new Vec2(arena.x() + random.nextFloat() * arena.width(), arena.y() + random.nextFloat() * arena.height());
            if (clear(p, walls, 12f)) {
                scorches.add(p);
            }
        }
    }

    // La bordure nord est une rangée de maisons dont on voit la façade ; les autres bordures, vues
    // de dos ou de côté, sont des murs d'enceinte bas, pour ne pas cacher le bas de l'écran.
    static Kind classify(Rect wall, Rect arena) {
        if (MapLayouts.CANALS.contains(wall)) {
            return Kind.CANAL;
        }
        if (MapLayouts.TORII.contains(wall)) {
            return Kind.TORII;
        }
        if (wall.width() <= 32f && wall.height() <= 32f) {
            return Kind.LANTERN;
        }
        if (wall.top() >= arena.top()) {
            return Kind.HOUSE;
        }
        if (Math.min(wall.width(), wall.height()) <= 24f) {
            return Kind.FENCE;
        }
        return Kind.HOUSE;
    }

    private void decorateHouse(Rect wall, Rect arena, List<Rect> walls) {
        float face = wall.y() + HOUSE_FRONT;
        // Fenêtres éclairées et lanternes rouges : des lumières le long de la façade.
        for (float x = wall.x(); x < wall.right(); x += BAY) {
            if (bayKind(wall, x) == 0) {
                lights.add(Light.steady(x + BAY / 2f, wall.y() + 15f, 34f, 1f, 0.66f, 0.35f, 0.5f));
            }
        }
        for (float x = wall.x() + 7f; x < wall.right() - 4f; x += GARLAND_STEP) {
            lights.add(new Light(x, face - 8f, 30f, 1f, 0.3f, 0.25f, 0.55f, 0.06f, x));
        }
        // Toit en feu, une maison sur trois environ.
        if (hash(wall.x(), wall.y()) % 3 == 0 && wall.height() > 10f) {
            List<Vec2> spots = new ArrayList<>();
            for (float x = wall.x() + 14f; x < wall.right() - 6f; x += 40f) {
                Vec2 spot = new Vec2(x, face + wall.height() - 8f);
                spots.add(spot);
                lights.add(Light.flame(spot.x(), spot.y() + 8f, 90f, 1f));
            }
            roofFires.put(wall, spots);
        }
        // Débris qui brûlent au pied de la façade, de temps en temps.
        if (wall.y() > arena.y() + 20f) {
            for (float x = wall.x() + 20f; x < wall.right() - 12f; x += 48f) {
                Vec2 p = new Vec2(x, wall.y() - 7f);
                if (hash(x, wall.y()) % 4 == 0 && clear(p, walls, 4f)) {
                    addGroundFire(p);
                }
            }
        }
    }

    // Les piliers d'un torii vont par deux, à la même hauteur : on retient lequel est à gauche.
    private void pairTorii(List<Rect> walls) {
        for (Rect left : walls) {
            if (kinds.get(left) != Kind.TORII) {
                continue;
            }
            for (Rect right : walls) {
                if (kinds.get(right) == Kind.TORII && right.y() == left.y() && right.x() > left.x()) {
                    toriiPairs.put(left, right);
                    float mid = (left.x() + right.right()) / 2f;
                    lights.add(Light.steady(mid, lanternBase(left) + 40f, 70f, 1f, 0.4f, 0.3f, 0.45f));
                }
            }
        }
    }

    private void addGroundFire(Vec2 p) {
        groundFires.add(p);
        lights.add(Light.flame(p.x(), p.y() + 8f, 80f, 1f));
    }

    // Vrai si le point est loin de tout mur d'au moins « margin ».
    private static boolean clear(Vec2 p, List<Rect> walls, float margin) {
        for (Rect b : MapLayouts.BRIDGES) {
            if (p.x() > b.x() - margin && p.x() < b.right() + margin && p.y() > b.y() - margin && p.y() < b.top() + margin) {
                return false;
            }
        }
        for (Rect w : walls) {
            if (p.x() > w.x() - margin && p.x() < w.right() + margin && p.y() > w.y() - margin
                    && p.y() < w.top() + margin + HOUSE_FRONT) {
                return false;
            }
        }
        return true;
    }

    // Hachage stable d'une position : le décor est toujours le même d'une partie à l'autre.
    private static int hash(float x, float y) {
        int h = (int) x * 73856093 ^ (int) y * 19349663;
        h ^= h >>> 13;
        return Math.floorMod(h, 1 << 20);
    }

    // 0 = éclairée, 1 = sombre, 2 = entrée à rideau (noren).
    private static int bayKind(Rect wall, float x) {
        int h = hash(x, wall.y() * 7f) % 6;
        return h == 0 ? 2 : h == 1 ? 1 : 0;
    }

    private static float lanternBase(Rect wall) {
        return wall.y() + wall.height() / 2f - 6f;
    }

    /** @return les lumières du décor (lanternes, fenêtres, feux), en unités du monde */
    public List<Light> lights() {
        return lights;
    }

    /** @return les endroits où il y a du feu (au sol et sur les toits), pour la fumée */
    public List<Vec2> fireSpots() {
        List<Vec2> spots = new ArrayList<>(groundFires);
        roofFires.values().forEach(spots::addAll);
        return spots;
    }

    // ------------------------------------------------------------------ sol

    /**
     * Dessine le sol visible, les traces de brûlure, l'ombre des murs et les feux de débris.
     * Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch  le pinceau d'images
     * @param arena  les limites de la map
     * @param walls  les murs
     * @param camera la caméra du monde, pour ne dessiner que ce qui est à l'écran
     * @param viewW  largeur visible, en unités
     * @param viewH  hauteur visible, en unités
     * @param time   temps écoulé, en secondes, pour animer les flammes
     */
    public void drawGround(SpriteBatch batch, Rect arena, List<Rect> walls, OrthographicCamera camera,
            float viewW, float viewH, float time) {
        prepare(arena, walls);
        float x0 = Math.max(arena.x(), camera.position.x - viewW / 2f - 2f);
        float y0 = Math.max(arena.y(), camera.position.y - viewH / 2f - 2f);
        float x1 = Math.min(arena.right(), camera.position.x + viewW / 2f + 2f);
        float y1 = Math.min(arena.top(), camera.position.y + viewH / 2f + 2f);
        // Une seule image pour tout le sol : la texture se répète, calée sur le monde.
        float s = FLOOR_SIZE;
        batch.draw(floor, x0, y0, x1 - x0, y1 - y0, x0 / s, -y0 / s, x1 / s, -y1 / s);
        drawCanals(batch, time);
        for (Vec2 p : scorches) {
            if (visible(p, x0, y0, x1, y1, 32f)) {
                batch.draw(scorch, p.x() - 16f, p.y() - 8f);
            }
        }
        // Ombre portée sous chaque façade, et ombre ronde sous les lanternes de pierre.
        batch.setColor(0f, 0f, 0f, 0.5f);
        for (Rect wall : walls) {
            Kind kind = kinds.get(wall);
            if (kind == Kind.CANAL) {
                continue;
            }
            if (kind == Kind.LANTERN || kind == Kind.TORII) {
                float cx = wall.x() + wall.width() / 2f;
                float w = kind == Kind.LANTERN ? 22f : 14f;
                batch.draw(shadow, cx - w / 2f, lanternBase(wall) - 4f, w, 9f);
            } else {
                batch.draw(white, wall.x() - 3f, wall.y() - WALL_SHADOW, wall.width(), WALL_SHADOW);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        for (Vec2 p : groundFires) {
            if (visible(p, x0, y0, x1, y1, 32f)) {
                drawFire(batch, p, time);
            }
        }
    }

    // L'eau est au niveau du sol : le quai nord se voit de face, le bord sud n'est qu'une margelle.
    private void drawCanals(SpriteBatch batch, float time) {
        if (canals.isEmpty()) {
            return;
        }
        TextureRegion frame = water[(int) (time / WATER_FRAME) % water.length];
        for (Rect canal : canals) {
            fill(batch, frame, canal.x(), canal.y(), canal.width(), canal.height());
            fill(batch, bank, canal.x(), canal.top() - bank.getRegionHeight(), canal.width(), bank.getRegionHeight());
            tint(batch, 0.42f, 0.38f, 0.36f, 1f);
            batch.draw(white, canal.x() - 1f, canal.y() - 1f, canal.width() + 2f, 2f);
            batch.draw(white, canal.x() - 1f, canal.y(), 1f, canal.height());
            batch.draw(white, canal.right(), canal.y(), 1f, canal.height());
            batch.setColor(1f, 1f, 1f, 1f);
        }
        // Ponts de bois : planches en travers, garde-corps vermillon au nord et au sud.
        for (Rect bridge : MapLayouts.BRIDGES) {
            fill(batch, planks, bridge.x() - 4f, bridge.y(), bridge.width() + 8f, bridge.height());
            tint(batch, 1f, 0.85f, 0.65f, 0.25f);
            batch.draw(white, bridge.x() + bridge.width() / 2f - 6f, bridge.y(), 12f, bridge.height());   // dos d'âne
            batch.setColor(1f, 1f, 1f, 1f);
            fill(batch, railing, bridge.x() - 4f, bridge.top() - 8f, bridge.width() + 8f, 8f);
            fill(batch, railing, bridge.x() - 4f, bridge.y() - 2f, bridge.width() + 8f, 8f);
        }
    }

    private void drawFire(SpriteBatch batch, Vec2 p, float time) {
        int frame = (int) (time / FIRE_FRAME + p.x() * 0.13f) % fire.length;
        batch.draw(fire[frame], p.x() - 16f, p.y() - 3f);
    }

    private static boolean visible(Vec2 p, float x0, float y0, float x1, float y1, float margin) {
        return p.x() > x0 - margin && p.x() < x1 + margin && p.y() > y0 - margin && p.y() < y1 + margin;
    }

    // ------------------------------------------------------------------ murs

    /**
     * Dessine un mur habillé selon sa forme. Le {@link SpriteBatch} doit déjà être ouvert.
     *
     * @param batch le pinceau d'images
     * @param wall  l'emprise du mur dans le modèle
     * @param hero  la position (lissée) du héros : un mur qui le cache devient transparent
     * @param time  temps écoulé, en secondes, pour animer les flammes des toits
     */
    public void drawWall(SpriteBatch batch, Rect wall, Vec2 hero, float time) {
        Kind kind = kinds.getOrDefault(wall, Kind.HOUSE);
        float alpha = hides(wall, kind, hero) ? XRAY_ALPHA : 1f;
        batch.setColor(1f, 1f, 1f, alpha);
        switch (kind) {
            case HOUSE -> drawHouse(batch, wall, alpha, time);
            case FENCE -> drawFence(batch, wall);
            case LANTERN -> batch.draw(stoneLantern, wall.x() + wall.width() / 2f - 12f, lanternBase(wall));
            case TORII -> drawTorii(batch, wall);
            case CANAL -> { }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // Le mur est devant le héros (plus bas à l'écran) et son volume dessiné recouvre son sprite.
    private boolean hides(Rect wall, Kind kind, Vec2 hero) {
        float feet = hero.y() - 8f;
        if (feet <= wall.y()) {
            return false;
        }
        float left = wall.x() - 2f;
        float right = wall.right() + 2f;
        float top;
        if (kind == Kind.CANAL) {
            return false;
        }
        if (kind == Kind.TORII) {
            Rect partner = toriiPairs.get(wall);
            if (partner == null) {
                return false;
            }
            left = wall.x() - BEAM_OVERHANG;
            right = partner.right() + BEAM_OVERHANG;
            top = lanternBase(wall) + pillar.getRegionHeight() + 6f;
        } else if (kind == Kind.LANTERN) {
            float cx = wall.x() + wall.width() / 2f;
            left = cx - 12f;
            right = cx + 12f;
            top = lanternBase(wall) + 44f;
        } else {
            top = wall.top() + (kind == Kind.HOUSE ? HOUSE_FRONT : WALL_FRONT);
        }
        return hero.x() + 12f > left && hero.x() - 12f < right && feet < top;
    }

    private void drawHouse(SpriteBatch batch, Rect wall, float alpha, float time) {
        boolean red = hash(wall.x(), wall.y()) % 2 == 0;
        // Façade : une travée de 16 unités après l'autre, la dernière coupée si besoin.
        for (float x = wall.x(); x < wall.right(); x += BAY) {
            float w = Math.min(BAY, wall.right() - x);
            TextureRegion bay = bays[(red ? 3 : 0) + bayKind(wall, x)];
            batch.draw(bay.getTexture(), x, wall.y(), w, HOUSE_FRONT,
                    bay.getRegionX(), bay.getRegionY(), (int) w, (int) HOUSE_FRONT, false, false);
        }
        // Toit de tuiles, qui déborde un peu sur les côtés.
        float roofY = wall.y() + HOUSE_FRONT;
        fill(batch, roof, wall.x() - 2f, roofY, wall.width() + 4f, wall.height());
        tint(batch, 0.09f, 0.10f, 0.14f, alpha);
        batch.draw(white, wall.x() - 3f, roofY, wall.width() + 6f, 1f);                  // bord du toit
        tint(batch, 0.29f, 0.33f, 0.41f, alpha);
        batch.draw(white, wall.x() - 3f, roofY + 1f, wall.width() + 6f, 1f);
        tint(batch, 0.55f, 0.6f, 0.7f, alpha);
        batch.draw(white, wall.x() - 2f, roofY + wall.height() - 1f, wall.width() + 4f, 1f);  // faîtage
        batch.setColor(1f, 1f, 1f, alpha);
        // Guirlande de lanternes rouges sous l'avant-toit.
        for (float x = wall.x() + 7f; x < wall.right() - 4f; x += GARLAND_STEP) {
            tint(batch, 0.08f, 0.05f, 0.05f, alpha);
            batch.draw(white, x, roofY - 3f, 1f, 3f);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(redLantern, x - 3f, roofY - 12f);
        }
        List<Vec2> spots = roofFires.get(wall);
        if (spots != null) {
            for (Vec2 spot : spots) {
                tint(batch, 0.08f, 0.04f, 0.05f, alpha);
                batch.draw(shadow, spot.x() - 10f, spot.y() - 3f, 20f, 7f);
                batch.setColor(1f, 1f, 1f, alpha);
                drawFire(batch, spot, time);
            }
        }
    }

    // Le pilier gauche dessine tout le portique (ses deux piliers et le linteau) : le linteau passe
    // ainsi toujours par-dessus les piliers. Le pilier droit ne dessine rien.
    private void drawTorii(SpriteBatch batch, Rect wall) {
        Rect partner = toriiPairs.get(wall);
        if (partner == null) {
            return;
        }
        float base = lanternBase(wall);
        float leftX = wall.x() + wall.width() / 2f;
        float rightX = partner.x() + partner.width() / 2f;
        batch.draw(pillar, leftX - pillar.getRegionWidth() / 2f, base);
        batch.draw(pillar, rightX - pillar.getRegionWidth() / 2f, base);
        float beamY = base + pillar.getRegionHeight() - 14f;
        fill(batch, beam, leftX - BEAM_OVERHANG, beamY, rightX - leftX + 2 * BEAM_OVERHANG, beam.getRegionHeight());
    }

    private void drawFence(SpriteBatch batch, Rect wall) {
        fill(batch, fenceFace, wall.x(), wall.y(), wall.width(), WALL_FRONT);
        float topY = wall.y() + WALL_FRONT;
        fill(batch, plaster, wall.x(), topY, wall.width(), wall.height());
        if (wall.width() >= wall.height()) {
            float band = Math.min(capH.getRegionHeight(), wall.height());
            fill(batch, capH, wall.x(), topY + (wall.height() - band) / 2f, wall.width(), band);
        } else {
            float band = Math.min(capV.getRegionWidth(), wall.width());
            fill(batch, capV, wall.x() + (wall.width() - band) / 2f, topY, band, wall.height());
        }
    }

    private void tint(SpriteBatch batch, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
    }

    // Répète une région sur un rectangle, en coupant ce qui dépasse en haut et à droite.
    private void fill(SpriteBatch batch, TextureRegion region, float x, float y, float w, float h) {
        int rw = region.getRegionWidth();
        int rh = region.getRegionHeight();
        for (float py = y; py < y + h; py += rh) {
            float ch = Math.min(rh, y + h - py);
            for (float px = x; px < x + w; px += rw) {
                float cw = Math.min(rw, x + w - px);
                batch.draw(region.getTexture(), px, py, cw, ch, region.getRegionX(),
                        region.getRegionY() + rh - (int) ch, (int) cw, (int) ch, false, false);
            }
        }
    }

    @Override
    public void dispose() {
        tiles.dispose();
        floor.dispose();
    }
}
