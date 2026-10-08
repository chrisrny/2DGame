package game.model.entity;

import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.World;
import java.util.Objects;

/**
 * Le personnage contrôlé par le joueur.
 *
 * <p>À chaque pas de jeu, il reçoit l'intention du joueur ({@link PlayerIntent}) et se déplace
 * en conséquence : il glisse le long des murs et ne sort jamais de l'arène. Les états
 * (attaque, esquive, coup reçu) arrivent avec P1-A2.</p>
 */
public final class Player implements Damageable {

    /** Rayon de la zone de collision du joueur, en unités. */
    public static final float RADIUS = 8f;

    private final PlayerStats stats;
    private Vec2 position;
    private int hp;

    /**
     * Crée un joueur avec tous ses points de vie.
     *
     * @param position position de départ
     * @param stats    statistiques du joueur
     */
    public Player(Vec2 position, PlayerStats stats) {
        this.position = Objects.requireNonNull(position, "position");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.hp = stats.maxHp();
    }

    /**
     * Fait avancer le joueur d'un pas de jeu.
     *
     * <p>Le déplacement vaut {@code move × vitesse × dt} : la vitesse est en unités par seconde,
     * quel que soit le nombre de pas. {@code move} ayant une longueur d'au plus 1, le joueur ne
     * va pas plus vite en diagonale.</p>
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param world  la salle où se trouve le joueur
     * @param dt     durée du pas, en secondes
     */
    public void update(PlayerIntent intent, World world, float dt) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(world, "world");
        Vec2 delta = intent.move().scale(stats.speed() * dt);
        Vec2 moved = Collisions.move(hitbox(), delta, world.walls());
        position = keepInside(moved, world.arena());
    }

    /**
     * Ramène un centre à l'intérieur de l'arène, en tenant compte du rayon.
     *
     * @param center le centre à borner
     * @param arena  les limites de l'arène (plus large et plus haute que le joueur)
     * @return le centre le plus proche où le joueur est entièrement dans l'arène
     */
    private static Vec2 keepInside(Vec2 center, Rect arena) {
        float x = Math.clamp(center.x(), arena.x() + RADIUS, arena.right() - RADIUS);
        float y = Math.clamp(center.y(), arena.y() + RADIUS, arena.top() - RADIUS);
        return new Vec2(x, y);
    }

    /**
     * Applique un coup au joueur.
     *
     * <p>Version minimale : le tir ami et les coups sur un joueur mort sont ignorés.
     * L'invincibilité et le recul arrivent avec P1-A5.</p>
     */
    @Override
    public boolean receive(Hit hit) {
        Objects.requireNonNull(hit, "hit");
        if (!isAlive() || hit.source() == faction()) {
            return false;
        }
        hp = Math.max(0, hp - hit.damage());
        return true;
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    @Override
    public Circle hitbox() {
        return new Circle(position, RADIUS);
    }

    @Override
    public Faction faction() {
        return Faction.PLAYER;
    }

    /** @return la position actuelle du centre du joueur */
    public Vec2 position() {
        return position;
    }

    /** @return les points de vie actuels */
    public int hp() {
        return hp;
    }

    /** @return les points de vie maximum */
    public int maxHp() {
        return stats.maxHp();
    }

    /** @return les statistiques du joueur */
    public PlayerStats stats() {
        return stats;
    }
}
