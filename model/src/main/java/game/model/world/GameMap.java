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

/**
 * La map de jeu : elle contient le joueur, les ennemis et les projectiles, et c'est par elle
 * qu'ils interagissent.
 *
 * <p>À chaque pas : le joueur bouge, puis les ennemis vivants, puis les ennemis au contact
 * frappent le joueur, puis les morts sont retirés en publiant {@code EntityDied}.
 * Les projectiles sont seulement gardés : leur déplacement arrive avec P1-B2.</p>
 */
public final class GameMap implements World {

    /** Vitesse du recul infligé au joueur par un ennemi au contact, en unités par seconde. */
    public static final float CONTACT_KNOCKBACK = 120f;

    /** Délai minimum entre deux coups de contact d'un même ennemi, en secondes. */
    public static final float CONTACT_COOLDOWN = 0.6f;

    private final MapLayout layout;
    private final Player player;
    private final EventBus events;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Map<Enemy, Float> contactCooldowns = new IdentityHashMap<>();

    /**
     * Crée une map.
     *
     * @param layout le plan de la map
     * @param player le joueur, déjà placé sur la map
     * @param events le bus d'événements de la partie
     */
    public GameMap(MapLayout layout, Player player, EventBus events) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.player = Objects.requireNonNull(player, "player");
        this.events = Objects.requireNonNull(events, "events");
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
        applyContactDamage(dt);
        removeDeadEnemies();
    }

    // Un ennemi au contact ne frappe qu'une fois par CONTACT_COOLDOWN, sinon il viderait les PV
    // du joueur en quelques pas.
    private void applyContactDamage(float dt) {
        for (Enemy enemy : enemies) {
            float cooldown = Math.max(0f, contactCooldowns.getOrDefault(enemy, 0f) - dt);
            if (cooldown == 0f && enemy.isAlive() && Collisions.overlaps(enemy.hitbox(), player.hitbox())) {
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
            }
        }
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

    /** @return les projectiles en vol (lecture seule) */
    public List<Projectile> projectiles() {
        return Collections.unmodifiableList(projectiles);
    }
}
