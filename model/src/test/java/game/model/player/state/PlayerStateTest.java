package game.model.player.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.FakeWorld;
import org.junit.jupiter.api.Test;

/** Tests des états du joueur, vus à travers le joueur. */
class PlayerStateTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    private static final Vec2 HAUT = new Vec2(0f, 1f);

    private final FakeWorld monde = FakeWorld.empty();
    private final Player joueur = new Player(DEPART, PlayerStats.defaults());

    private static PlayerIntent intention(Vec2 move, Vec2 aim, boolean attack, boolean dodge) {
        return new PlayerIntent(move, aim, attack, dodge, false, false);
    }

    private static PlayerIntent bouger(Vec2 move) {
        return intention(move, DROITE, false, false);
    }

    private static PlayerIntent attaquer(boolean enfonce) {
        return intention(Vec2.ZERO, DROITE, enfonce, false);
    }

    private void pas(PlayerIntent intent) {
        joueur.update(intent, monde, FixedStep.DT);
    }

    private void tuer() {
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
    }

    @Test
    void unJoueurNeufEstAuRepos() {
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind());
    }

    @Test
    void bougerPasseEnDeplacementEtAvanceDesLePremierPas() {
        pas(bouger(DROITE));
        assertEquals(PlayerStateKind.MOVING, joueur.stateKind());
        assertTrue(joueur.position().x() > DEPART.x());
    }

    @Test
    void lacherLesTouchesRevientAuRepos() {
        pas(bouger(DROITE));
        pas(PlayerIntent.NONE);
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind());
    }

    @Test
    void auReposLeJoueurNeBougePas() {
        pas(PlayerIntent.NONE);
        assertEquals(DEPART, joueur.position());
    }

    @Test
    void unCoupMortelPasseEnEtatMort() {
        tuer();
        assertEquals(PlayerStateKind.DEAD, joueur.stateKind());
    }

    @Test
    void unJoueurMortNeBougePlusEtResteMort() {
        tuer();
        for (int i = 0; i < 60; i++) {
            pas(bouger(DROITE));
        }
        assertEquals(DEPART, joueur.position());
        assertEquals(PlayerStateKind.DEAD, joueur.stateKind());
    }

    @Test
    void leRegardSuitLaVisee() {
        pas(intention(Vec2.ZERO, HAUT, false, false));
        assertEquals(HAUT, joueur.facing());
    }

    @Test
    void unJoueurMortGardeSonDernierRegard() {
        pas(intention(Vec2.ZERO, HAUT, false, false));
        tuer();
        pas(intention(Vec2.ZERO, DROITE, false, false));
        assertEquals(HAUT, joueur.facing());
    }

    @Test
    void unAppuiMaintenuNEstDetecteQuUneFois() {
        pas(attaquer(true));
        assertTrue(joueur.attackPressed());
        pas(attaquer(true));
        assertFalse(joueur.attackPressed());
        pas(attaquer(true));
        assertFalse(joueur.attackPressed());
    }

    @Test
    void relacherPuisRappuyerEstUnNouvelAppui() {
        pas(attaquer(true));
        pas(attaquer(false));
        assertFalse(joueur.attackPressed());
        pas(attaquer(true));
        assertTrue(joueur.attackPressed());
    }

    @Test
    void lAppuiDEsquiveEstDetecteSeparement() {
        pas(intention(Vec2.ZERO, DROITE, false, true));
        assertTrue(joueur.dodgePressed());
        assertFalse(joueur.attackPressed());
    }

    @Test
    void chaqueEntreeDansUnEtatCreeUnNouvelObjet() {
        PlayerState repos = new IdleState();
        PlayerState deplacement = repos.next(joueur, bouger(DROITE));
        PlayerState retour = deplacement.next(joueur, PlayerIntent.NONE);
        assertNotEquals(repos, retour);
        assertEquals(PlayerStateKind.IDLE, retour.kind());
    }
}
