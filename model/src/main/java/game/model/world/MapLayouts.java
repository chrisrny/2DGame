package game.model.world;

import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.ArrayList;
import java.util.List;

/** Les maps du jeu, décrites en code. */
public final class MapLayouts {

    /** Épaisseur du mur qui entoure la map. */
    public static final float WALL = 16f;

    /** Arène du boss dans la map de la démo : la zone centrale, la plus dégagée. */
    public static final Rect BOSS_ARENA = new Rect(460f, 230f, 1000f, 560f);

    /**
     * Canaux de l'ouest et de l'est, coupés en deux par un pont : de l'eau, infranchissable à pied,
     * mais les projectiles et les sorts passent au-dessus.
     */
    public static final List<Rect> CANALS = List.of(
            r(168, 280, 40, 200), r(168, 560, 40, 180),
            r(1712, 280, 40, 200), r(1712, 560, 40, 180));

    /** Ponts entre les deux moitiés de chaque canal : des passages libres, pas des murs. */
    public static final List<Rect> BRIDGES = List.of(r(168, 480, 40, 80), r(1712, 480, 40, 80));

    /** Piliers des torii, deux par porte, au bout des couloirs du nord et du sud. */
    public static final List<Rect> TORII = List.of(
            r(896, 885, 14, 14), r(1010, 885, 14, 14),
            r(896, 175, 14, 14), r(1010, 175, 14, 14));

    private MapLayouts() {
    }

    /** @return la map de la démo */
    public static MapLayout main() {
        Rect arena = new Rect(0f, 0f, 1920f, 1080f);
        List<Rect> walls = new ArrayList<>(border(arena));
        walls.addAll(List.of(
                // Roche qui rend le contour irrégulier : coins en escalier, baie, avancée, renflements.
                r(16, 16, 176, 96), r(16, 112, 80, 64),
                r(16, 952, 128, 112), r(144, 1000, 112, 64),
                r(1744, 944, 160, 120), r(1648, 1008, 96, 56),
                r(1760, 16, 144, 128), r(1840, 144, 64, 64),
                r(896, 1000, 128, 64),
                r(832, 16, 256, 48),
                r(16, 280, 72, 160),
                r(1832, 600, 72, 200),
                // Départ : quatre petits piliers autour du joueur.
                r(840, 420, 24, 24), r(1056, 420, 24, 24), r(840, 636, 24, 24), r(1056, 636, 24, 24),
                // Ruines en L.
                r(220, 840, 160, 24), r(220, 760, 24, 104), r(430, 800, 120, 24), r(430, 800, 24, 140),
                // Couloirs ouverts au milieu, en haut et en bas.
                r(720, 880, 176, 24), r(1024, 880, 176, 24),
                r(720, 170, 176, 24), r(1024, 170, 176, 24),
                // Grille de piliers.
                r(1380, 800, 32, 32), r(1510, 800, 32, 32), r(1640, 800, 32, 32),
                r(1380, 920, 32, 32), r(1510, 920, 32, 32), r(1640, 920, 32, 32),
                // Passages entre la zone de départ et les côtés.
                r(320, 400, 24, 110), r(320, 570, 24, 110),
                r(1576, 400, 24, 110), r(1576, 570, 24, 110),
                // Rochers.
                r(260, 180, 64, 48), r(360, 280, 48, 48), r(470, 120, 80, 40),
                // Piliers épars.
                r(1380, 120, 32, 32), r(1520, 230, 32, 32), r(1660, 120, 32, 32)));
        walls.addAll(CANALS);
        walls.addAll(TORII);
        List<Vec2> spawnPoints = List.of(
                v(90, 880), v(300, 980), v(780, 1000), v(1140, 1000), v(1560, 1010), v(1840, 880),
                v(100, 560), v(480, 600), v(1440, 560), v(1760, 520),
                v(140, 240), v(560, 260), v(780, 110), v(1160, 110), v(1460, 60), v(1700, 250));
        return new MapLayout(arena, walls, spawnPoints, BOSS_ARENA, CANALS);
    }

    private static List<Rect> border(Rect arena) {
        float w = arena.width();
        float h = arena.height();
        return List.of(
                new Rect(0f, 0f, w, WALL),
                new Rect(0f, h - WALL, w, WALL),
                new Rect(0f, WALL, WALL, h - 2 * WALL),
                new Rect(w - WALL, WALL, WALL, h - 2 * WALL));
    }

    private static Rect r(float x, float y, float width, float height) {
        return new Rect(x, y, width, height);
    }

    private static Vec2 v(float x, float y) {
        return new Vec2(x, y);
    }
}
