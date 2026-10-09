package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.player.state.PlayerStateKind;
import game.model.world.FakeWorld;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de l'esquive du joueur. */
class PlayerDodgeTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    private static final Vec2 HAUT = new Vec2(0f, 1f);
    private static final float DISTANCE = PlayerStats.defaults().dodgeDistance();
    private static final Hit COUP = new Hit(10, Faction.ENEMY, false, Vec2.ZERO);

    private FakeWorld monde = FakeWorld.empty();
    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(42L));

    private static PlayerIntent intention(Vec2 move, Vec2 aim, boolean attack, boolean dodge) {
        return new PlayerIntent(move, aim, attack, dodge, false, false);
    }

    private static PlayerIntent esquiver(Vec2 move) {
        return intention(move, DROITE, false, true);
    }

    private void pas(PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    /** Lance une esquive puis laisse passer {@code pasApres} pas sans rien toucher. */
    private void esquiveSur(Vec2 move, int pasApres) {
        pas(esquiver(move), 1);
        pas(PlayerIntent.NONE, pasApres);
    }

    @Test
    void espacePasseEnEsquive() {
        pas(esquiver(Vec2.ZERO), 1);
        assertEquals(PlayerStateKind.DODGING, joueur.stateKind());
    }

    @Test
    void enBougeantLEsquiveSuitLeDeplacement() {
        esquiveSur(DROITE, 20);
        assertEquals(DEPART.x() + DISTANCE, joueur.position().x(), 1e-3f);
        assertEquals(DEPART.y(), joueur.position().y(), 1e-3f);
    }

    @Test
    void aLArretLEsquiveSuitLeRegard() {
        pas(intention(Vec2.ZERO, HAUT, false, true), 1);
        pas(intention(Vec2.ZERO, HAUT, false, false), 20);
        assertEquals(DEPART.x(), joueur.position().x(), 1e-3f);
        assertEquals(DEPART.y() + DISTANCE, joueur.position().y(), 1e-3f);
    }

    @Test
    void enDiagonaleLEsquiveFaitLaMemeDistance() {
        esquiveSur(new Vec2(1f, 1f), 20);
        assertEquals(DISTANCE, joueur.position().distanceTo(DEPART), 1e-3f);
    }

    @Test
    void laDirectionEstFigeeAuDepart() {
        pas(esquiver(DROITE), 1);
        pas(intention(HAUT, DROITE, false, false), 5);
        assertEquals(PlayerStateKind.DODGING, joueur.stateKind());
        assertEquals(DEPART.y(), joueur.position().y(), 1e-3f);
    }

    @Test
    void leJoueurEstInvinciblePendantLEsquive() {
        pas(esquiver(DROITE), 1);
        assertFalse(joueur.receive(COUP));
        assertEquals(PlayerStats.defaults().maxHp(), joueur.hp());
    }

    @Test
    void leJoueurRedevientVulnerableApresLEsquive() {
        esquiveSur(DROITE, 20);
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind());
        assertTrue(joueur.receive(COUP));
    }

    @Test
    void aLaFinDeLEsquiveLeJoueurRepartSIlBouge() {
        pas(esquiver(DROITE), 1);
        pas(intention(DROITE, DROITE, false, false), 20);
        assertEquals(PlayerStateKind.MOVING, joueur.stateKind());
        assertTrue(joueur.position().x() > DEPART.x() + DISTANCE);
    }

    @Test
    void unEspaceMaintenuNeDonneQuUneEsquive() {
        pas(esquiver(Vec2.ZERO), 120);
        assertEquals(DEPART.x() + DISTANCE, joueur.position().x(), 1e-3f);
    }

    @Test
    void laRechargeEmpecheUneEsquiveTropRapide() {
        esquiveSur(Vec2.ZERO, 20);
        pas(esquiver(Vec2.ZERO), 1);
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind(), "esquive refusée pendant la recharge");
        pas(PlayerIntent.NONE, 40);
        pas(esquiver(Vec2.ZERO), 1);
        assertEquals(PlayerStateKind.DODGING, joueur.stateKind(), "esquive possible après la recharge");
    }

    @Test
    void unMurArreteLEsquive() {
        Rect mur = new Rect(130f, 0f, 10f, 1000f);
        monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(mur));
        esquiveSur(DROITE, 20);
        assertTrue(joueur.position().x() <= mur.x() - Player.RADIUS, "le joueur a traversé le mur");
    }

    @Test
    void lEsquivePasseAvantLAttaque() {
        pas(intention(Vec2.ZERO, DROITE, true, true), 1);
        assertEquals(PlayerStateKind.DODGING, joueur.stateKind());
    }

    @Test
    void onNePeutPasEsquiverPendantUneAttaque() {
        pas(intention(Vec2.ZERO, DROITE, true, false), 1);
        pas(esquiver(DROITE), 1);
        assertEquals(PlayerStateKind.ATTACKING, joueur.stateKind());
        assertEquals(DEPART, joueur.position());
    }

    @Test
    void unJoueurMortNEsquivePas() {
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
        esquiveSur(DROITE, 20);
        assertEquals(PlayerStateKind.DEAD, joueur.stateKind());
        assertEquals(DEPART, joueur.position());
    }
}
