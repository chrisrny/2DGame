package game.model.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de l'Éventail de lames : nombre de lames, cône, dégâts à bout portant et de loin. */
class BladeFanTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final float TOLERANCE_DEGRES = 0.01f;

    /** Cible fixe qui garde les coups reçus. */
    private static final class Cible implements Damageable {
        private final Vec2 position;
        private final List<Hit> coups = new ArrayList<>();

        Cible(Vec2 position) {
            this.position = position;
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
            return new Circle(position, 8f);
        }

        @Override
        public Faction faction() {
            return Faction.ENEMY;
        }
    }

    private final FakeWorld monde = FakeWorld.empty();
    // Sans visée donnée, le joueur regarde vers la droite.
    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(1L));
    private final BladeFan eventail = new BladeFan();

    // Le faux monde ne fait pas voler les projectiles : le test s'en charge.
    private void voler(int pas) {
        for (int i = 0; i < pas; i++) {
            for (Projectile lame : monde.projectiles()) {
                lame.update(monde, FixedStep.DT);
            }
        }
    }

    private static float angleEnDegres(Vec2 v) {
        return (float) Math.toDegrees(Math.atan2(v.y(), v.x()));
    }

    @Test
    void lEventailLanceCinqLamesDuJoueur() {
        assertTrue(eventail.tryActivate(joueur, monde));

        List<Projectile> lames = monde.projectiles();
        assertEquals(BladeFan.BLADE_COUNT, lames.size());
        for (Projectile lame : lames) {
            assertEquals(Faction.PLAYER, lame.faction());
            assertEquals(BladeFan.DAMAGE, lame.damage());
            assertEquals(BladeFan.LIFETIME, lame.lifetime(), 1e-4f);
            assertEquals(BladeFan.SPEED, lame.velocity().length(), 1e-3f);
        }
    }

    @Test
    void laLameDuMilieuSuitExactementLaVisee() {
        eventail.tryActivate(joueur, monde);

        Projectile milieu = monde.projectiles().get(BladeFan.BLADE_COUNT / 2);
        assertEquals(BladeFan.SPEED, milieu.velocity().x(), 1e-3f);
        assertEquals(0f, milieu.velocity().y(), 1e-3f);
    }

    @Test
    void lesLamesSontRegulierementRepartiesSurLeCone() {
        eventail.tryActivate(joueur, monde);

        List<Projectile> lames = monde.projectiles();
        float demiCone = BladeFan.SPREAD_DEGREES / 2f;
        assertEquals(-demiCone, angleEnDegres(lames.get(0).velocity()), TOLERANCE_DEGRES);
        assertEquals(demiCone, angleEnDegres(lames.get(lames.size() - 1).velocity()), TOLERANCE_DEGRES);
        float ecart = BladeFan.SPREAD_DEGREES / (BladeFan.BLADE_COUNT - 1);
        for (int i = 1; i < lames.size(); i++) {
            float difference = angleEnDegres(lames.get(i).velocity()) - angleEnDegres(lames.get(i - 1).velocity());
            assertEquals(ecart, difference, TOLERANCE_DEGRES);
        }
    }

    @Test
    void lesLamesPartentDevantLeJoueur() {
        eventail.tryActivate(joueur, monde);

        Projectile milieu = monde.projectiles().get(BladeFan.BLADE_COUNT / 2);
        assertEquals(DEPART.x() + Player.RADIUS + 4f, milieu.position().x(), 1e-4f);
        assertEquals(DEPART.y(), milieu.position().y(), 1e-4f);
    }

    @Test
    void unEnnemiColleEncaissePlusieursLames() {
        Cible collee = new Cible(DEPART.add(new Vec2(24f, 0f)));
        monde.addTarget(collee);

        eventail.tryActivate(joueur, monde);
        voler(10);

        assertTrue(collee.coups.size() >= 3, "lames reçues : " + collee.coups.size());
    }

    @Test
    void unEnnemiLointainNeRecoitQuUneLame() {
        Cible lointaine = new Cible(DEPART.add(new Vec2(150f, 0f)));
        monde.addTarget(lointaine);

        eventail.tryActivate(joueur, monde);
        voler(60);

        assertEquals(1, lointaine.coups.size());
    }

    @Test
    void lesLamesDisparaissentApresLeurPortee() {
        eventail.tryActivate(joueur, monde);
        voler(Math.round(BladeFan.LIFETIME / FixedStep.DT) + 1);

        for (Projectile lame : monde.projectiles()) {
            assertFalse(lame.isAlive());
        }
    }

    @Test
    void unSecondEventailAttendLaFinDuTempsDeRecharge() {
        eventail.tryActivate(joueur, monde);
        assertFalse(eventail.tryActivate(joueur, monde));

        eventail.update(BladeFan.COOLDOWN);
        assertTrue(eventail.tryActivate(joueur, monde));
        assertEquals(2 * BladeFan.BLADE_COUNT, monde.projectiles().size());
    }
}
