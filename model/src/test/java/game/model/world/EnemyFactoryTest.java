package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import game.model.commun.FixedStep;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Prowler;
import game.model.ai.ArcherBehavior;
import org.junit.jupiter.api.Test;

/** Tests de la fabrique d'ennemis et du multiplicateur. */
class EnemyFactoryTest {

    private static final Vec2 ICI = new Vec2(100f, 100f);

    @Test
    void chaqueTypeDonneLaBonneClasse() {
        assertInstanceOf(Prowler.class, EnemyFactory.create(EnemyType.PROWLER, ICI, 1f));
        assertInstanceOf(Archer.class, EnemyFactory.create(EnemyType.ARCHER, ICI, 1f));
        assertInstanceOf(Charger.class, EnemyFactory.create(EnemyType.CHARGER, ICI, 1f));
    }

    @Test
    void aUnLEnnemiGardeSesValeursDeReference() {
        Enemy rodeur = EnemyFactory.create(EnemyType.PROWLER, ICI, 1f);
        assertEquals(Prowler.MAX_HP, rodeur.hp());
        assertEquals(Prowler.CONTACT_DAMAGE, rodeur.contactDamage());
        assertEquals(ICI, rodeur.position());
    }

    @Test
    void leMultiplicateurSAppliqueAuxPvEtAuxDegats() {
        Enemy chargeur = EnemyFactory.create(EnemyType.CHARGER, ICI, 1.3f);
        assertEquals(52, chargeur.maxHp());
        assertEquals(52, chargeur.hp());
        assertEquals(Math.round(Charger.CHARGE_DAMAGE * 1.3f), chargeur.scaleDamage(Charger.CHARGE_DAMAGE));
        assertEquals(Prowler.SPEED, EnemyFactory.create(EnemyType.PROWLER, ICI, 2f).speed(), 0f);
    }

    @Test
    void leMultiplicateurSAppliqueAussiAuxTirsDeLArcher() {
        FakeWorld monde = FakeWorld.empty();
        monde.setPlayerPosition(ICI.add(new Vec2(150f, 0f)));
        Enemy archer = EnemyFactory.create(EnemyType.ARCHER, ICI, 2f);
        for (int i = 0; i < 100; i++) {
            archer.update(monde, FixedStep.DT);
        }
        assertEquals(2 * ArcherBehavior.PROJECTILE_DAMAGE, monde.projectiles().get(0).damage());
    }

    @Test
    void unMultiplicateurNulEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> EnemyFactory.create(EnemyType.PROWLER, ICI, 0f));
    }
}
