package game.model.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Damageable;
import game.model.entity.Player;
import game.model.event.GameEvent;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de la Lame lancée : création du projectile, cibles touchées, murs, portée. */
class ThrownBladeTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final Vec2 HAUT = new Vec2(0f, 1f);

    /** Cible fixe qui garde les coups reçus. */
    private static final class Cible implements Damageable {
        private final Vec2 position;
        private final Faction faction;
        private final List<Hit> coups = new ArrayList<>();

        Cible(Vec2 position, Faction faction) {
            this.position = position;
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
            return new Circle(position, 8f);
        }

        @Override
        public Faction faction() {
            return faction;
        }
    }

    private final Player joueur = new Player(DEPART, PlayerStats.defaults(), new Random(1L));
    private final ThrownBlade lame = new ThrownBlade();

    // Tourne le joueur vers le haut : la visée suit la souris même à l'arrêt.
    private void viserVersLeHaut(FakeWorld monde) {
        joueur.update(new PlayerIntent(Vec2.ZERO, HAUT, false, false, false, false), monde, FixedStep.DT);
    }

    // Fait voler la lame pendant un nombre de pas donné (le faux monde ne la met pas à jour seul).
    private static void voler(Projectile projectile, FakeWorld monde, int pas) {
        for (int i = 0; i < pas; i++) {
            projectile.update(monde, FixedStep.DT);
        }
    }

    @Test
    void laLamePartDevantLeJoueurDansLaDirectionVisee() {
        FakeWorld monde = FakeWorld.empty();
        viserVersLeHaut(monde);
        assertTrue(lame.tryActivate(joueur, monde));

        assertEquals(1, monde.projectiles().size());
        Projectile projectile = monde.projectiles().get(0);
        float ecart = Player.RADIUS + 4f;
        assertEquals(DEPART.x(), projectile.position().x(), 1e-4f);
        assertEquals(DEPART.y() + ecart, projectile.position().y(), 1e-4f);
        assertEquals(0f, projectile.velocity().x(), 1e-4f);
        assertEquals(ThrownBlade.SPEED, projectile.velocity().y(), 1e-4f);
    }

    @Test
    void laLameAppartientAuJoueurEtPorteSesDegats() {
        FakeWorld monde = FakeWorld.empty();
        lame.tryActivate(joueur, monde);

        Projectile projectile = monde.projectiles().get(0);
        assertEquals(Faction.PLAYER, projectile.faction());
        assertEquals(ThrownBlade.DAMAGE, projectile.damage());
        assertEquals(ThrownBlade.LIFETIME, projectile.lifetime(), 1e-4f);
        assertEquals(ThrownBlade.RADIUS, projectile.hitbox().radius(), 1e-4f);
    }

    @Test
    void laLameToucheUnEnnemiEnFaceEtPublieLeCoup() {
        FakeWorld monde = FakeWorld.empty();
        Cible ennemi = new Cible(DEPART.add(new Vec2(100f, 0f)), Faction.ENEMY);
        monde.addTarget(ennemi);
        List<GameEvent.EntityHit> recus = new ArrayList<>();
        monde.events().subscribe(event -> {
            if (event instanceof GameEvent.EntityHit coup) {
                recus.add(coup);
            }
        });

        lame.tryActivate(joueur, monde);
        Projectile projectile = monde.projectiles().get(0);
        voler(projectile, monde, 30);
        monde.events().flush();

        assertEquals(1, ennemi.coups.size());
        assertEquals(ThrownBlade.DAMAGE, ennemi.coups.get(0).damage());
        assertFalse(projectile.isAlive());
        assertEquals(1, recus.size());
    }

    @Test
    void laLameNeToucheJamaisUneCibleDuJoueur() {
        FakeWorld monde = FakeWorld.empty();
        Cible allie = new Cible(DEPART.add(new Vec2(100f, 0f)), Faction.PLAYER);
        monde.addTarget(allie);

        lame.tryActivate(joueur, monde);
        voler(monde.projectiles().get(0), monde, 30);

        assertTrue(allie.coups.isEmpty());
    }

    @Test
    void laLameSArreteContreUnMur() {
        Rect mur = new Rect(150f, 0f, 16f, 1000f);
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(mur));
        Cible derriere = new Cible(DEPART.add(new Vec2(150f, 0f)), Faction.ENEMY);
        monde.addTarget(derriere);

        lame.tryActivate(joueur, monde);
        Projectile projectile = monde.projectiles().get(0);
        voler(projectile, monde, 60);

        assertFalse(projectile.isAlive());
        assertTrue(derriere.coups.isEmpty());
    }

    @Test
    void laLameDisparaitApresSaDureeDeVie() {
        FakeWorld monde = FakeWorld.empty();
        lame.tryActivate(joueur, monde);
        Projectile projectile = monde.projectiles().get(0);

        voler(projectile, monde, 89);
        assertTrue(projectile.isAlive());
        voler(projectile, monde, 2);
        assertFalse(projectile.isAlive());
    }

    @Test
    void uneSecondeLameAttendLaFinDuTempsDeRecharge() {
        FakeWorld monde = FakeWorld.empty();
        lame.tryActivate(joueur, monde);
        assertFalse(lame.tryActivate(joueur, monde));

        lame.update(ThrownBlade.COOLDOWN);
        assertTrue(lame.tryActivate(joueur, monde));
        assertEquals(2, monde.projectiles().size());
    }
}
