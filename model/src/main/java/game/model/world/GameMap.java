package game.model.world;

import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.PlayerIntent;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * La map de jeu : elle contient le joueur, les ennemis et les projectiles, et c'est par elle qu'ils
 * interagissent.
 */
public final class GameMap implements World {

    /** Vitesse du recul infligé au joueur par un ennemi au contact, en unités par seconde. */
    public static final float CONTACT_KNOCKBACK = 120f;

    /** Délai minimum entre deux coups de contact d'un même ennemi, en secondes. */
    public static final float CONTACT_COOLDOWN = 0.6f;

    /** Marge gardée entre un ennemi et les murs quand il contourne un obstacle. */
    public static final float PATH_MARGIN = 2f;

    /** Nombre maximum de passes de séparation par pas de jeu. */
    public static final int SEPARATION_PASSES = 50;

    /** En dessous de cette distance au joueur, l'ennemi va droit sur lui sans chercher de chemin. */
    public static final float DIRECT_CHASE_DISTANCE = 2f * NavGrid.CELL;

    /** Distance minimale au joueur d'un point d'apparition : aucun ennemi n'apparaît sur lui. */
    public static final float MIN_SPAWN_DISTANCE = 200f;

    /**
     * Distance au joueur où apparaissent les ennemis d'une vague : juste hors de l'écran (sa
     * demi-diagonale fait 367 unités), pour qu'ils arrivent vite au combat sans surgir sous ses
     * yeux.
     */
    public static final float WAVE_SPAWN_MIN = 380f;
    public static final float WAVE_SPAWN_MAX = 560f;

    /** Une vague arrive d'un côté : ses ennemis sont répartis à ±75° autour d'une direction tirée au hasard. */
    public static final float WAVE_SPREAD_DEGREES = 75f;

    // Essais pour trouver une place libre et accessible, et marge autour d'un ennemi qui apparaît
    // (le plus gros, le Chargeur, a un rayon de 10).
    private static final int SPAWN_TRIES = 40;
    private static final float SPAWN_CLEARANCE = 12f;

    // Tolérance sur le délai des vagues : 90 pas de 1/60 s font 1,5 s à un arrondi de float près.
    private static final float EPSILON = 1e-4f;

    private final MapLayout layout;
    // Les murs moins l'eau, calculés une fois : ceux qui arrêtent les projectiles.
    private final List<Rect> solidWalls;
    private final Player player;
    private final EventBus events;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Map<Enemy, Float> contactCooldowns = new IdentityHashMap<>();
    private final Random random;
    // Une grille de navigation par taille d'ennemi (rayon + marge, arrondi à l'unité) : un grand
    // ennemi ne passe pas partout où passe un petit.
    private final Map<Integer, NavGrid> navGrids = new HashMap<>();
    private StageConfig stage;
    private int wavesSpawned;
    private float waveTimer;
    private int kills;
    private boolean stageCleared;

    /**
     * Crée une map, sans stage en cours : aucune vague n'apparaît avant {@link #startStage}.
     *
     * @param layout le plan de la map
     * @param player le joueur, déjà placé sur la map
     * @param events le bus d'événements de la partie
     * @param random la source de hasard de la partie, pour tirer les points d'apparition
     */
    public GameMap(MapLayout layout, Player player, EventBus events, Random random) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.solidWalls = layout.walls().stream().filter(wall -> !layout.water().contains(wall)).toList();
        this.player = Objects.requireNonNull(player, "player");
        this.events = Objects.requireNonNull(events, "events");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Commence un stage : remet à zéro le compteur d'ennemis tués et repart de sa première vague.
     * Les ennemis déjà présents restent sur la map.
     *
     * @param newStage le stage à jouer
     */
    public void startStage(StageConfig newStage) {
        this.stage = Objects.requireNonNull(newStage, "newStage");
        this.wavesSpawned = 0;
        this.waveTimer = 0f;
        this.kills = 0;
        this.stageCleared = false;
    }

