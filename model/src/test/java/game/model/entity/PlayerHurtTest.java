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
import game.model.event.GameEvent;
import game.model.player.state.PlayerStateKind;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests des coups reçus par le joueur : état touché, recul, invincibilité et mort. */
class PlayerHurtTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    private static final int PV_MAX = PlayerStats.defaults().maxHp();
    private static final int DEGATS = 10;

    private FakeWorld monde = FakeWorld.empty();
    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(42L));

    private static Hit coup(Vec2 recul) {
        return new Hit(DEGATS, Faction.ENEMY, false, recul);
    }

    private static Hit coupMortel() {
        return new Hit(1000, Faction.ENEMY, false, Vec2.ZERO);
    }

    private static PlayerIntent intention(Vec2 move, boolean attack, boolean dodge) {
        return new PlayerIntent(move, DROITE, attack, dodge, false, false);
    }

    private void pas(PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    private long morts(List<GameEvent> recus) {
        return recus.stream().filter(e -> e instanceof GameEvent.PlayerDied).count();
    }

    @Test
    void unCoupRetireDesPvEtPasseEnEtatTouche() {
        assertTrue(joueur.receive(coup(Vec2.ZERO)));
        assertEquals(PV_MAX - DEGATS, joueur.hp());
        assertEquals(PlayerStateKind.HURT, joueur.stateKind());
    }

    @Test
    void leReculRepousseDansLeSensDuCoupPuisSArrete() {
        joueur.receive(coup(new Vec2(120f, 0f)));
        pas(PlayerIntent.NONE, 20);
        Vec2 apresRecul = joueur.position();
        assertTrue(apresRecul.x() > DEPART.x() + 5f);
        assertEquals(DEPART.y(), apresRecul.y(), 1e-3f);
        pas(PlayerIntent.NONE, 10);
        assertEquals(apresRecul, joueur.position());
    }

    @Test
    void leReculNeTraversePasLesMurs() {
        monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(new Rect(110f, 0f, 20f, 1000f)));
        joueur.receive(coup(new Vec2(300f, 0f)));
        pas(PlayerIntent.NONE, 20);
        assertTrue(joueur.position().x() <= 110f - Player.RADIUS + 1e-3f);
    }

    @Test
    void pendantLEtatToucheLesIntentionsSontIgnorees() {
        joueur.receive(coup(Vec2.ZERO));
        pas(intention(DROITE, true, true), 6);
        assertEquals(PlayerStateKind.HURT, joueur.stateKind());
        assertEquals(DEPART, joueur.position());
        assertEquals(0f, joueur.attackCooldownLeft());
        assertEquals(0f, joueur.dodgeCooldownLeft());
    }

    @Test
    void aLaFinDeLEtatToucheLeJoueurRepartSIlBouge() {
        joueur.receive(coup(Vec2.ZERO));
        pas(intention(DROITE, false, false), 15);
        assertEquals(PlayerStateKind.MOVING, joueur.stateKind());
    }

    @Test
    void aLaFinDeLEtatToucheLeJoueurSArreteSinon() {
        joueur.receive(coup(Vec2.ZERO));
        pas(PlayerIntent.NONE, 15);
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind());
    }

    @Test
    void unSecondCoupPendantLInvincibiliteEstIgnore() {
        joueur.receive(coup(Vec2.ZERO));
        pas(PlayerIntent.NONE, 40);
        assertTrue(joueur.isInvulnerable());
        assertFalse(joueur.receive(coup(Vec2.ZERO)));
        assertEquals(PV_MAX - DEGATS, joueur.hp());
    }

    @Test
    void apresLInvincibiliteUnCoupPasseDeNouveau() {
        joueur.receive(coup(Vec2.ZERO));
        pas(PlayerIntent.NONE, 50);
        assertFalse(joueur.isInvulnerable());
        assertTrue(joueur.receive(coup(Vec2.ZERO)));
        assertEquals(PV_MAX - 2 * DEGATS, joueur.hp());
    }

    @Test
    void unCoupInterromptUneAttaque() {
        pas(intention(Vec2.ZERO, true, false), 1);
        assertEquals(PlayerStateKind.ATTACKING, joueur.stateKind());
        assertTrue(joueur.receive(coup(Vec2.ZERO)));
        assertEquals(PlayerStateKind.HURT, joueur.stateKind());
    }

    @Test
    void unCoupMortelPasseDirectementEnEtatMort() {
        assertTrue(joueur.receive(coupMortel()));
        assertEquals(PlayerStateKind.DEAD, joueur.stateKind());
        assertEquals(0f, joueur.invulnerableLeft());
    }

    @Test
    void laMortPublieUnSeulPlayerDied() {
        List<GameEvent> recus = new ArrayList<>();
        monde.events().subscribe(recus::add);
        joueur.receive(coupMortel());
        pas(PlayerIntent.NONE, 5);
        monde.events().flush();
        assertEquals(1, morts(recus));
        pas(PlayerIntent.NONE, 5);
        monde.events().flush();
        assertEquals(1, morts(recus));
    }

    @Test
    void unJoueurToucheMaisVivantNePubliePasPlayerDied() {
        List<GameEvent> recus = new ArrayList<>();
        monde.events().subscribe(recus::add);
        joueur.receive(coup(Vec2.ZERO));
        pas(PlayerIntent.NONE, 5);
        monde.events().flush();
        assertEquals(0, morts(recus));
    }
}
