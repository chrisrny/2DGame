package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.ChargerBehavior;
import game.model.ai.ChargerBehavior.Phase;
import game.model.commun.FixedStep;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests du Chargeur et de son comportement. */
class ChargerTest {

    private static final Vec2 DEPART = new Vec2(500f, 500f);

    private FakeWorld monde = FakeWorld.empty();

    private Charger chargeurAvecJoueurA(float distance) {
        monde.setPlayerPosition(DEPART.add(new Vec2(distance, 0f)));
        return new Charger(DEPART);
    }

    private void avancer(Charger chargeur, float secondes) {
        int pas = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < pas; i++) {
            chargeur.update(monde, FixedStep.DT);
        }
    }

    // Avance pas à pas jusqu'à ce que le Chargeur entre dans la phase voulue (10 s au plus).
    private void avancerJusquA(Charger chargeur, Phase phase) {
        for (int i = 0; i < 600 && chargeur.phase() != phase; i++) {
            chargeur.update(monde, FixedStep.DT);
        }
        assertEquals(phase, chargeur.phase());
    }

    @Test
    void unChargeurNeufA40PvEtNeBlessePasHorsDeSaCharge() {
        Charger chargeur = new Charger(DEPART);
        assertEquals(40, chargeur.hp());
        assertEquals(Phase.APPROACH, chargeur.phase());
        assertEquals(0, chargeur.contactDamage());
    }

    @Test
    void loinDuJoueurIlApprocheA60() {
        Charger chargeur = chargeurAvecJoueurA(400f);
        avancer(chargeur, 1f);
        assertEquals(Phase.APPROACH, chargeur.phase());
        assertEquals(400f - Charger.SPEED, chargeur.position().distanceTo(monde.playerPosition()), 0.01f);
    }

    @Test
    void lesPhasesSEnchainentDansLOrdreAvecLesBonnesDurees() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        List<Phase> phases = new ArrayList<>();
        List<Integer> debuts = new ArrayList<>();
        phases.add(chargeur.phase());
        debuts.add(0);
        for (int pas = 1; pas <= 300 && phases.size() < 5; pas++) {
            chargeur.update(monde, FixedStep.DT);
            if (chargeur.phase() != phases.get(phases.size() - 1)) {
                phases.add(chargeur.phase());
                debuts.add(pas);
            }
        }
        assertEquals(List.of(Phase.APPROACH, Phase.WIND_UP, Phase.CHARGE, Phase.STUNNED, Phase.APPROACH), phases);
        assertEquals(ChargerBehavior.WIND_UP_DURATION, duree(debuts, 1), 0.001f);
        assertEquals(ChargerBehavior.CHARGE_MAX_DURATION, duree(debuts, 2), 0.001f);
        assertEquals(ChargerBehavior.STUN_DURATION, duree(debuts, 3), 0.001f);
    }

    private static float duree(List<Integer> debuts, int phase) {
        return (debuts.get(phase + 1) - debuts.get(phase)) * FixedStep.DT;
    }

    @Test
    void pendantLAnnonceIlResteImmobileEtLeSignale() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        avancer(chargeur, 0.1f);
        assertTrue(chargeur.isWindingUp());
        avancer(chargeur, 0.4f);
        assertTrue(chargeur.isWindingUp());
        assertEquals(DEPART, chargeur.position());
        assertEquals(0, chargeur.contactDamage());
    }

    @Test
    void laChargeVaEnLigneDroiteA300SurAuPlus240() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        avancerJusquA(chargeur, Phase.CHARGE);
        assertEquals(Charger.CHARGE_DAMAGE, chargeur.contactDamage());
        avancerJusquA(chargeur, Phase.STUNNED);
        float parcouru = chargeur.position().distanceTo(DEPART);
        assertEquals(ChargerBehavior.CHARGE_SPEED * ChargerBehavior.CHARGE_MAX_DURATION, parcouru, 0.5f);
        assertEquals(DEPART.y(), chargeur.position().y(), 0.001f);
    }

    @Test
    void laDirectionEstFigeeAuDebutDeLaCharge() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        avancerJusquA(chargeur, Phase.CHARGE);
        // Le joueur esquive vers le haut : la charge continue tout droit vers la droite.
        monde.setPlayerPosition(DEPART.add(new Vec2(0f, 200f)));
        avancer(chargeur, 0.3f);
        assertTrue(chargeur.position().x() > DEPART.x());
        assertEquals(DEPART.y(), chargeur.position().y(), 0.001f);
    }

    @Test
    void laChargeSArreteContreUnMurEtLeChargeurEstEtourdi() {
        monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(new Rect(650f, 0f, 10f, 1000f)));
        Charger chargeur = chargeurAvecJoueurA(100f);
        avancerJusquA(chargeur, Phase.CHARGE);
        // Sans le mur, il serait encore en charge après 0,6 s (il charge 0,8 s).
        avancer(chargeur, 0.6f);
        assertTrue(chargeur.isStunned());
        assertTrue(chargeur.position().x() + Charger.RADIUS <= 650f);
        assertEquals(0, chargeur.contactDamage());
    }

    @Test
    void etourdiIlNeBougePasPuisRepart() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        avancerJusquA(chargeur, Phase.STUNNED);
        Vec2 ici = chargeur.position();
        avancer(chargeur, 0.9f);
        assertTrue(chargeur.isStunned());
        assertEquals(ici, chargeur.position());
        avancer(chargeur, 0.2f);
        assertFalse(chargeur.isStunned());
    }

    @Test
    void leChargeurViseLeJoueurLaOuIlEstSansAnticiper() {
        Charger chargeur = chargeurAvecJoueurA(100f);
        // Le joueur s'écarte vers le haut pendant l'annonce : seul le boss anticipe.
        Vec2 vitesse = new Vec2(0f, 120f);
        for (int i = 0; i < 600 && chargeur.phase() != Phase.CHARGE; i++) {
            monde.setPlayerPosition(monde.playerPosition().add(vitesse.scale(FixedStep.DT)));
            chargeur.update(monde, FixedStep.DT);
        }
        assertEquals(Phase.CHARGE, chargeur.phase());

        Vec2 avant = chargeur.position();
        chargeur.update(monde, FixedStep.DT);
        Vec2 direction = chargeur.position().sub(avant).normalized();
        Vec2 versLeJoueur = monde.playerPosition().sub(avant).normalized();
        assertEquals(versLeJoueur.x(), direction.x(), 0.001f);
        assertEquals(versLeJoueur.y(), direction.y(), 0.001f);
    }
}
