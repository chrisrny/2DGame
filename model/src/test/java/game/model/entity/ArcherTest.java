package game.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.ai.ArcherBehavior;
import game.model.combat.Projectile;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.Vec2;
import game.model.world.FakeWorld;
import org.junit.jupiter.api.Test;

/** Tests de l'Archer et de son comportement. */
class ArcherTest {

    private static final Vec2 DEPART = new Vec2(500f, 500f);

    private final FakeWorld monde = FakeWorld.empty();

    private Archer archerAvecJoueurA(float distance) {
        monde.setPlayerPosition(DEPART.add(new Vec2(distance, 0f)));
        return new Archer(DEPART);
    }

    private void avancer(Archer archer, float secondes) {
        int pas = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < pas; i++) {
            archer.update(monde, FixedStep.DT);
        }
    }

    @Test
    void unArcherNeufA20Pv() {
        Archer archer = new Archer(DEPART);
        assertEquals(20, archer.hp());
        assertEquals(Faction.ENEMY, archer.faction());
    }

    @Test
    void lArcherReculeQuandLeJoueurEstA50() {
        Archer archer = archerAvecJoueurA(50f);
        avancer(archer, 0.5f);
        assertTrue(archer.position().distanceTo(monde.playerPosition()) > 50f);
        assertTrue(archer.position().x() < DEPART.x());
    }

    @Test
    void lArcherAvanceQuandLeJoueurEstTropLoin() {
        Archer archer = archerAvecJoueurA(400f);
        avancer(archer, 1f);
        assertEquals(400f - Archer.SPEED, archer.position().distanceTo(monde.playerPosition()), 0.01f);
    }

    @Test
    void lArcherNeBougePasEntre120Et180() {
        Archer archer = archerAvecJoueurA(150f);
        avancer(archer, 1f);
        assertEquals(DEPART, archer.position());
    }

    @Test
    void lArcherSeStabiliseDansSaZone() {
        Archer archer = archerAvecJoueurA(50f);
        avancer(archer, 5f);
        float distance = archer.position().distanceTo(monde.playerPosition());
        assertTrue(distance >= ArcherBehavior.MIN_DISTANCE && distance <= ArcherBehavior.MAX_DISTANCE);
    }

    @Test
    void lArcherNeTirePasAvantUnDelaiComplet() {
        Archer archer = archerAvecJoueurA(150f);
        avancer(archer, 1.4f);
        assertTrue(monde.projectiles().isEmpty());
    }

    @Test
    void lArcherTireVersLeJoueurToutesLes1Secondes5() {
        Archer archer = archerAvecJoueurA(150f);
        avancer(archer, 1.55f);
        assertEquals(1, monde.projectiles().size());
        Projectile fleche = monde.projectiles().get(0);
        assertEquals(Faction.ENEMY, fleche.faction());
        assertEquals(ArcherBehavior.PROJECTILE_DAMAGE, fleche.damage());
        assertEquals(ArcherBehavior.PROJECTILE_SPEED, fleche.velocity().length(), 0.01f);
        assertTrue(fleche.velocity().x() > 0f, "la flèche part vers le joueur, à droite");
        avancer(archer, 1.5f);
        assertEquals(2, monde.projectiles().size());
    }

    @Test
    void lArcherNeTirePasHorsDePortee() {
        monde.setPlayerPosition(new Vec2(5000f, 500f));
        Archer archer = new Archer(DEPART);
        avancer(archer, 3f);
        assertTrue(monde.projectiles().isEmpty());
    }
}
