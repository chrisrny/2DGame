package game.model.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.event.GameEvent;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de l'Onde de choc : portée, dégâts, recul vers l'extérieur. */
class ShockwaveTest {

    private static final Vec2 CENTRE = new Vec2(100f, 100f);

    /** Cible fixe qui garde les coups reçus. */
    private static final class Cible implements Damageable {
        private final Vec2 position;
        private final Faction faction;
        private final List<Hit> coups = new ArrayList<>();

        Cible(Vec2 position, Faction faction) {
            this.position = position;
            this.faction = faction;
        }

        @Override
        public boolean receive(Hit hit) {
            coups.add(hit);
            return true;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public Circle hitbox() {
            return new Circle(position, 8f);
        }

        @Override
        public Faction faction() {
            return faction;
        }
    }

    private final FakeWorld monde = FakeWorld.empty();
    private final Player joueur = new Player(CENTRE, PlayerStats.defaults(), new Random(1L));
    private final Shockwave onde = new Shockwave();

    private Cible cible(float dx, float dy, Faction faction) {
        Cible cible = new Cible(CENTRE.add(new Vec2(dx, dy)), faction);
        monde.addTarget(cible);
        return cible;
    }

    @Test
    void lesEnnemisDansLeRayonSontTouches() {
        Cible proche = cible(40f, 0f, Faction.ENEMY);
        Cible autreCote = cible(0f, -50f, Faction.ENEMY);
        onde.tryActivate(joueur, monde);
        assertEquals(1, proche.coups.size());
        assertEquals(Shockwave.DAMAGE, proche.coups.get(0).damage());
        assertEquals(1, autreCote.coups.size());
    }

    @Test
    void lesEnnemisAuDelaDuRayonNeSontPasTouches() {
        Cible loin = cible(Shockwave.RADIUS + 8f + 5f, 0f, Faction.ENEMY);
        onde.tryActivate(joueur, monde);
        assertTrue(loin.coups.isEmpty());
    }

    @Test
    void unAllieNEstPasTouche() {
        Cible allie = cible(20f, 0f, Faction.PLAYER);
        onde.tryActivate(joueur, monde);
        assertTrue(allie.coups.isEmpty());
    }

    @Test
    void leReculPousseVersLExterieur() {
        Cible gauche = cible(-30f, 0f, Faction.ENEMY);
        Cible haut = cible(0f, 30f, Faction.ENEMY);
        onde.tryActivate(joueur, monde);
        Vec2 reculGauche = gauche.coups.get(0).knockback();
        Vec2 reculHaut = haut.coups.get(0).knockback();
        assertEquals(-Shockwave.KNOCKBACK, reculGauche.x(), 1e-3f);
        assertEquals(0f, reculGauche.y(), 1e-3f);
        assertEquals(Shockwave.KNOCKBACK, reculHaut.y(), 1e-3f);
    }

    @Test
    void chaqueCoupPublieEntityHit() {
        List<GameEvent> recus = new ArrayList<>();
        monde.events().subscribe(recus::add);
        cible(30f, 0f, Faction.ENEMY);
        cible(-30f, 0f, Faction.ENEMY);
        onde.tryActivate(joueur, monde);
        monde.events().flush();
        assertEquals(2, recus.stream().filter(e -> e instanceof GameEvent.EntityHit).count());
    }

    @Test
    void laRechargeDureSixSecondes() {
        assertEquals(6f, onde.cooldown());
    }
}
