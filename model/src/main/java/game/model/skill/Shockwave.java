package game.model.skill;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.event.GameEvent;
import game.model.world.World;

/**
 * Onde de choc : frappe tous les ennemis autour du joueur, les repousse vers l'extérieur et les
 * étourdit un court instant.
 */
public final class Shockwave extends Skill {

    /** Rayon de l'onde autour du centre du joueur, en unités. */
    public static final float RADIUS = 96f;

    /** Dégâts infligés à chaque cible touchée. */
    public static final int DAMAGE = 28;

    /** Vitesse de recul donnée aux cibles, en unités par seconde : plus fort que l'attaque de base. */
    public static final float KNOCKBACK = 400f;

    /** Temps de recharge, en secondes. */
    public static final float COOLDOWN = 5f;

    /** Durée d'étourdissement des cibles, en secondes (le boss y est insensible). */
    public static final float STUN = 0.5f;

    /** Crée l'Onde de choc. */
    public Shockwave() {
        super("Onde de choc", COOLDOWN);
    }

    @Override
    protected void activate(Player player, World world) {
        Vec2 center = player.position();
        for (Damageable target : world.targetsIn(new Circle(center, RADIUS), player.faction())) {
            Vec2 targetCenter = target.hitbox().center();
            Vec2 away = targetCenter.sub(center).normalized();
            if (away.length() == 0f) {
                away = player.facing();
            }
            Hit hit = new Hit(DAMAGE, player.faction(), false, away.scale(KNOCKBACK), STUN);
            if (target.receive(hit)) {
                world.events().publish(new GameEvent.EntityHit(targetCenter, DAMAGE, false, target.faction()));
                player.registerHit();
            }
        }
    }
}
