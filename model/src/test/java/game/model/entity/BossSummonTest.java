package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.BossSummons;
import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.combat.Telegraph;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests des renforts du boss : des Rôdeurs annoncés, à intervalle fixe, dans les deux phases, en nombre limité. */
class BossSummonTest {

    private static final Vec2 DEPART = new Vec2(500f, 500f);
    private static final Vec2 JOUEUR = DEPART.add(new Vec2(300f, 0f));

    /** Temps après lequel la première invocation est arrivée. */
    private static final float PREMIERE_ARRIVEE = BossSummons.FIRST_DELAY + BossSummons.WIND_UP + 0.1f;

    private static Boss boss(FakeWorld monde, float multiplicateur) {
        monde.setPlayerPosition(JOUEUR);
        return new Boss(DEPART, multiplicateur);
    }

    private static void avancer(Boss boss, FakeWorld monde, float secondes) {
        int pas = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < pas; i++) {
            boss.update(monde, FixedStep.DT);
        }
    }

    /** Avance jusqu'à la prochaine annonce d'invocation et rend le temps écoulé. */
    private static float avancerJusquALAnnonce(Boss boss, FakeWorld monde) {
        for (int i = 1; i <= 1200; i++) {
            boss.update(monde, FixedStep.DT);
            if (!boss.summonTelegraphs().isEmpty()) {
                return i * FixedStep.DT;
            }
        }
        throw new AssertionError("aucune annonce d'invocation");
    }

    private static void passerEnPhase2(Boss boss, FakeWorld monde) {
        boss.receive(new Hit(boss.maxHp() / 2, Faction.PLAYER, false, Vec2.ZERO));
        boss.update(monde, FixedStep.DT);
    }

    @Test
    void desRodeursSontAnnoncesDesLaPhase1() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        float attente = avancerJusquALAnnonce(boss, monde);
        assertEquals(1, boss.phaseNumber());
        assertEquals(BossSummons.FIRST_DELAY, attente, 2 * FixedStep.DT);
        assertEquals(BossSummons.COUNT, boss.summonTelegraphs().size());
    }

    @Test
    void rienNApparaitAvantLaFinDeLAnnonce() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        avancerJusquALAnnonce(boss, monde);
        List<Vec2> annonces = boss.summonTelegraphs().stream().map(t -> t.zone().center()).toList();
        float duree = 0f;
        while (!boss.summonTelegraphs().isEmpty()) {
            assertTrue(monde.spawned().isEmpty(), "un Rôdeur est arrivé pendant l'annonce");
            boss.update(monde, FixedStep.DT);
            duree += FixedStep.DT;
        }
        assertEquals(BossSummons.WIND_UP, duree, 2 * FixedStep.DT);
        // Les Rôdeurs arrivent exactement là où l'annonce était dessinée.
        assertEquals(annonces, monde.spawned().stream().map(Enemy::position).toList());
        assertTrue(monde.spawned().stream().allMatch(Prowler.class::isInstance));
    }

    @Test
    void uneNouvelleInvocationAChaqueIntervalle() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        avancer(boss, monde, PREMIERE_ARRIVEE);
        assertEquals(BossSummons.COUNT, monde.spawned().size());
        avancer(boss, monde, BossSummons.INTERVAL);
        assertEquals(2 * BossSummons.COUNT, monde.spawned().size());
    }

    @Test
    void lesInvocationsContinuentEnPhase2SansRepartirDeZero() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        avancer(boss, monde, PREMIERE_ARRIVEE);
        passerEnPhase2(boss, monde);
        assertEquals(2, boss.phaseNumber());
        assertEquals(BossSummons.COUNT, boss.summonedAlive(), "les Rôdeurs de la phase 1 comptent encore");
        // Le minuteur ne repart pas de zéro : pas de nouvelle invocation après FIRST_DELAY...
        avancer(boss, monde, PREMIERE_ARRIVEE);
        assertEquals(BossSummons.COUNT, monde.spawned().size());
        // ... la suivante arrive un intervalle après la première.
        avancer(boss, monde, BossSummons.INTERVAL - PREMIERE_ARRIVEE);
        assertEquals(2 * BossSummons.COUNT, monde.spawned().size());
    }

    @Test
    void jamaisPlusDeSixInvoquesEnVie() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        // Assez longtemps pour 8 invocations : sans limite, il y aurait 24 Rôdeurs.
        avancer(boss, monde, 60f);
        assertEquals(BossSummons.MAX_ALIVE, monde.spawned().size());
        assertEquals(6, boss.summonedAlive());
    }

    @Test
    void unRodeurTueLibereUnePlace() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1f);
        avancer(boss, monde, 60f);
        Enemy premier = monde.spawned().get(0);
        premier.receive(new Hit(premier.maxHp(), Faction.PLAYER, false, Vec2.ZERO));
        avancer(boss, monde, BossSummons.INTERVAL);
        assertEquals(BossSummons.MAX_ALIVE + 1, monde.spawned().size());
        assertEquals(BossSummons.MAX_ALIVE, boss.summonedAlive());
    }

    @Test
    void lesRodeursOntLeMultiplicateurDuBoss() {
        FakeWorld monde = FakeWorld.empty();
        Boss boss = boss(monde, 1.5f);
        avancer(boss, monde, PREMIERE_ARRIVEE);
        Enemy rodeur = monde.spawned().get(0);
        assertEquals(Math.round(Prowler.MAX_HP * 1.5f), rodeur.maxHp());
        assertEquals(Math.round(Prowler.CONTACT_DAMAGE * 1.5f), rodeur.contactDamage());
    }

    @Test
    void lesRodeursNApparaissentNiDansUnMurNiSurLeJoueur() {
        // Un mur juste au-dessus du boss : la première place essayée (son flanc) est bouchée.
        Rect mur = new Rect(440f, 525f, 120f, 40f);
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(mur));
        Boss boss = boss(monde, 1f);
        avancerJusquALAnnonce(boss, monde);
        List<Telegraph> annonces = boss.summonTelegraphs();
        assertEquals(BossSummons.COUNT, annonces.size());
        for (Telegraph annonce : annonces) {
            Vec2 place = annonce.zone().center();
            assertFalse(Collisions.overlaps(new Circle(place, Prowler.RADIUS), mur));
            assertTrue(place.distanceTo(monde.playerPosition()) >= BossSummons.MIN_PLAYER_DISTANCE);
        }
    }

    @Test
    void memeSituationMemesPlaces() {
        FakeWorld monde1 = FakeWorld.empty();
        FakeWorld monde2 = FakeWorld.empty();
        Boss boss1 = boss(monde1, 1f);
        Boss boss2 = boss(monde2, 1f);
        avancer(boss1, monde1, 25f);
        avancer(boss2, monde2, 25f);
        assertEquals(monde1.spawned().stream().map(Enemy::position).toList(),
                monde2.spawned().stream().map(Enemy::position).toList());
    }
}
