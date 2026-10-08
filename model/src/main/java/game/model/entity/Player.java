package game.model.entity;

import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.player.state.DeadState;
import game.model.player.state.IdleState;
import game.model.player.state.PlayerState;
import game.model.player.state.PlayerStateKind;
import game.model.world.World;
import java.util.Objects;

/**
 * Le personnage contrôlé par le joueur.
 *
 * <p>Son comportement dépend de son état courant ({@link PlayerState}, pattern State) :
 * le joueur ne fait que détecter les appuis, suivre la visée et déléguer à cet état.
 * Quand ses points de vie tombent à zéro, il passe en état mort quel que soit son état.</p>
 */
public final class Player implements Damageable {

    /** Rayon de la zone de collision du joueur, en unités. */
    public static final float RADIUS = 8f;

    private final PlayerStats stats;
    private Vec2 position;
    private int hp;
    private PlayerState state = new IdleState();
    private Vec2 facing = new Vec2(1f, 0f);
    private PlayerIntent previousIntent = PlayerIntent.NONE;
    private boolean attackPressed;
    private boolean dodgePressed;

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
     * <p>Dans l'ordre : détecter les appuis, suivre la visée, laisser l'état courant décider
     * de l'état suivant, puis faire agir cet état.</p>
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param world  la salle où se trouve le joueur
     * @param dt     durée du pas, en secondes
     */
    public void update(PlayerIntent intent, World world, float dt) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(world, "world");
        detectPresses(intent);
        if (isAlive()) {
            facing = intent.aim();
        }
        state = Objects.requireNonNull(state.next(this, intent), "état suivant");
        state.act(this, intent, world, dt);
    }

    /**
     * Un bouton est « appuyé » au seul pas où il passe de relâché à enfoncé :
     * une attaque part une fois par clic, même si le bouton reste enfoncé.
     *
     * @param intent l'intention de ce pas
     */
    private void detectPresses(PlayerIntent intent) {
        attackPressed = intent.attack() && !previousIntent.attack();
        dodgePressed = intent.dodge() && !previousIntent.dodge();
        previousIntent = intent;
    }

    /**
     * Déplace le joueur pendant un pas. Utilisé par les états qui font bouger le joueur.
     *
     * <p>Le déplacement vaut {@code direction × vitesse × dt} : la vitesse est en unités par
     * seconde, quel que soit le nombre de pas. Une direction d'au plus 1 de long (garanti par
     * {@link PlayerIntent}) empêche d'aller plus vite en diagonale. Le joueur glisse le long des
     * murs et ne sort jamais de l'arène.</p>
     *
     * @param direction direction du déplacement, de longueur 0 à 1
     * @param world     la salle où se trouve le joueur
     * @param dt        durée du pas, en secondes
     */
    public void walk(Vec2 direction, World world, float dt) {
        Vec2 delta = direction.scale(stats.speed() * dt);
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
     * Un coup mortel fait passer le joueur en état mort. L'invincibilité et le recul
     * arrivent avec P1-A5.</p>
     */
    @Override
    public boolean receive(Hit hit) {
        Objects.requireNonNull(hit, "hit");
        if (!isAlive() || hit.source() == faction()) {
            return false;
        }
        hp = Math.max(0, hp - hit.damage());
        if (hp == 0) {
            state = new DeadState();
        }
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

    /** @return la nature de l'état courant, pour la vue */
    public PlayerStateKind stateKind() {
        return state.kind();
    }

    /** @return la direction du regard, de longueur 1 : celle de la visée au dernier pas vivant */
    public Vec2 facing() {
        return facing;
    }

    /** @return {@code true} si le bouton d'attaque vient d'être enfoncé à ce pas */
    public boolean attackPressed() {
        return attackPressed;
    }

    /** @return {@code true} si le bouton d'esquive vient d'être enfoncé à ce pas */
    public boolean dodgePressed() {
        return dodgePressed;
    }

    /** @return les statistiques du joueur */
    public PlayerStats stats() {
        return stats;
    }
}