    /**
     * Ajoute un ennemi sur la map.
     *
     * @param enemy l'ennemi, déjà placé
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(Objects.requireNonNull(enemy, "enemy"));
    }

    /**
     * Fait avancer la map d'un pas de jeu.
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param dt durée du pas, en secondes
     */
    public void update(PlayerIntent intent, float dt) {
        player.update(intent, this, dt);
        for (Enemy enemy : List.copyOf(enemies)) {
            if (enemy.isAlive()) {
                enemy.update(this, dt);
            }
        }
        separateEnemies();
        updateProjectiles(dt);
        applyContactDamage(dt);
        removeDeadEnemies();
        updateWaves(dt);
    }

    // Les ennemis ne se chevauchent jamais : sans ça, une vague qui poursuit le joueur finit
    // empilée en un seul rond.
    private void separateEnemies() {
        List<Enemy> alive = enemies.stream().filter(Enemy::isAlive).toList();
        for (int pass = 0; pass < SEPARATION_PASSES; pass++) {
            boolean anyOverlap = false;
            for (int i = 0; i < alive.size(); i++) {
                for (int j = i + 1; j < alive.size(); j++) {
                    anyOverlap |= separate(alive.get(i), alive.get(j), i, j);
                }
            }
            if (!anyOverlap) {
                return;
            }
        }
    }

    // Écarte deux ennemis qui se chevauchent : chacun recule d'une part du chevauchement, le plus
    // léger plus que le plus lourd.
    private boolean separate(Enemy a, Enemy b, int i, int j) {
        float minDistance = a.hitbox().radius() + b.hitbox().radius();
        Vec2 delta = b.position().sub(a.position());
        float distance = delta.length();
        float overlap = minDistance - distance;
        if (overlap <= 0f) {
            return false;
        }
        // Deux ennemis exactement au même endroit (même point d'apparition) n'ont pas de direction
        // : on en choisit une qui dépend de la paire, pour rester reproductible.
        Vec2 normal = distance > 0f ? delta.scale(1f / distance) : fallbackDirection(i, j);
        float shareA = mass(b) / (mass(a) + mass(b));

        float doneA = -pushEnemy(a, normal.scale(-overlap * shareA)).dot(normal);
        float doneB = pushEnemy(b, normal.scale(overlap - doneA)).dot(normal);
        float left = overlap - doneA - doneB;
        if (left > 0f) {
            pushEnemy(a, normal.scale(-left));
        }
        return true;
    }

    // La poussée d'un appel ne dépasse pas le rayon : Collisions.move suppose qu'un déplacement
    // reste plus petit que le rayon, sinon un ennemi pourrait traverser un mur fin.
    private Vec2 pushEnemy(Enemy enemy, Vec2 push) {
        float max = enemy.hitbox().radius();
        Vec2 limited = push.length() > max ? push.normalized().scale(max) : push;
        if (limited.length() == 0f) {
            return Vec2.ZERO;
        }
        return enemy.moveBy(limited, this);
    }

    // « Masse » pour partager la poussée : la surface de l'ennemi, multipliée par sa résistance.
    private static float mass(Enemy enemy) {
        float r = enemy.hitbox().radius();
        return r * r * enemy.pushResistance();
    }

    private static Vec2 fallbackDirection(int i, int j) {
        double angle = (i * 31 + j * 17) * 0.7853981633974483;
        return new Vec2((float) Math.cos(angle), (float) Math.sin(angle));
    }

    // On parcourt une copie : une mise à jour peut ajouter un projectile (spawnProjectile) sans
    // casser le parcours.
    private void updateProjectiles(float dt) {
        for (Projectile projectile : List.copyOf(projectiles)) {
            projectile.update(this, dt);
        }
        projectiles.removeIf(projectile -> !projectile.isAlive());
    }

