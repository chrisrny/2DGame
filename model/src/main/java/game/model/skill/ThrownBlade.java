package game.model.skill;

import game.model.combat.Projectile;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.World;

/**
 * Lame lancée : tire une lame vers la souris, qui frappe le premier ennemi qu'elle rencontre.
 *
 * <p>La lame est un {@link Projectile} ordinaire, le même que les flèches des archers : c'est lui
 * qui avance, s'arrête contre les murs, touche une cible de l'autre faction et publie le coup.
 * La compétence se contente de le créer devant le joueur. Comme l'Onde de choc, elle ne déclenche
 * pas les reliques.</p>
 */
public final class ThrownBlade extends Skill {

    /** Vitesse de la lame, en unités par seconde. */
    public static final float SPEED = 260f;

    /** Dégâts infligés à la cible touchée. */
    public static final int DAMAGE = 12;

    /** Rayon de la zone de collision de la lame, en unités. */
    public static final float RADIUS = 4f;

    /** Durée de vol maximale, en secondes : la portée vaut SPEED * LIFETIME. */
    public static final float LIFETIME = 1.5f;

    /** Temps de recharge, en secondes. */
    public static final float COOLDOWN = 3f;

    // La lame part juste devant le bord du joueur, pour ne pas apparaître dans son corps.
    private static final float SPAWN_GAP = 4f;

    /** Crée la Lame lancée. */
    public ThrownBlade() {
        super("Lame lancée", COOLDOWN);
    }

    @Override
    protected void activate(Player player, World world) {
        Vec2 direction = player.facing();
        Vec2 start = player.position().add(direction.scale(Player.RADIUS + SPAWN_GAP));
        world.spawnProjectile(new Projectile(start, direction.scale(SPEED), RADIUS,
                player.faction(), DAMAGE, LIFETIME));
    }
}
