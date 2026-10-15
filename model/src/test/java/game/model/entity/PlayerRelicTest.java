package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.relic.AshBoots;
import game.model.relic.EmberHeart;
import game.model.relic.HunterEye;
import game.model.relic.SharpenedBlade;
import game.model.relic.VampireFang;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests du joueur avec des reliques : stats réelles, stats de base, soin et PV max. */
class PlayerRelicTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    /** Stats par défaut sans critique, pour que les dégâts soient prévisibles. */
    private static final PlayerStats SANS_CRITIQUE = PlayerStats.defaults().withCritChance(0f);

    private final FakeWorld monde = FakeWorld.empty();

    /** Cible fixe devant le joueur, qui garde les coups reçus. */
    private static final class Cible implements Damageable {
        private final List<Hit> coups = new ArrayList<>();

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
            return new Circle(DEPART.add(new Vec2(22f, 0f)), 8f);
        }

        @Override
        public Faction faction() {
            return Faction.ENEMY;
        }
    }

    private static Player joueur(PlayerStats stats) {
        return new Player(DEPART, stats, new Random(42L));
    }

    private void pas(Player joueur, PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    private void attaquer(Player joueur) {
        pas(joueur, new PlayerIntent(Vec2.ZERO, DROITE, true, false, false, false), 1);
    }

    private static Hit coup(int degats) {
        return new Hit(degats, Faction.ENEMY, false, Vec2.ZERO);
    }

    @Test
    void lesStatsReellesIntegrentLesReliquesMaisPasLesStatsDeBase() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.addRelic(new SharpenedBlade());
        joueur.addRelic(new HunterEye());
        assertEquals(12, joueur.stats().attackDamage());
        assertEquals(0.20f, joueur.stats().critChance(), 1e-4f);
        assertEquals(PlayerStats.defaults(), joueur.baseStats());
    }

    @Test
    void avecLesBottesLeJoueurMarchePlusVite() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.addRelic(new AshBoots());
        pas(joueur, new PlayerIntent(DROITE, DROITE, false, false, false, false), 60);
        assertEquals(DEPART.x() + 138f, joueur.position().x(), 1e-1f);
    }

    @Test
    void avecLaLameLeJoueurFrappePlusFort() {
        Player joueur = joueur(SANS_CRITIQUE);
        joueur.addRelic(new SharpenedBlade());
        Cible cible = new Cible();
        monde.addTarget(cible);
        attaquer(joueur);
        assertEquals(1, cible.coups.size());
        assertEquals(12, cible.coups.get(0).damage());
    }

    @Test
    void leCoeurDeBraiseAugmenteLeMaxEtSoigneDeLaMemeQuantite() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.receive(coup(30));
        joueur.addRelic(new EmberHeart());
        assertEquals(120, joueur.maxHp());
        assertEquals(90, joueur.hp());
    }

    @Test
    void leCoeurDeBraiseAPleineVieDonnePleineVie() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.addRelic(new EmberHeart());
        assertEquals(120, joueur.hp());
    }

    @Test
    void leCrocVampireSoigneQuandOnToucheUneCible() {
        Player joueur = joueur(SANS_CRITIQUE);
        joueur.addRelic(new VampireFang());
        monde.addTarget(new Cible());
        joueur.receive(coup(30));
        pas(joueur, PlayerIntent.NONE, 15);
        attaquer(joueur);
        assertEquals(71, joueur.hp());
    }

    @Test
    void leCrocVampireNeDepassePasLeMaximum() {
        Player joueur = joueur(SANS_CRITIQUE);
        joueur.addRelic(new VampireFang());
        monde.addTarget(new Cible());
        attaquer(joueur);
        assertEquals(100, joueur.hp());
    }

    @Test
    void leCrocVampireNeSoignePasUnCoupDansLeVide() {
        Player joueur = joueur(SANS_CRITIQUE);
        joueur.addRelic(new VampireFang());
        joueur.receive(coup(30));
        pas(joueur, PlayerIntent.NONE, 15);
        attaquer(joueur);
        assertEquals(70, joueur.hp());
    }

    @Test
    void unJoueurMortNEstPasSoigne() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.receive(coup(1000));
        joueur.heal(50);
        joueur.addRelic(new EmberHeart());
        assertEquals(0, joueur.hp());
    }

    @Test
    void laListeDesReliquesEstEnLectureSeule() {
        Player joueur = joueur(PlayerStats.defaults());
        joueur.addRelic(new VampireFang());
        assertEquals(1, joueur.relics().size());
        assertThrows(UnsupportedOperationException.class, () -> joueur.relics().add(new AshBoots()));
    }
}
