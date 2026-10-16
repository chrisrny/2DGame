package game.model.relic;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.event.GameEvent;
import game.model.world.World;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pas de braise : chaque esquive laisse derrière le joueur une traînée de feu qui brûle les
 * ennemis qui s'y trouvent.
 *
 * <p>Pendant l'esquive, la relique pose {@link #ZONE_COUNT} zones à la position du joueur,
 * régulièrement espacées dans le temps : la traînée suit donc le vrai trajet, même si un mur
 * coupe l'esquive. Toutes les {@link #TICK} secondes, chaque ennemi présent dans au moins une
 * zone reçoit {@link #DAMAGE_PER_TICK} dégâts, une seule fois même si les zones se chevauchent.</p>
 */
public final class EmberStep extends AbstractRelic {

    /** Nombre de zones posées par esquive. */
    public static final int ZONE_COUNT = 3;

    /** Rayon d'une zone de feu, en unités. */
    public static final float ZONE_RADIUS = 12f;

    /** Durée de vie d'une zone, en secondes. */
    public static final float ZONE_LIFETIME = 1.5f;

    /** Intervalle entre deux brûlures, en secondes. */
    public static final float TICK = 0.5f;

    /** Dégâts infligés à chaque brûlure. */
    public static final int DAMAGE_PER_TICK = 3;

    // Les durées sont décomptées pas à pas en float : cette marge évite qu'un reste minuscule
    // décale un événement d'un pas.
    private static final float EPSILON = 1e-4f;

    /** Une zone de feu : sa position et le temps qu'il lui reste. */
    private static final class Zone {
        private final Circle area;
        private float timeLeft = ZONE_LIFETIME;

        private Zone(Vec2 center) {
            this.area = new Circle(center, ZONE_RADIUS);
        }
    }

    private final List<Zone> zones = new ArrayList<>();
    private int dropsLeft;
    private float nextDropIn;
    private float nextTickIn = TICK;

    /** Crée la relique. */
    public EmberStep() {
        super("Pas de braise", "L'esquive laisse du feu");
    }

    @Override
    public void onDodge(Player player) {
        dropsLeft = ZONE_COUNT;
        nextDropIn = 0f;
    }

    @Override
    public void update(Player player, World world, float dt) {
        dropZone(player, dt);
        burn(world, dt);
        for (Zone zone : zones) {
            zone.timeLeft -= dt;
        }
        zones.removeIf(zone -> zone.timeLeft <= EPSILON);
    }

    // Une zone tout de suite, puis une tous les dodgeDuration / ZONE_COUNT : les zones couvrent
    // toute la durée de l'esquive.
    private void dropZone(Player player, float dt) {
        if (dropsLeft == 0) {
            return;
        }
        if (nextDropIn <= EPSILON) {
            zones.add(new Zone(player.position()));
            dropsLeft--;
            nextDropIn = player.stats().dodgeDuration() / ZONE_COUNT;
        }
        nextDropIn -= dt;
    }

    // Sans zone, le compteur repart de zéro : la première brûlure tombe TICK après la pose.
    private void burn(World world, float dt) {
        if (zones.isEmpty()) {
            nextTickIn = TICK;
            return;
        }
        nextTickIn -= dt;
        if (nextTickIn > EPSILON) {
            return;
        }
        nextTickIn += TICK;
        Set<Damageable> burning = new LinkedHashSet<>();
        for (Zone zone : zones) {
            burning.addAll(world.targetsIn(zone.area, Faction.PLAYER));
        }
        for (Damageable target : burning) {
            Hit hit = new Hit(DAMAGE_PER_TICK, Faction.PLAYER, false, Vec2.ZERO);
            if (target.receive(hit)) {
                world.events().publish(new GameEvent.EntityHit(
                        target.hitbox().center(), DAMAGE_PER_TICK, false, target.faction()));
            }
        }
    }

    @Override
    public List<Circle> fireZones() {
        return zones.stream().map(zone -> zone.area).toList();
    }
}
