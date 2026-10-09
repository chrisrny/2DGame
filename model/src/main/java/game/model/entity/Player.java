package game.model.entity;

import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.event.GameEvent;
import game.model.player.state.DeadState;
import game.model.player.state.DodgingState;
import game.model.player.state.IdleState;
import game.model.player.state.PlayerState;
import game.model.player.state.PlayerStateKind;
import game.model.world.World;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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

    /** Durée d'une attaque, en secondes : le joueur reste immobile pendant ce temps. */
    public static final float ATTACK_DURATION = 0.2f;

    /** Multiplicateur des dégâts d'un coup critique. */
    public static final int CRIT_MULTIPLIER = 2;

    /** Vitesse de recul donnée aux cibles touchées, en unités par seconde. */
    public static final float KNOCKBACK_SPEED = 300f;

    private final PlayerStats stats;
    private final Random random;
    private Vec2 position;
    private int hp;
    private PlayerState state = new IdleState();
    private Vec2 facing = new Vec2(1f, 0f);
    private PlayerIntent previousIntent = PlayerIntent.NONE;
    private boolean attackPressed;
    private boolean dodgePressed;
    private float attackCooldownLeft;
    private float dodgeCooldownLeft;

    /**
     * Crée un joueur avec tous ses points de vie.
     *
     * @param position position de départ
     * @param stats    statistiques du joueur
     * @param random   source de hasard de la partie (coups critiques), partagée et initialisée
     *                 avec la graine de la partie pour que tout soit reproductible
     */
    public Player(Vec2 position, PlayerStats stats, Random random) {
        this.position = Objects.requireNonNull(position, "position");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.random = Objects.requireNonNull(random, "random");
        this.hp = stats.maxHp();
    }

    /**
     * Fait avancer le joueur d'un pas de jeu.
     *
     * <p>Dans l'ordre : décompter la recharge, détecter les appuis, suivre la visée, laisser
     * l'état courant décider de l'état suivant, puis faire agir cet état.</p>
     *
     * @param intent ce que le joueur veut faire pendant ce pas
     * @param world  la salle où se trouve le joueur
     * @param dt     durée du pas, en secondes
     */
    public void update(PlayerIntent intent, World world, float dt) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(world, "world");
        attackCooldownLeft = Math.max(0f, attackCooldownLeft - dt);
        dodgeCooldownLeft = Math.max(0f, dodgeCooldownLeft - dt);
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
        moveBy(direction.scale(stats.speed() * dt), world);
    }

    /**
     * Déplace le joueur d'un vecteur donné : il glisse le long des murs et ne sort jamais
     * de l'arène. C'est le seul endroit où la position change ; la marche et l'esquive
     * passent toutes deux par ici.
     *
     * @param delta déplacement voulu pendant ce pas
     * @param world la salle où se trouve le joueur
     */
    public void moveBy(Vec2 delta, World world) {
        Vec2 moved = Collisions.move(hitbox(), delta, world.walls());
        position = keepInside(moved, world.arena());
    }

    /**
     * Indique si une esquive peut commencer à ce pas : le bouton vient d'être enfoncé
     * et la recharge d'esquive est terminée.
     *
     * @return {@code true} si le joueur peut esquiver maintenant
     */
    public boolean canStartDodge() {
        return dodgePressed && dodgeCooldownLeft <= 0f;
    }

    /**
     * Lance une esquive et relance sa recharge. Utilisé par les états qui autorisent l'esquive.
     *
     * <p>La direction est figée au départ : celle du déplacement si le joueur bouge,
     * sinon celle du regard.</p>
     *
     * @param move direction de déplacement demandée à ce pas
     * @return l'état d'esquive dans lequel passer
     */
    public DodgingState startDodge(Vec2 move) {
        dodgeCooldownLeft = stats.dodgeCooldown();
        Vec2 direction = move.length() > 0f ? move.normalized() : facing;
        return new DodgingState(direction);
    }

    /**
     * Indique si une attaque peut commencer à ce pas : le bouton vient d'être enfoncé
     * et la recharge est terminée.
     *
     * @return {@code true} si le joueur peut attaquer maintenant
     */
    public boolean canStartAttack() {
        return attackPressed && attackCooldownLeft <= 0f;
    }

    /**
     * Zone touchée par l'attaque : un cercle juste devant le joueur, dans la direction du regard.
     *
     * @return un cercle de diamètre égal à la portée, collé au bord du joueur
     */
    public Circle attackZone() {
        float reach = stats.attackRange() / 2f;
        Vec2 center = position.add(facing.scale(RADIUS + reach));
        return new Circle(center, reach);
    }

    /**
     * Porte un coup : frappe toutes les cibles ennemies dans la zone d'attaque et relance
     * la recharge. Utilisé par l'état d'attaque, une fois par attaque.
     *
     * <p>Chaque cible tire son propre critique. Le recul pousse la cible dans la direction
     * joueur → cible. Chaque coup accepté publie un {@link GameEvent.EntityHit}, que la vue
     * utilisera pour afficher les dégâts.</p>
     *
     * @param world la salle, qui sait quelles cibles sont dans la zone
     * @return le nombre de cibles réellement touchées
     */
    public int strike(World world) {
        attackCooldownLeft = stats.attackCooldown();
        List<Damageable> targets = world.targetsIn(attackZone(), faction());
        int touched = 0;
        for (Damageable target : targets) {
            boolean critical = random.nextFloat() < stats.critChance();
            int damage = critical ? stats.attackDamage() * CRIT_MULTIPLIER : stats.attackDamage();
            Vec2 targetCenter = target.hitbox().center();
            Vec2 away = targetCenter.sub(position).normalized();
            if (away.length() == 0f) {
                away = facing;
            }
            Hit hit = new Hit(damage, faction(), critical, away.scale(KNOCKBACK_SPEED));
            if (target.receive(hit)) {
                touched++;
                world.events().publish(new GameEvent.EntityHit(targetCenter, damage, critical, target.faction()));
            }
        }
        return touched;
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
     * Les coups sont aussi ignorés quand l'état courant rend invincible (esquive).
     * Un coup mortel fait passer le joueur en état mort. L'invincibilité après un coup
     * et le recul arrivent avec P1-A5.</p>
     */
    @Override
    public boolean receive(Hit hit) {
        Objects.requireNonNull(hit, "hit");
        if (!isAlive() || hit.source() == faction() || state.invulnerable()) {
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

    /** @return le temps restant avant de pouvoir attaquer de nouveau, en secondes */
    public float attackCooldownLeft() {
        return attackCooldownLeft;
    }

    /** @return le temps restant avant de pouvoir esquiver de nouveau, en secondes */
    public float dodgeCooldownLeft() {
        return dodgeCooldownLeft;
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
