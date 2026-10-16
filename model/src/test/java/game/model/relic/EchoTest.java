package game.model.relic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests d'Écho, à travers le joueur : une attaque sur quatre frappe une deuxième fois. */
class EchoTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);
    /** Sans critique, pour que les dégâts soient prévisibles. */
    private static final PlayerStats SANS_CRITIQUE = PlayerStats.defaults().withCritChance(0f);
    /** Pas d'attente entre deux attaques : plus que la recharge de 0,4 s. */
    private static final int ATTENTE = 30;

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

    private final FakeWorld monde = FakeWorld.empty();
    private final Cible cible = new Cible();
    private final Player joueur = new Player(DEPART, SANS_CRITIQUE, new Random(1L));

    EchoTest() {
        monde.addTarget(cible);
        joueur.addRelic(new Echo());
    }

    private void pas(PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    private static PlayerIntent repos() {
        return new PlayerIntent(Vec2.ZERO, DROITE, false, false, false, false);
    }

    // Une attaque : le bouton est enfoncé un pas, puis relâché le temps de la recharge.
    private void attaquerPuisAttendre(int nombre) {
        for (int i = 0; i < nombre; i++) {
            pas(new PlayerIntent(Vec2.ZERO, DROITE, true, false, false, false), 1);
            pas(repos(), ATTENTE);
        }
    }

    @Test
    void troisAttaquesNeDeclenchentPasDEcho() {
        attaquerPuisAttendre(3);
        assertEquals(3, cible.coups.size());
    }

    @Test
    void laQuatriemeAttaqueFrappeDeuxFois() {
        attaquerPuisAttendre(4);
        assertEquals(5, cible.coups.size());
    }

    @Test
    void huitAttaquesDonnentDeuxEchos() {
        attaquerPuisAttendre(8);
        assertEquals(10, cible.coups.size());
    }

    @Test
    void lEchoPartExactementApresLeDelai() {
        attaquerPuisAttendre(3);
        pas(new PlayerIntent(Vec2.ZERO, DROITE, true, false, false, false), 1);
        assertEquals(4, cible.coups.size());
        int pasDuDelai = Math.round(Echo.DELAY / FixedStep.DT);
        pas(repos(), pasDuDelai - 1);
        assertEquals(4, cible.coups.size());
        pas(repos(), 1);
        assertEquals(5, cible.coups.size());
    }

    @Test
    void lEchoNeRelancePasLaRecharge() {
        attaquerPuisAttendre(3);
        pas(new PlayerIntent(Vec2.ZERO, DROITE, true, false, false, false), 1);
        int pasDuDelai = Math.round(Echo.DELAY / FixedStep.DT);
        pas(repos(), pasDuDelai);
        assertEquals(5, cible.coups.size());
        float attendu = SANS_CRITIQUE.attackCooldown() - pasDuDelai * FixedStep.DT;
        assertEquals(attendu, joueur.attackCooldownLeft(), 1e-4f);
    }

    @Test
    void pasDEchoSiLeJoueurMeurtAvant() {
        attaquerPuisAttendre(3);
        pas(new PlayerIntent(Vec2.ZERO, DROITE, true, false, false, false), 1);
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
        pas(repos(), ATTENTE);
        assertEquals(4, cible.coups.size());
    }

    @Test
    void lEchoDeclencheAussiLeCrocVampire() {
        joueur.addRelic(new VampireFang());
        joueur.receive(new Hit(20, Faction.ENEMY, false, Vec2.ZERO));
        pas(repos(), 60);
        int avant = joueur.hp();
        attaquerPuisAttendre(4);
        assertEquals(avant + 5 * VampireFang.HEAL_PER_HIT, joueur.hp());
    }
}
