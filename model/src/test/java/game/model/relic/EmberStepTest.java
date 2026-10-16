package game.model.relic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
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

/**
 * Tests de Pas de braise, relique seule : pose des zones, brûlures, durée de vie.
 * Le joueur reste immobile, donc les trois zones se superposent.
 */
class EmberStepTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);

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
    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(1L));
    private final EmberStep braise = new EmberStep();

    private void pas(int nombre) {
        for (int i = 0; i < nombre; i++) {
            braise.update(joueur, monde, FixedStep.DT);
        }
    }

    private Cible cible(Vec2 position, Faction faction) {
        Cible cible = new Cible(position, faction);
        monde.addTarget(cible);
        return cible;
    }

    @Test
    void sansEsquiveIlNYAPasDeFeu() {
        pas(60);
        assertTrue(braise.fireZones().isEmpty());
    }

    @Test
    void uneEsquivePoseTroisZonesPendantSaDuree() {
        braise.onDodge(joueur);
        pas(1);
        assertEquals(1, braise.fireZones().size());
        pas(8);
        assertEquals(EmberStep.ZONE_COUNT, braise.fireZones().size());
        assertEquals(EmberStep.ZONE_RADIUS, braise.fireZones().get(0).radius(), 1e-4f);
    }

    @Test
    void lesZonesDisparaissentApresLeurDureeDeVie() {
        braise.onDodge(joueur);
        pas(120);
        assertTrue(braise.fireZones().isEmpty());
    }

    @Test
    void pasDeBrulureAvantLePremierIntervalle() {
        Cible ennemi = cible(DEPART, Faction.ENEMY);
        braise.onDodge(joueur);
        pas(29);
        assertTrue(ennemi.coups.isEmpty());
    }

    @Test
    void unEnnemiSousTroisZonesNeBruleQuUneFoisParIntervalle() {
        Cible ennemi = cible(DEPART, Faction.ENEMY);
        braise.onDodge(joueur);
        pas(30);
        assertEquals(1, ennemi.coups.size());
        assertEquals(EmberStep.DAMAGE_PER_TICK, ennemi.coups.get(0).damage());
    }

    @Test
    void unEnnemiQuiResteDansLeFeuBruleTroisFoisAuTotal() {
        Cible ennemi = cible(DEPART, Faction.ENEMY);
        braise.onDodge(joueur);
        pas(200);
        assertEquals(3, ennemi.coups.size());
    }

    @Test
    void laBrulureNeRepoussePasEtPublieLeCoup() {
        Cible ennemi = cible(DEPART, Faction.ENEMY);
        List<GameEvent.EntityHit> recus = new ArrayList<>();
        monde.events().subscribe(event -> {
            if (event instanceof GameEvent.EntityHit coup) {
                recus.add(coup);
            }
        });
        braise.onDodge(joueur);
        pas(30);
        monde.events().flush();

        assertEquals(0f, ennemi.coups.get(0).knockback().length(), 1e-6f);
        assertEquals(1, recus.size());
        assertEquals(EmberStep.DAMAGE_PER_TICK, recus.get(0).damage());
    }

    @Test
    void unEnnemiHorsDuFeuNeBrulePas() {
        Cible loin = cible(DEPART.add(new Vec2(100f, 0f)), Faction.ENEMY);
        braise.onDodge(joueur);
        pas(120);
        assertTrue(loin.coups.isEmpty());
    }

    @Test
    void leFeuNeBrulePasUneCibleDuJoueur() {
        Cible allie = cible(DEPART, Faction.PLAYER);
        braise.onDodge(joueur);
        pas(120);
        assertTrue(allie.coups.isEmpty());
    }
}
