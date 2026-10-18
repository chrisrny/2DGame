package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.EnemyBehavior;
import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import game.model.world.World;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests de la base commune des ennemis. */
class EnemyTest {

    private static final float EPSILON = 1e-4f;
    private static final World MONDE_VIDE = FakeWorld.empty();

    private static Enemy ennemi() {
        return ennemi((self, world, dt) -> { });
    }

    private static Enemy ennemi(EnemyBehavior comportement) {
        return new Enemy(new Vec2(10f, 50f), 30, 8f, 60f, 10, comportement) { };
    }

    private static void avancer(Enemy ennemi, World monde, int pas) {
        for (int i = 0; i < pas; i++) {
            ennemi.update(monde, FixedStep.DT);
        }
    }

    private static Hit coup(Faction source, int degats, Vec2 recul) {
        return new Hit(degats, source, false, recul);
    }

    @Test
    void unCoupDUnEnnemiEstIgnore() {
        Enemy e = ennemi();
        assertFalse(e.receive(coup(Faction.ENEMY, 10, Vec2.ZERO)));
        assertEquals(30, e.hp());
    }

    @Test
    void unCoupDuJoueurRetireDesPv() {
        Enemy e = ennemi();
        assertTrue(e.receive(coup(Faction.PLAYER, 10, Vec2.ZERO)));
        assertEquals(20, e.hp());
    }

    @Test
    void aZeroPvLEnnemiEstMortEtIgnoreLesCoups() {
        Enemy e = ennemi();
        e.receive(coup(Faction.PLAYER, 50, Vec2.ZERO));
        assertEquals(0, e.hp());
        assertFalse(e.isAlive());
        assertFalse(e.receive(coup(Faction.PLAYER, 10, Vec2.ZERO)));
    }

    @Test
    void unReculDe120RepousseDe10UnitesEnTout() {
        Enemy e = ennemi();
        e.receive(coup(Faction.PLAYER, 0, new Vec2(120f, 0f)));
        avancer(e, MONDE_VIDE, 1);
        assertEquals(12f, e.position().x(), EPSILON);
        avancer(e, MONDE_VIDE, 20);
        assertEquals(20f, e.position().x(), 0.01f);
    }

    @Test
    void lEnnemiNAgitPasPendantSonRecul() {
        int[] actions = {0};
        Enemy e = ennemi((self, world, dt) -> actions[0]++);
        e.receive(coup(Faction.PLAYER, 0, new Vec2(120f, 0f)));
        avancer(e, MONDE_VIDE, 5);
        assertEquals(0, actions[0]);
        avancer(e, MONDE_VIDE, 10);
        assertTrue(actions[0] > 0);
    }

    @Test
    void unCoupSansReculNInterromptPasLEnnemi() {
        int[] actions = {0};
        Enemy e = ennemi((self, world, dt) -> actions[0]++);
        e.receive(coup(Faction.PLAYER, 5, Vec2.ZERO));
        avancer(e, MONDE_VIDE, 1);
        assertEquals(1, actions[0]);
    }

    @Test
    void leReculSArreteContreUnMur() {
        World monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(new Rect(20f, 0f, 4f, 100f)));
        Enemy e = ennemi();
        e.receive(coup(Faction.PLAYER, 0, new Vec2(120f, 0f)));
        avancer(e, monde, 20);
        assertEquals(12f, e.position().x(), EPSILON);
    }

    @Test
    void moveTowardAvanceALaVitesseDeLEnnemi() {
        Enemy e = ennemi();
        e.moveToward(new Vec2(100f, 50f), MONDE_VIDE, 0.5f);
        assertEquals(40f, e.position().x(), EPSILON);
    }

    @Test
    void moveTowardNeDepassePasLaCible() {
        Enemy e = ennemi();
        e.moveToward(new Vec2(15f, 50f), MONDE_VIDE, 0.5f);
        assertEquals(15f, e.position().x(), EPSILON);
    }

    @Test
    void unEnnemiEtourdiNAgitPlusPuisReprend() {
        int[] actions = {0};
        Enemy e = ennemi((self, world, dt) -> actions[0]++);
        e.receive(new Hit(0, Faction.PLAYER, false, Vec2.ZERO, 0.5f));
        assertTrue(e.isStunned());
        avancer(e, MONDE_VIDE, 29);
        assertEquals(0, actions[0], "aucune action pendant l'étourdissement");
        avancer(e, MONDE_VIDE, 5);
        assertFalse(e.isStunned());
        assertTrue(actions[0] > 0, "il agit de nouveau une fois réveillé");
    }

    @Test
    void unEtourdissementPlusCourtNeRaccourcitPasCeluiEnCours() {
        Enemy e = ennemi();
        e.receive(new Hit(0, Faction.PLAYER, false, Vec2.ZERO, 1f));
        e.receive(new Hit(0, Faction.PLAYER, false, Vec2.ZERO, 0.2f));
        assertEquals(1f, e.stunLeft(), EPSILON);
    }

    @Test
    void unEnnemiEtourdiSubitQuandMemeSonRecul() {
        Enemy e = ennemi();
        e.receive(new Hit(0, Faction.PLAYER, false, new Vec2(120f, 0f), 0.5f));
        avancer(e, MONDE_VIDE, 20);
        assertEquals(20f, e.position().x(), 0.01f);
    }

    @Test
    void unCoupOrdinaireNEtourditPas() {
        Enemy e = ennemi();
        e.receive(coup(Faction.PLAYER, 5, Vec2.ZERO));
        assertFalse(e.isStunned());
    }
}
