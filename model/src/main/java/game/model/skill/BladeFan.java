package game.model.skill;

import game.model.combat.Projectile;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.World;

/**
 * Éventail de lames : lance {@link #BLADE_COUNT} lames en cône vers la souris.
 *
 * <p>Les lames partent toutes du même point, régulièrement réparties sur {@link #SPREAD_DEGREES}
 * degrés, la lame du milieu pile dans l'axe de la visée. Collé à un ennemi, le cône est encore
 * étroit et plusieurs lames le touchent : c'est un gros coup à bout portant. De loin, le cône
 * s'ouvre et chaque cible n'en reçoit plus qu'une. Chaque lame traverse {@link #PIERCE} ennemis :
 * une file d'ennemis est fauchée d'un coup.</p>
 *
 * <p>Chaque lame est un {@link Projectile} ordinaire, le même que les flèches des archers : il
 * avance, s'arrête contre les murs, survole l'eau, touche une cible de l'autre faction et publie le
 * coup. Comme l'Onde de choc, chaque coup de lame prévient les reliques
 * ({@link Player#registerHit()}) : le Croc vampire soigne aussi sur les lames.</p>
 */
public final class BladeFan extends Skill {

    /** Nombre de lames lancées à chaque utilisation. */
    public static final int BLADE_COUNT = 7;

    /** Ouverture du cône, en degrés, entre la première et la dernière lame. */
    public static final float SPREAD_DEGREES = 60f;

    /** Dégâts de chaque lame. */
    public static final int DAMAGE = 9;

    /** Vitesse des lames, en unités par seconde. */
    public static final float SPEED = 380f;

    /** Rayon de la zone de collision d'une lame, en unités. */
    public static final float RADIUS = 3f;

    /** Durée de vol maximale, en secondes : la portée vaut SPEED * LIFETIME. */
    public static final float LIFETIME = 0.7f;

    /** Temps de recharge, en secondes. */
    public static final float COOLDOWN = 2.5f;

    /** Nombre d'ennemis qu'une lame traverse avant de disparaître. */
    public static final int PIERCE = 2;

    // Les lames partent juste devant le bord du joueur, pour ne pas apparaître dans son corps.
    private static final float SPAWN_GAP = 4f;

    /** Crée l'Éventail de lames. */
    public BladeFan() {
        super("Éventail de lames", COOLDOWN);
    }

    @Override
    protected void activate(Player player, World world) {
        Vec2 aim = player.facing();
        Vec2 start = player.position().add(aim.scale(Player.RADIUS + SPAWN_GAP));
        float spread = (float) Math.toRadians(SPREAD_DEGREES);
        float step = spread / (BLADE_COUNT - 1);
        for (int i = 0; i < BLADE_COUNT; i++) {
            Vec2 direction = aim.rotated(-spread / 2f + i * step);
            world.spawnProjectile(new Projectile(start, direction.scale(SPEED), RADIUS,
                    player.faction(), DAMAGE, LIFETIME, PIERCE, player::registerHit));
        }
    }
}
