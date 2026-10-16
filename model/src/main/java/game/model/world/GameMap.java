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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * La map de jeu : elle contient le joueur, les ennemis et les projectiles, et c'est par elle
 * qu'ils interagissent.
 *
 * <p>À chaque pas : le joueur bouge, puis les ennemis vivants, qui sont ensuite écartés pour ne
 * jamais se chevaucher, puis les projectiles (y compris
 * ceux tirés pendant ce pas), puis les ennemis au contact frappent le joueur, puis les morts
 * sont retirés en publiant {@code EntityDied}, et les projectiles disparus sont retirés.
 * Enfin, si un stage est en cours, la vague suivante apparaît quand il le faut.</p>
 *
 * <p>Vagues : quand plus aucun ennemi n'est vivant, la map attend le {@code delayBefore} de la
 * vague suivante, puis la fait apparaître sur des points d'apparition tirés au hasard, jamais
 * à moins de {@link #MIN_SPAWN_DISTANCE} du joueur. Quand le joueur a tué assez d'ennemis,
 * elle publie {@code StageCleared} une seule fois et n'envoie plus de vague.</p>
 */
public final class GameMap implements World {

    /** Vitesse du recul infligé au joueur par un ennemi au contact, en unités par seconde. */
    public static final float CONTACT_KNOCKBACK = 120f;

    /** Délai minimum entre deux coups de contact d'un même ennemi, en secondes. */
    public static final float CONTACT_COOLDOWN = 0.6f;

    /**
     * Marge gardée entre un ennemi et les murs quand il contourne un obstacle. Sans elle, il vise
     * des points qui frôlent les murs, et un arrondi de calcul suffit à le bloquer contre un coin.
     */
    public static final float PATH_MARGIN = 2f;

    /**
     * Nombre maximum de passes de séparation par pas de jeu. On s'arrête dès que plus aucun
     * ennemi n'en chevauche un autre : quelques passes suffisent d'habitude, la limite ne sert
     * que dans une foule très dense (mesuré : moins de 1,2 ms par pas avec 26 ennemis serrés).
     */
    public static final int SEPARATION_PASSES = 50;

    /** En dessous de cette distance au joueur, l'ennemi va droit sur lui sans chercher de chemin. */
    public static final float DIRECT_CHASE_DISTANCE = 2f * NavGrid.CELL;

    /** Distance minimale au joueur d'un point d'apparition : aucun ennemi n'apparaît sur lui. */
    public static final float MIN_SPAWN_DISTANCE = 200f;

    // Tolérance sur le délai des vagues : 90 pas de 1/60 s font 1,5 s à un arrondi de float près.
    private static final float EPSILON = 1e-4f;

    private final MapLayout layout;
    private final Player player;
    private final EventBus events;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Map<Enemy, Float> contactCooldowns = new IdentityHashMap<>();
    private final Random random;
    // Une grille de navigation par taille d'ennemi (rayon + marge, arrondi à l'unité) : un grand
    // ennemi ne passe pas partout où passe un petit. Créées à la première demande.
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
     * @param dt     durée du pas, en secondes
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
    // empilée en un seul rond. Écarter A de B peut pousser B dans C : on refait donc plusieurs
    // passes, jusqu'à ce que plus personne ne se chevauche (ou SEPARATION_PASSES au plus).
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
    // léger plus que le plus lourd. Si l'un est bloqué par un mur, l'autre recule du reste.
    private boolean separate(Enemy a, Enemy b, int i, int j) {
        float minDistance = a.hitbox().radius() + b.hitbox().radius();
        Vec2 delta = b.position().sub(a.position());
        float distance = delta.length();
        float overlap = minDistance - distance;
        if (overlap <= 0f) {
            return false;
        }
        // Deux ennemis exactement au même endroit (même point d'apparition) n'ont pas de
        // direction : on en choisit une qui dépend de la paire, pour rester reproductible.
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
    // reste plus petit que le rayon, sinon un ennemi pourrait traverser un mur fin. Les passes
    // suivantes finissent le travail si besoin.
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

    // On parcourt une copie : une mise à jour peut ajouter un projectile (spawnProjectile)
    // sans casser le parcours. Les projectiles disparus sont retirés à la fin du pas.
    private void updateProjectiles(float dt) {
        for (Projectile projectile : List.copyOf(projectiles)) {
            projectile.update(this, dt);
        }
        projectiles.removeIf(projectile -> !projectile.isAlive());
    }

    // Un ennemi au contact ne frappe qu'une fois par CONTACT_COOLDOWN, sinon il viderait les PV
    // du joueur en quelques pas. Un ennemi sans dégâts de contact en ce moment (le Chargeur hors
    // de sa charge) ne frappe pas : un coup à 0 mettrait quand même le joueur en état touché.
    private void applyContactDamage(float dt) {
        for (Enemy enemy : enemies) {
            float cooldown = Math.max(0f, contactCooldowns.getOrDefault(enemy, 0f) - dt);
            if (cooldown == 0f && enemy.isAlive() && enemy.contactDamage() > 0
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

    // Les points sont mélangés avec le Random de la partie : même graine, mêmes apparitions.
    // Les types sont parcourus dans l'ordre de l'enum, car l'ordre d'un Map.copyOf change
    // d'une exécution à l'autre et casserait la reproductibilité.
    private void spawn(Wave wave) {
        List<Vec2> points = spawnPointsAwayFromPlayer();
        Collections.shuffle(points, random);
        int next = 0;
        for (EnemyType type : EnemyType.values()) {
            int count = wave.enemies().getOrDefault(type, 0);
            for (int i = 0; i < count; i++) {
                addEnemy(EnemyFactory.create(type, points.get(next % points.size()), stage.multiplier()));
                next++;
            }
        }
    }

    /**
     * Tire au hasard un point d'apparition à au moins {@link #MIN_SPAWN_DISTANCE} du joueur,
     * pour un ennemi qui n'arrive pas avec une vague, comme le boss.
     *
     * @return un point d'apparition éloigné du joueur
     * @throws IllegalStateException si tous les points sont trop près du joueur
     */
    public Vec2 randomSpawnPoint() {
        List<Vec2> points = spawnPointsAwayFromPlayer();
        return points.get(random.nextInt(points.size()));
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
    public void spawnProjectile(Projectile projectile) {
        projectiles.add(Objects.requireNonNull(projectile, "projectile"));
    }

    /**
     * Va tout droit vers le joueur si rien ne gêne ; sinon, suit la grille de navigation
     * pour contourner les murs.
     */
    @Override
    public Vec2 pathTowardPlayer(Vec2 from, float radius) {
        Vec2 target = player.position();
        float body = radius + PATH_MARGIN;
        // Tout près du joueur, même collé à un mur, on fonce sur lui : le glissement le long
        // des murs suffit, et un détour l'empêcherait de le toucher.
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
