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
 * <p>À chaque pas : le joueur bouge, puis les ennemis vivants, puis les projectiles (y compris
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
        updateProjectiles(dt);
        applyContactDamage(dt);
        removeDeadEnemies();
        updateWaves(dt);
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
