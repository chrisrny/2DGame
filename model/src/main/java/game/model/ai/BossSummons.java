package game.model.ai;

import game.model.combat.Collisions;
import game.model.combat.Telegraph;
import game.model.commun.Circle;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Boss;
import game.model.entity.Enemy;
import game.model.entity.Prowler;
import game.model.world.EnemyFactory;
import game.model.world.EnemyType;
import game.model.world.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Les renforts du boss : il invoque {@link #COUNT} Rôdeurs toutes les {@link #INTERVAL} secondes,
 * pendant tout le combat, jamais plus de {@link #MAX_ALIVE} en vie en même temps.
 */
public final class BossSummons {

    /** Délai entre l'apparition du boss et la première invocation, en secondes. */
    public static final float FIRST_DELAY = 3f;

    /** Temps entre deux invocations, en secondes : assez court pour que la pression ne retombe pas. */
    public static final float INTERVAL = 7f;

    /** Nombre de Rôdeurs invoqués à chaque fois. */
    public static final int COUNT = 3;

    /** Nombre maximum de Rôdeurs invoqués en vie en même temps. */
    public static final int MAX_ALIVE = 6;

    /** Durée de l'annonce d'une invocation, en secondes : le joueur voit où ils vont arriver. */
    public static final float WIND_UP = 0.5f;

    /** Distance entre le centre du boss et un Rôdeur invoqué : juste à côté de lui. */
    public static final float DISTANCE = 40f;

    /** Distance minimale entre un Rôdeur invoqué et le joueur : il n'apparaît jamais sur lui. */
    public static final float MIN_PLAYER_DISTANCE = 40f;

    /** Rayon du cercle d'annonce dessiné à l'endroit d'une arrivée. */
    public static final float MARK_RADIUS = 12f;

    // Places essayées autour du boss, en degrés par rapport à la direction du joueur : d'abord sur
    // ses flancs (les Rôdeurs prennent le joueur en tenaille), en dernier vers le joueur.
    private static final int[] ANGLES = {90, -90, 135, -135, 45, -45, 180, 0};

    // Même tolérance que pour les autres minuteurs décomptés pas à pas en float.
    private static final float EPSILON = 1e-4f;

    private final List<Enemy> summoned = new ArrayList<>();
    private List<Circle> pendingZones = List.of();
    private float nextIn = FIRST_DELAY;
    private float windUp;

    /**
     * Fait avancer les invocations d'un pas : une annonce en cours se termine par l'arrivée des
     * Rôdeurs ; à chaque intervalle, une nouvelle annonce commence s'il reste de la place sous le
     * maximum.
     *
     * @param boss le boss qui invoque
     * @param world le monde, pour le joueur, les murs et l'apparition des Rôdeurs
     * @param dt durée du pas, en secondes
     */
    public void update(Boss boss, World world, float dt) {
        summoned.removeIf(enemy -> !enemy.isAlive());
        if (!pendingZones.isEmpty()) {
            windUp += dt;
            if (windUp >= WIND_UP - EPSILON) {
                for (Circle zone : pendingZones) {
                    Enemy prowler = EnemyFactory.create(EnemyType.PROWLER, zone.center(), boss.multiplier());
                    world.spawnEnemy(prowler);
                    summoned.add(prowler);
                }
                pendingZones = List.of();
            }
        }
        nextIn -= dt;
        if (nextIn > EPSILON) {
            return;
        }
        nextIn += INTERVAL;
        int room = MAX_ALIVE - summoned.size();
        if (pendingZones.isEmpty() && room > 0) {
            pendingZones = zones(boss, world, Math.min(COUNT, room));
            windUp = 0f;
        }
    }

    // Les places libres autour du boss, dans l'ordre de ANGLES.
    private static List<Circle> zones(Boss boss, World world, int count) {
        Vec2 center = boss.position();
        Vec2 toPlayer = world.playerPosition().sub(center).normalized();
        double base = toPlayer.length() == 0f ? 0.0 : Math.atan2(toPlayer.y(), toPlayer.x());
        List<Circle> zones = new ArrayList<>();
        for (int i = 0; i < ANGLES.length && zones.size() < count; i++) {
            double angle = base + Math.toRadians(ANGLES[i]);
            Vec2 offset = new Vec2((float) Math.cos(angle), (float) Math.sin(angle)).scale(DISTANCE);
            Circle zone = new Circle(center.add(offset), Prowler.RADIUS);
            if (isFree(zone, center, world, zones)) {
                zones.add(zone);
            }
        }
        return zones;
    }

    // Une place est libre si le Rôdeur y tient (dans l'arène, hors des murs), si le boss la rejoint
    // sans mur entre eux, si elle n'est pas sur le joueur ni sur un autre Rôdeur annoncé.
    private static boolean isFree(Circle zone, Vec2 from, World world, List<Circle> taken) {
        Vec2 point = zone.center();
        float r = zone.radius();
        Rect arena = world.arena();
        boolean inArena = point.x() - r >= arena.x() && point.x() + r <= arena.right()
                && point.y() - r >= arena.y() && point.y() + r <= arena.top();
        return inArena
                && world.walls().stream().noneMatch(wall -> Collisions.overlaps(zone, wall))
                && world.hasClearPath(from, point, r)
                && point.distanceTo(world.playerPosition()) >= MIN_PLAYER_DISTANCE
                && taken.stream().noneMatch(other -> Collisions.overlaps(zone, other));
    }

    /**
     * Les arrivées de Rôdeurs en cours d'annonce : un cercle par Rôdeur, qui se remplit pendant
     * {@link #WIND_UP}.
     */
    public List<Telegraph> telegraphs() {
        float progress = Math.min(1f, windUp / WIND_UP);
        return pendingZones.stream()
                .map(zone -> new Telegraph(new Circle(zone.center(), MARK_RADIUS), progress))
                .toList();
    }

    /** @return le nombre de Rôdeurs invoqués encore en vie */
    public int alive() {
        return (int) summoned.stream().filter(Enemy::isAlive).count();
    }
}
