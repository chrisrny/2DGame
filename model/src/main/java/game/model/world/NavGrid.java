package game.model.world;

import game.model.combat.Collisions;
import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Grille de navigation : elle permet aux ennemis de contourner les murs pour rejoindre le joueur.
 */
public final class NavGrid {

    /** Côté d'une case, en unités. */
    public static final float CELL = 16f;

    /** Nombre maximum de cases suivies à l'avance pour choisir le point à viser. */
    public static final int LOOKAHEAD = 12;

    private static final int UNREACHABLE = Integer.MAX_VALUE;
    private static final int STRAIGHT_COST = 10;
    private static final int DIAGONAL_COST = 14;
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
    };

    private final Rect arena;
    private final List<Rect> walls;
    private final int cols;
    private final int rows;
    private final boolean[] walkable;
    private final int[] distance;
    private int targetCell = -1;

    /**
     * Crée la grille d'une map.
     *
     * @param arena les limites de la map
     * @param walls les murs et obstacles
     * @param clearance rayon des ennemis pour lesquels une case doit être praticable
     */
    public NavGrid(Rect arena, List<Rect> walls, float clearance) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.walls = List.copyOf(walls);
        this.cols = Math.max(1, (int) Math.ceil(arena.width() / CELL));
        this.rows = Math.max(1, (int) Math.ceil(arena.height() / CELL));
        this.walkable = new boolean[cols * rows];
        this.distance = new int[cols * rows];
        Arrays.fill(distance, UNREACHABLE);
        for (int cell = 0; cell < walkable.length; cell++) {
            Circle body = new Circle(center(cell), clearance);
            walkable[cell] = this.walls.stream().noneMatch(wall -> Collisions.overlaps(body, wall));
        }
    }

    /**
     * Met à jour la carte de flux vers une cible. Le calcul n'est refait que si la cible a changé
     * de case.
     *
     * @param target la position à rejoindre, en général celle du joueur
     */
    public void updateTarget(Vec2 target) {
        int cell = cellOf(target);
        if (cell == targetCell) {
            return;
        }
        targetCell = cell;
        computeDistances(cell);
    }

    /**
     * Le point à viser pour se rapprocher de la cible en contournant les murs.
     *
     * @param from position de l'ennemi
     * @param radius rayon de l'ennemi, pour vérifier qu'il passe en ligne droite jusqu'au point
     * @param target la cible donnée à {@link #updateTarget}, renvoyée si aucun chemin n'existe
     * @return le point à viser maintenant
     */
    public Vec2 nextWaypoint(Vec2 from, float radius, Vec2 target) {
        int start = startCell(from);
        if (start < 0) {
            return target;
        }
        // On descend la carte de flux sur quelques cases...
        int[] chain = new int[LOOKAHEAD + 1];
        int length = 0;
        chain[length++] = start;
        int cell = start;
        while (length <= LOOKAHEAD && cell != targetCell) {
            int next = bestNeighbour(cell);
            if (next < 0) {
                break;
            }
            chain[length++] = next;
            cell = next;
        }
        // ... et on vise la plus lointaine qu'on atteint en ligne droite.
        if (cell == targetCell && Collisions.pathIsClear(from, target, radius, walls)) {
            return target;
        }
        for (int i = length - 1; i > 0; i--) {
            Vec2 point = center(chain[i]);
            if (Collisions.pathIsClear(from, point, radius, walls)) {
                return point;
            }
        }
        return center(length > 1 ? chain[1] : start);
    }

    /**
     * Indique si une case est praticable.
     *
     * @param point un point de la map
     * @return {@code true} si la case qui contient ce point est praticable
     */
    public boolean isWalkable(Vec2 point) {
        int cell = cellOf(point);
        return cell >= 0 && walkable[cell];
    }

    /**
     * Indique si la cible actuelle peut être rejointe depuis un point.
     *
     * @param point un point de la map
     * @return {@code true} si un chemin mène de ce point à la cible
     */
    public boolean canReachTarget(Vec2 point) {
        int cell = cellOf(point);
        return cell >= 0 && distance[cell] != UNREACHABLE;
    }

    // Dijkstra depuis la case cible.
    private void computeDistances(int source) {
        Arrays.fill(distance, UNREACHABLE);
        if (source < 0) {
            return;
        }
        distance[source] = 0;
        PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        queue.add(new long[] {0, source});
        while (!queue.isEmpty()) {
            long[] head = queue.poll();
            int cell = (int) head[1];
            if (head[0] > distance[cell]) {
                continue;
            }
            for (int[] dir : DIRECTIONS) {
                int next = neighbour(cell, dir[0], dir[1]);
                if (next < 0 || !walkable[next]) {
                    continue;
                }
                int cost = distance[cell] + (dir[0] != 0 && dir[1] != 0 ? DIAGONAL_COST : STRAIGHT_COST);
                if (cost < distance[next]) {
                    distance[next] = cost;
                    queue.add(new long[] {cost, next});
                }
            }
        }
    }

    private int bestNeighbour(int cell) {
        int best = -1;
        int bestDistance = distance[cell];
        for (int[] dir : DIRECTIONS) {
            int next = neighbour(cell, dir[0], dir[1]);
            if (next >= 0 && distance[next] < bestDistance) {
                best = next;
                bestDistance = distance[next];
            }
        }
        return best;
    }

    // Un ennemi collé à un mur peut se trouver sur une case non praticable : on prend alors la case
    // voisine, à deux cases au plus, la plus proche de la cible et en vue.
    private int startCell(Vec2 from) {
        int cell = cellOf(from);
        if (cell >= 0 && distance[cell] != UNREACHABLE) {
            return cell;
        }
        int col = Math.clamp((int) ((from.x() - arena.x()) / CELL), 0, cols - 1);
        int row = Math.clamp((int) ((from.y() - arena.y()) / CELL), 0, rows - 1);
        int best = -1;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int c = col + dx;
                int r = row + dy;
                if (c < 0 || r < 0 || c >= cols || r >= rows) {
                    continue;
                }
                int candidate = r * cols + c;
                if (distance[candidate] != UNREACHABLE
                        && (best < 0 || distance[candidate] < distance[best])
                        && Collisions.pathIsClear(from, center(candidate), 0f, walls)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    // Voisin d'une case.
    private int neighbour(int cell, int dx, int dy) {
        int col = cell % cols + dx;
        int row = cell / cols + dy;
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return -1;
        }
        if (dx != 0 && dy != 0
                && (!walkable[row * cols + (col - dx)] || !walkable[(row - dy) * cols + col])) {
            return -1;
        }
        return row * cols + col;
    }

    private int cellOf(Vec2 point) {
        int col = (int) Math.floor((point.x() - arena.x()) / CELL);
        int row = (int) Math.floor((point.y() - arena.y()) / CELL);
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            return -1;
        }
        return row * cols + col;
    }

    private Vec2 center(int cell) {
        return new Vec2(arena.x() + (cell % cols + 0.5f) * CELL, arena.y() + (cell / cols + 0.5f) * CELL);
    }
}