    // Un ennemi au contact ne frappe qu'une fois par CONTACT_COOLDOWN, sinon il viderait les PV du
    // joueur en quelques pas.
    private void applyContactDamage(float dt) {
        for (Enemy enemy : enemies) {
            float cooldown = Math.max(0f, contactCooldowns.getOrDefault(enemy, 0f) - dt);
            if (cooldown == 0f && enemy.isAlive() && !enemy.isStunned() && enemy.contactDamage() > 0
                    && Collisions.overlaps(enemy.hitbox(), player.hitbox())) {
                Vec2 push = player.position().sub(enemy.position()).normalized().scale(CONTACT_KNOCKBACK);
                Hit hit = new Hit(enemy.contactDamage(), Faction.ENEMY, false, push);
                if (player.receive(hit)) {
                    events.publish(new GameEvent.EntityHit(player.position(), hit.damage(), false, Faction.PLAYER));
                    cooldown = CONTACT_COOLDOWN;
                }
            }
            contactCooldowns.put(enemy, cooldown);
        }
    }

    private void removeDeadEnemies() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            if (!enemy.isAlive()) {
                events.publish(new GameEvent.EntityDied(enemy.position(), Faction.ENEMY));
                contactCooldowns.remove(enemy);
                it.remove();
                kills++;
            }
        }
        if (stage != null && !stageCleared && kills >= stage.killsToClear()) {
            stageCleared = true;
            events.publish(new GameEvent.StageCleared(stage.number()));
        }
    }

    // Le délai d'une vague ne compte qu'à partir du moment où plus aucun ennemi n'est vivant.
    private void updateWaves(float dt) {
        if (stage == null || stageCleared || stage.waves().isEmpty() || !enemies.isEmpty()) {
            waveTimer = 0f;
            return;
        }
        List<Wave> waves = stage.waves();
        // Après la dernière vague, elle revient tant que le stage n'est pas fini.
        Wave next = waves.get(Math.min(wavesSpawned, waves.size() - 1));
        waveTimer += dt;
        if (waveTimer >= next.delayBefore() - EPSILON) {
            spawn(next);
            wavesSpawned++;
            waveTimer = 0f;
        }
    }

    // Tout est tiré avec le Random de la partie : même graine, mêmes apparitions.
    private void spawn(Wave wave) {
        List<Vec2> fallback = spawnPointsAwayFromPlayer();
        Collections.shuffle(fallback, random);
        double side = random.nextDouble() * Math.PI * 2;
        int next = 0;
        for (EnemyType type : EnemyType.values()) {
            int count = wave.enemies().getOrDefault(type, 0);
            for (int i = 0; i < count; i++) {
                Vec2 point = nearSpawnPoint(side).orElse(fallback.get(next % fallback.size()));
                addEnemy(EnemyFactory.create(type, point, stage.multiplier()));
                next++;
            }
        }
    }

    /**
     * Cherche une place pour un ennemi de vague : hors de l'écran mais près du joueur (entre {@link
     * #WAVE_SPAWN_MIN} et {@link #WAVE_SPAWN_MAX}), du côté d'où arrive la vague, sur une case
     * libre d'où l'on peut rejoindre le joueur (pas dans une maison ni dans un canal).
     */
    private Optional<Vec2> nearSpawnPoint(double side) {
        Vec2 center = player.position();
        int clearance = (int) Math.ceil(SPAWN_CLEARANCE + PATH_MARGIN);
        NavGrid nav = navGrids.computeIfAbsent(clearance,
                size -> new NavGrid(layout.arena(), layout.walls(), size));
        nav.updateTarget(center);
        double spread = Math.toRadians(WAVE_SPREAD_DEGREES);
        for (int i = 0; i < SPAWN_TRIES; i++) {
            double angle = side + (random.nextDouble() * 2 - 1) * spread;
            float distance = WAVE_SPAWN_MIN + random.nextFloat() * (WAVE_SPAWN_MAX - WAVE_SPAWN_MIN);
            Vec2 point = center.add(new Vec2((float) Math.cos(angle) * distance, (float) Math.sin(angle) * distance));
            Circle body = new Circle(point, SPAWN_CLEARANCE);
            if (layout.arena().contains(point)
                    && layout.walls().stream().noneMatch(wall -> Collisions.overlaps(body, wall))
                    && nav.canReachTarget(point)) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    /**
     * Tire au hasard un point d'apparition à au moins {@link #MIN_SPAWN_DISTANCE} du joueur, pour
     * un ennemi qui n'arrive pas avec une vague, comme le boss.
     */
    public Vec2 randomSpawnPoint() {
        List<Vec2> points = spawnPointsAwayFromPlayer();
        return points.get(random.nextInt(points.size()));
    }

    /**
     * Le point d'apparition du boss : celui de l'arène du boss le plus loin du joueur, pour lui
     * laisser le temps de le voir arriver.
     */
    public Vec2 bossSpawnPoint() {
        return layout.spawnPoints().stream()
                .filter(layout.bossArena()::contains)
                .max(Comparator.comparingDouble(point -> point.distanceTo(player.position())))
                .orElseGet(this::randomSpawnPoint);
    }

    private List<Vec2> spawnPointsAwayFromPlayer() {
        List<Vec2> points = new ArrayList<>();
        for (Vec2 point : layout.spawnPoints()) {
            if (point.distanceTo(player.position()) >= MIN_SPAWN_DISTANCE) {
                points.add(point);
            }
        }
        if (points.isEmpty()) {
            throw new IllegalStateException("Aucun point d'apparition à plus de " + MIN_SPAWN_DISTANCE + " du joueur");
        }
        return points;
    }

    @Override
    public Vec2 playerPosition() {
        return player.position();
    }

    @Override
    public Rect arena() {
        return layout.arena();
    }

    @Override
    public List<Rect> walls() {
        return layout.walls();
    }

    @Override
    public List<Rect> solidWalls() {
        return solidWalls;
    }

    @Override
    public List<Damageable> targetsIn(Circle zone, Faction attacker) {
        List<Damageable> candidates = new ArrayList<>(enemies);
        candidates.add(player);
        return candidates.stream()
                .filter(Damageable::isAlive)
                .filter(target -> target.faction() != attacker)
                .filter(target -> Collisions.overlaps(zone, target.hitbox()))
                .toList();
    }

    @Override
    public void spawnEnemy(Enemy enemy) {
        // Ajouté à la liste parcourue par update : celle-ci travaille sur une copie, l'ennemi
        // invoqué n'agit donc qu'à partir du pas suivant.
        addEnemy(enemy);
    }

    @Override
    public void spawnProjectile(Projectile projectile) {
        projectiles.add(Objects.requireNonNull(projectile, "projectile"));
    }

    /**
     * Va tout droit vers le joueur si rien ne gêne ; sinon, suit la grille de navigation pour
     * contourner les murs.
     */
    @Override
    public Vec2 pathTowardPlayer(Vec2 from, float radius) {
        Vec2 target = player.position();
        float body = radius + PATH_MARGIN;
        // Tout près du joueur, même collé à un mur, on fonce sur lui : le glissement le long des
        // murs suffit, et un détour l'empêcherait de le toucher.
        if (from.distanceTo(target) <= DIRECT_CHASE_DISTANCE
                || Collisions.pathIsClear(from, target, body, layout.walls())) {
            return target;
        }
        int clearance = (int) Math.ceil(body);
        NavGrid nav = navGrids.computeIfAbsent(clearance,
                size -> new NavGrid(layout.arena(), layout.walls(), size));
        // La carte de flux suit le joueur : elle n'est recalculée que quand il change de case.
        nav.updateTarget(target);
        return nav.nextWaypoint(from, body, target);
    }

    @Override
    public EventBus events() {
        return events;
    }

    /** @return le plan de la map */
    public MapLayout layout() {
        return layout;
    }

    /** @return les ennemis encore sur la map (lecture seule) */
    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    /** @return le stage en cours, ou {@code null} si aucun stage n'a commencé */
    public StageConfig stage() {
        return stage;
    }

    /** @return le nombre d'ennemis tués depuis le début du stage en cours */
    public int kills() {
        return kills;
    }

    /** @return le nombre de vagues apparues depuis le début du stage en cours */
    public int wavesSpawned() {
        return wavesSpawned;
    }

    /** @return {@code true} si le stage en cours est fini */
    public boolean isStageCleared() {
        return stageCleared;
    }

    /** @return les projectiles en vol (lecture seule) */
    public List<Projectile> projectiles() {
        return Collections.unmodifiableList(projectiles);
    }
}
