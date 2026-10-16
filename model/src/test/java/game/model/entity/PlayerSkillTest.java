package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.skill.Shockwave;
import game.model.skill.BladeFan;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de l'utilisation des compétences par le joueur. */
class PlayerSkillTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 DROITE = new Vec2(1f, 0f);

    /** Cible fixe proche du joueur, qui garde les coups reçus. */
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
            return new Circle(DEPART.add(new Vec2(30f, 0f)), 8f);
        }

        @Override
        public Faction faction() {
            return Faction.ENEMY;
        }
    }

    private final FakeWorld monde = FakeWorld.empty();
    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(1L));
    private final Cible cible = new Cible();

    PlayerSkillTest() {
        monde.addTarget(cible);
    }

    private static PlayerIntent intention(boolean attack, boolean dodge, boolean skill1) {
        return new PlayerIntent(Vec2.ZERO, DROITE, attack, dodge, skill1, false);
    }

    private static PlayerIntent lancer() {
        return new PlayerIntent(Vec2.ZERO, DROITE, false, false, false, true);
    }

    private void pas(PlayerIntent intent, int nombre) {
        for (int i = 0; i < nombre; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    private long ondes() {
        return cible.coups.stream().filter(h -> h.damage() == Shockwave.DAMAGE).count();
    }

    @Test
    void leClicDroitDeclencheLOndeDeChoc() {
        pas(intention(false, false, true), 1);
        assertEquals(1, ondes());
        assertFalse(joueur.skill1().isReady());
    }

    @Test
    void unClicMaintenuNeDeclencheQuUneOnde() {
        pas(intention(false, false, true), 30);
        assertEquals(1, ondes());
    }

    @Test
    void pasDOndePendantUneAttaque() {
        pas(intention(true, false, true), 1);
        assertEquals(0, ondes());
        assertTrue(joueur.skill1().isReady());
    }

    @Test
    void pasDOndePendantUneEsquive() {
        pas(intention(false, true, false), 1);
        pas(intention(false, false, true), 1);
        assertEquals(0, ondes());
    }

    @Test
    void pasDOndeQuandOnVientDEtreTouche() {
        joueur.receive(new Hit(10, Faction.ENEMY, false, Vec2.ZERO));
        pas(intention(false, false, true), 1);
        assertEquals(0, ondes());
    }

    @Test
    void pasDOndeUneFoisMort() {
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
        pas(intention(false, false, true), 1);
        assertEquals(0, ondes());
    }

    @Test
    void pasDeDeuxiemeOndeAvantSixSecondes() {
        pas(intention(false, false, true), 1);
        pas(PlayerIntent.NONE, 300);
        pas(intention(false, false, true), 1);
        assertEquals(1, ondes());
    }

    @Test
    void apresSixSecondesUneNouvelleOndePart() {
        pas(intention(false, false, true), 1);
        pas(PlayerIntent.NONE, 370);
        pas(intention(false, false, true), 1);
        assertEquals(2, ondes());
    }

    @Test
    void leDeuxiemeEmplacementContientLEventailDeLames() {
        assertTrue(joueur.skill2().orElseThrow() instanceof BladeFan);
    }

    @Test
    void laToucheEDeclencheLEventail() {
        pas(lancer(), 1);
        assertEquals(BladeFan.BLADE_COUNT, monde.projectiles().size());
        assertFalse(joueur.skill2().orElseThrow().isReady());
        assertTrue(joueur.skill1().isReady());
    }

    @Test
    void uneToucheMaintenueNeLanceQuUnEventail() {
        pas(lancer(), 30);
        assertEquals(BladeFan.BLADE_COUNT, monde.projectiles().size());
    }

    @Test
    void pasDeLamePendantUneEsquive() {
        pas(intention(false, true, false), 1);
        pas(lancer(), 1);
        assertTrue(monde.projectiles().isEmpty());
    }

    @Test
    void pasDeLameQuandOnVientDEtreTouche() {
        joueur.receive(new Hit(10, Faction.ENEMY, false, Vec2.ZERO));
        pas(lancer(), 1);
        assertTrue(monde.projectiles().isEmpty());
    }

    @Test
    void unNouvelEventailPartApresTroisSecondes() {
        pas(lancer(), 1);
        pas(PlayerIntent.NONE, 170);
        pas(lancer(), 1);
        assertEquals(BladeFan.BLADE_COUNT, monde.projectiles().size());
        pas(PlayerIntent.NONE, 20);
        pas(lancer(), 1);
        assertEquals(2 * BladeFan.BLADE_COUNT, monde.projectiles().size());
    }
}
