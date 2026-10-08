package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.event.GameEvent;
import game.model.player.state.PlayerStateKind;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de l'attaque du joueur. */
class PlayerAttackTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    /** Juste devant le joueur qui regarde à droite : dans la zone d'attaque. */
    private static final Vec2 DEVANT = new Vec2(120f, 100f);
    /** Juste derrière le joueur qui regarde à droite : hors de la zone d'attaque. */
    private static final Vec2 DERRIERE = new Vec2(80f, 100f);

    private final FakeWorld monde = FakeWorld.empty();

    /** Cible minimale qui accepte tous les coups et les garde pour le test. */
    private static final class Cible implements Damageable {
        private final Circle zone;
        private final Faction faction;
        private final List<Hit> coups = new ArrayList<>();

        Cible(Vec2 position, Faction faction) {
            this.zone = new Circle(position, 8f);
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
            return zone;
        }

        @Override
        public Faction faction() {
            return faction;
        }
    }

    private static PlayerStats statsAvecCritique(float chance) {
        PlayerStats base = PlayerStats.defaults();
        return new PlayerStats(base.speed(), base.maxHp(), base.attackDamage(), base.attackRange(),
                base.attackCooldown(), chance, base.dodgeDistance(), base.dodgeDuration(), base.dodgeCooldown());
    }

    private static Player joueur(float chanceCritique) {
        return new Player(DEPART, statsAvecCritique(chanceCritique), new Random(42L));
    }

    private static PlayerIntent intention(Vec2 move, boolean attack) {
        return new PlayerIntent(move, DROITE, attack, false, false, false);
    }

    private Cible cible(Vec2 position) {
        Cible cible = new Cible(position, Faction.ENEMY);
        monde.addTarget(cible);
        return cible;
    }

    private void pas(Player joueur, PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    /** Un clic : enfoncé pendant un pas, puis relâché. */
    private void clic(Player joueur) {
        pas(joueur, intention(Vec2.ZERO, true), 1);
        pas(joueur, intention(Vec2.ZERO, false), 1);
    }

    @Test
    void unClicPasseEnAttaque() {
        Player joueur = joueur(0f);
        pas(joueur, intention(Vec2.ZERO, true), 1);
        assertEquals(PlayerStateKind.ATTACKING, joueur.stateKind());
    }

    @Test
    void uneCibleDevantEstToucheeAvecLesDegatsDeBase() {
        Player joueur = joueur(0f);
        Cible cible = cible(DEVANT);
        clic(joueur);
        assertEquals(1, cible.coups.size());
        assertEquals(PlayerStats.defaults().attackDamage(), cible.coups.get(0).damage());
        assertFalse(cible.coups.get(0).critical());
        assertEquals(Faction.PLAYER, cible.coups.get(0).source());
    }

    @Test
    void uneCibleDerriereNEstPasTouchee() {
        Player joueur = joueur(0f);
        Cible cible = cible(DERRIERE);
        clic(joueur);
        assertTrue(cible.coups.isEmpty());
    }

    @Test
    void uneCibleAllieeNEstPasTouchee() {
        Player joueur = joueur(0f);
        Cible alliee = new Cible(DEVANT, Faction.PLAYER);
        monde.addTarget(alliee);
        clic(joueur);
        assertTrue(alliee.coups.isEmpty());
    }

    @Test
    void unClicMaintenuNeDonneQuUneAttaque() {
        Player joueur = joueur(0f);
        Cible cible = cible(DEVANT);
        pas(joueur, intention(Vec2.ZERO, true), 120);
        assertEquals(1, cible.coups.size());
    }

    @Test
    void laRechargeEmpecheUneAttaqueTropRapide() {
        Player joueur = joueur(0f);
        Cible cible = cible(DEVANT);
        clic(joueur);
        pas(joueur, intention(Vec2.ZERO, false), 10);
        clic(joueur);
        assertEquals(1, cible.coups.size(), "deuxième clic pendant la recharge ignoré");
        pas(joueur, intention(Vec2.ZERO, false), 30);
        clic(joueur);
        assertEquals(2, cible.coups.size(), "attaque possible après la recharge");
    }

    @Test
    void unCritiqueDoubleLesDegats() {
        Player joueur = joueur(1f);
        Cible cible = cible(DEVANT);
        clic(joueur);
        Hit coup = cible.coups.get(0);
        assertTrue(coup.critical());
        assertEquals(PlayerStats.defaults().attackDamage() * Player.CRIT_MULTIPLIER, coup.damage());
    }

    @Test
    void leReculPousseLaCibleLoinDuJoueur() {
        Player joueur = joueur(0f);
        Cible cible = cible(DEVANT);
        clic(joueur);
        Vec2 recul = cible.coups.get(0).knockback();
        assertTrue(recul.x() > 0f);
        assertEquals(Player.KNOCKBACK_SPEED, recul.length(), 1e-3f);
    }

    @Test
    void unCoupPublieUnEvenementDeDegats() {
        Player joueur = joueur(0f);
        cible(DEVANT);
        List<GameEvent> recus = new ArrayList<>();
        monde.events().subscribe(recus::add);
        clic(joueur);
        monde.events().flush();
        assertEquals(1, recus.size());
        GameEvent.EntityHit evenement = assertInstanceOf(GameEvent.EntityHit.class, recus.get(0));
        assertEquals(PlayerStats.defaults().attackDamage(), evenement.damage());
        assertEquals(Faction.ENEMY, evenement.target());
    }

    @Test
    void leJoueurNeBougePasPendantLAttaque() {
        Player joueur = joueur(0f);
        pas(joueur, intention(DROITE, true), 1);
        pas(joueur, intention(DROITE, false), 5);
        assertEquals(PlayerStateKind.ATTACKING, joueur.stateKind());
        assertEquals(DEPART, joueur.position());
    }

    @Test
    void aLaFinDeLAttaqueLeJoueurRevientAuRepos() {
        Player joueur = joueur(0f);
        clic(joueur);
        pas(joueur, PlayerIntent.NONE, 20);
        assertEquals(PlayerStateKind.IDLE, joueur.stateKind());
    }

    @Test
    void aLaFinDeLAttaqueLeJoueurRepartSIlBouge() {
        Player joueur = joueur(0f);
        clic(joueur);
        pas(joueur, intention(DROITE, false), 20);
        assertEquals(PlayerStateKind.MOVING, joueur.stateKind());
        assertTrue(joueur.position().x() > DEPART.x());
    }

    @Test
    void unJoueurMortNAttaquePas() {
        Player joueur = joueur(0f);
        Cible cible = cible(DEVANT);
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
        clic(joueur);
        assertTrue(cible.coups.isEmpty());
        assertEquals(PlayerStateKind.DEAD, joueur.stateKind());
    }

    @Test
    void laMemeGraineDonneLesMemesCritiques() {
        assertEquals(critiquesSur20Attaques(7L), critiquesSur20Attaques(7L));
    }

    private List<Boolean> critiquesSur20Attaques(long graine) {
        FakeWorld autreMonde = FakeWorld.empty();
        Cible cible = new Cible(DEVANT, Faction.ENEMY);
        autreMonde.addTarget(cible);
        Player joueur = new Player(DEPART, statsAvecCritique(0.5f), new Random(graine));
        for (int i = 0; i < 20; i++) {
            joueur.update(intention(Vec2.ZERO, true), autreMonde, FixedStep.DT);
            for (int j = 0; j < 30; j++) {
                joueur.update(intention(Vec2.ZERO, false), autreMonde, FixedStep.DT);
            }
        }
        return cible.coups.stream().map(Hit::critical).toList();
    }

    @Test
    void frapperUnRodeurLuiRetireDesPvEtLeRepousse() {
        Player joueur = joueur(0f);
        Prowler rodeur = new Prowler(DEVANT);
        monde.addTarget(rodeur);
        monde.setPlayerPosition(DEPART);
        clic(joueur);
        assertEquals(Prowler.MAX_HP - PlayerStats.defaults().attackDamage(), rodeur.hp());
        float distanceAvant = rodeur.position().distanceTo(DEPART);
        for (int i = 0; i < 9; i++) {
            rodeur.update(monde, FixedStep.DT);
        }
        assertTrue(rodeur.position().distanceTo(DEPART) > distanceAvant + 10f, "le Rôdeur a reculé");
    }
}
