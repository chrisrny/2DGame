package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.BossFirstPhase;
import game.model.ai.BossSecondPhase;
import game.model.combat.Hit;
import game.model.combat.Projectile;
import game.model.combat.Telegraph;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests de la phase 2 du boss : même cycle qu'en phase 1, plus vite, plus large, et des braises. */
class BossSecondPhaseTest {

    private static final Vec2 DEPART = new Vec2(500f, 500f);
    private static final int PAS_MAX = 600;

    private final FakeWorld monde = FakeWorld.empty();
    private final Boss boss = new Boss(DEPART);

    BossSecondPhaseTest() {
        monde.setPlayerPosition(DEPART.add(new Vec2(300f, 0f)));
        // À la moitié de ses PV, le boss passe en phase 2 au pas suivant.
        boss.receive(new Hit(Boss.MAX_HP / 2, Faction.PLAYER, false, Vec2.ZERO));
        boss.update(monde, FixedStep.DT);
    }

    private float avancerJusquAUneAnnonce() {
        for (int i = 1; i <= PAS_MAX; i++) {
            boss.update(monde, FixedStep.DT);
            if (boss.currentAttackTelegraph().isPresent()) {
                return i * FixedStep.DT;
            }
        }
        throw new AssertionError("aucune annonce");
    }

    @Test
    void leBossEstEnPhase2() {
        assertEquals(2, boss.phaseNumber());
    }

    @Test
    void lesPausesSontPlusCourtesEtLaFrappePlusLarge() {
        float attente = avancerJusquAUneAnnonce();
        assertEquals(BossSecondPhase.PAUSE_DURATION, attente, 2 * FixedStep.DT);
        Optional<Telegraph> annonce = boss.currentAttackTelegraph();
        assertEquals(BossSecondPhase.SLAM_RADIUS, annonce.get().zone().radius(), 0.001f);
    }

    @Test
    void chaqueFrappeProjetteUnAnneauDeBraises() {
        avancerJusquAUneAnnonce();
        Vec2 impact = boss.currentAttackTelegraph().get().zone().center();
        while (boss.currentAttackTelegraph().isPresent()) {
            boss.update(monde, FixedStep.DT);
        }
        assertEquals(BossSecondPhase.EMBER_COUNT, monde.projectiles().size());
        for (Projectile braise : monde.projectiles()) {
            assertEquals(Faction.ENEMY, braise.faction());
            assertEquals(impact, braise.position(), "les braises partent du point d'impact");
            assertEquals(BossSecondPhase.EMBER_SPEED, braise.velocity().length(), 0.01f);
        }
        // Elles partent dans toutes les directions : leur somme est nulle.
        Vec2 somme = monde.projectiles().stream().map(Projectile::velocity).reduce(Vec2.ZERO, Vec2::add);
        assertTrue(somme.length() < 0.01f, somme.toString());
    }

    @Test
    void laChargeEstAnnonceeMoinsLongtemps() {
        // D'abord la frappe, puis la charge.
        avancerJusquAUneAnnonce();
        while (boss.currentAttackTelegraph().isPresent()) {
            boss.update(monde, FixedStep.DT);
        }
        avancerJusquAUneAnnonce();
        int pas = 0;
        while (boss.currentAttackTelegraph().isPresent() && pas < PAS_MAX) {
            boss.update(monde, FixedStep.DT);
            pas++;
        }

        assertEquals(BossSecondPhase.CHARGE_WIND_UP, pas * FixedStep.DT, FixedStep.DT);
        assertTrue(BossSecondPhase.CHARGE_WIND_UP < BossFirstPhase.CHARGE_WIND_UP);
    }
}
