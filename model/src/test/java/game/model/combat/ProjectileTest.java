package game.model.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.GameEvent;
import game.model.world.FakeWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests des projectiles. */
class ProjectileTest {

    private static final Vec2 DROITE = new Vec2(200f, 0f);

    private static Projectile flecheEnnemie(Vec2 depart) {
        return new Projectile(depart, DROITE, 3f, Faction.ENEMY, 8, 3f);
    }

    private static void avancer(Projectile p, FakeWorld monde, int pas) {
        for (int i = 0; i < pas; i++) {
            p.update(monde, FixedStep.DT);
        }
    }

    private static Player joueurEn(Vec2 position) {
        return new Player(position, PlayerStats.defaults(), new Random(42L));
    }

    @Test
    void leProjectileAvanceSelonSaVitesse() {
        Projectile fleche = flecheEnnemie(new Vec2(100f, 100f));
        avancer(fleche, FakeWorld.empty(), 60);
        assertEquals(300f, fleche.position().x(), 0.01f);
        assertEquals(100f, fleche.position().y(), 0f);
        assertTrue(fleche.isAlive());
    }

    @Test
    void leProjectileDisparaitALaFinDeSaDureeDeVie() {
        Projectile fleche = flecheEnnemie(new Vec2(100f, 100f));
        FakeWorld monde = FakeWorld.empty();
        avancer(fleche, monde, 179);
        assertTrue(fleche.isAlive());
        avancer(fleche, monde, 2);
        assertFalse(fleche.isAlive());
    }

    @Test
    void leProjectileDisparaitContreUnMur() {
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(new Rect(150f, 0f, 16f, 300f)));
        Projectile fleche = flecheEnnemie(new Vec2(100f, 100f));
        avancer(fleche, monde, 60);
        assertFalse(fleche.isAlive());
        assertTrue(fleche.position().x() < 150f + 16f, "il s'arrête dans le mur, pas derrière");
    }

    @Test
    void unProjectileEnnemiToucheLeJoueurPasUnAutreEnnemi() {
        FakeWorld monde = FakeWorld.empty();
        Prowler rodeur = new Prowler(new Vec2(130f, 100f));
        Player joueur = joueurEn(new Vec2(200f, 100f));
        monde.addTarget(rodeur);
        monde.addTarget(joueur);
        int pvJoueur = joueur.hp();
        Projectile fleche = flecheEnnemie(new Vec2(100f, 100f));
        avancer(fleche, monde, 60);
        assertEquals(Prowler.MAX_HP, rodeur.hp());
        assertEquals(pvJoueur - 8, joueur.hp());
        assertFalse(fleche.isAlive());
    }

    @Test
    void unProjectileNeToucheQuUneCible() {
        FakeWorld monde = FakeWorld.empty();
        Prowler a = new Prowler(new Vec2(110f, 100f));
        Prowler b = new Prowler(new Vec2(110f, 100f));
        monde.addTarget(a);
        monde.addTarget(b);
        Projectile lame = new Projectile(new Vec2(100f, 100f), DROITE, 3f, Faction.PLAYER, 10, 3f);
        avancer(lame, monde, 10);
        assertEquals(2 * Prowler.MAX_HP - 10, a.hp() + b.hp());
    }

    @Test
    void unCoupDeProjectilePublieEntityHit() {
        FakeWorld monde = FakeWorld.empty();
        List<GameEvent> recus = new ArrayList<>();
        monde.events().subscribe(recus::add);
        monde.addTarget(joueurEn(new Vec2(110f, 100f)));
        avancer(flecheEnnemie(new Vec2(100f, 100f)), monde, 5);
        monde.events().flush();
        assertTrue(recus.stream().anyMatch(e -> e instanceof GameEvent.EntityHit hit
                && hit.damage() == 8 && hit.target() == Faction.PLAYER));
    }

    @Test
    void leJoueurQuiEsquiveEstTraverse() {
        FakeWorld monde = FakeWorld.empty();
        Player joueur = joueurEn(new Vec2(100f, 100f));
        monde.addTarget(joueur);
        joueur.update(new PlayerIntent(Vec2.ZERO, new Vec2(1f, 0f), false, true, false, false), monde, FixedStep.DT);
        int pv = joueur.hp();
        Projectile fleche = flecheEnnemie(joueur.position());
        fleche.update(monde, FixedStep.DT);
        assertEquals(pv, joueur.hp());
        assertTrue(fleche.isAlive());
    }

    @Test
    void unProjectileDisparuNeBougePlus() {
        Projectile fleche = new Projectile(new Vec2(100f, 100f), DROITE, 3f, Faction.ENEMY, 8, 0.01f);
        FakeWorld monde = FakeWorld.empty();
        avancer(fleche, monde, 1);
        Vec2 position = fleche.position();
        avancer(fleche, monde, 10);
        assertEquals(position, fleche.position());
    }

    @Test
    void uneLamePercanteTraverseSesPremieresCiblesPuisDisparait() {
        FakeWorld monde = FakeWorld.empty();
        Prowler a = new Prowler(new Vec2(120f, 100f));
        Prowler b = new Prowler(new Vec2(160f, 100f));
        Prowler c = new Prowler(new Vec2(200f, 100f));
        monde.addTarget(a);
        monde.addTarget(b);
        monde.addTarget(c);
        Projectile lame = new Projectile(new Vec2(100f, 100f), DROITE, 3f, Faction.PLAYER, 10, 3f, 2);
        avancer(lame, monde, 60);
        assertEquals(Prowler.MAX_HP - 10, a.hp(), "touchée une seule fois malgré plusieurs pas au contact");
        assertEquals(Prowler.MAX_HP - 10, b.hp());
        assertEquals(Prowler.MAX_HP, c.hp(), "la troisième cible n'est pas touchée");
        assertEquals(2, lame.hits());
        assertFalse(lame.isAlive());
    }

    @Test
    void unProjectileDoitPouvoirToucherAuMoinsUneCible() {
        assertThrows(IllegalArgumentException.class,
                () -> new Projectile(Vec2.ZERO, DROITE, 3f, Faction.PLAYER, 10, 3f, 0));
    }

    @Test
    void unProjectileSurvoleLEauMaisPasUnMur() {
        Rect canal = new Rect(150f, 0f, 16f, 300f);
        Rect mur = new Rect(250f, 0f, 16f, 300f);
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(canal, mur), List.of(canal));
        Projectile fleche = flecheEnnemie(new Vec2(100f, 100f));
        avancer(fleche, monde, 20);
        assertTrue(fleche.isAlive(), "l'eau ne l'arrête pas");
        assertTrue(fleche.position().x() > canal.right(), "il est passé de l'autre côté du canal");
        avancer(fleche, monde, 40);
        assertFalse(fleche.isAlive(), "le mur, lui, l'arrête");
    }

    @Test
    void leTireurEstPrevenuACibleTouchee() {
        FakeWorld monde = FakeWorld.empty();
        monde.addTarget(new Prowler(new Vec2(120f, 100f)));
        monde.addTarget(new Prowler(new Vec2(160f, 100f)));
        int[] coups = {0};
        Projectile lame = new Projectile(new Vec2(100f, 100f), DROITE, 3f, Faction.PLAYER, 10, 3f, 2,
                () -> coups[0]++);
        avancer(lame, monde, 60);
        assertEquals(2, coups[0]);
    }
}
