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
import game.model.world.FakeWorld;
import game.model.world.World;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests du joueur. */
class PlayerTest {

    private static final Vec2 DEPART = new Vec2(100f, 100f);
    private static final float VITESSE = PlayerStats.defaults().speed();

    private static Player joueur(Vec2 position) {
        return new Player(position, PlayerStats.defaults());
    }

    private static PlayerIntent bouger(float x, float y) {
        return new PlayerIntent(new Vec2(x, y), new Vec2(1f, 0f), false, false, false, false);
    }

    /** Fait avancer le joueur d'une seconde : 60 pas de jeu. */
    private static void uneSeconde(Player joueur, PlayerIntent intent, World monde) {
        for (int i = 0; i < 60; i++) {
            joueur.update(intent, monde, FixedStep.DT);
        }
    }

    @Test
    void unJoueurNeufALaVieMaximum() {
        Player joueur = joueur(DEPART);
        assertEquals(PlayerStats.defaults().maxHp(), joueur.hp());
        assertTrue(joueur.isAlive());
        assertEquals(Faction.PLAYER, joueur.faction());
    }

    @Test
    void sansIntentionLeJoueurNeBougePas() {
        Player joueur = joueur(DEPART);
        uneSeconde(joueur, PlayerIntent.NONE, FakeWorld.empty());
        assertEquals(DEPART, joueur.position());
    }

    @Test
    void enUneSecondeLeJoueurParcourtSaVitesse() {
        Player joueur = joueur(DEPART);
        uneSeconde(joueur, bouger(1f, 0f), FakeWorld.empty());
        assertEquals(DEPART.x() + VITESSE, joueur.position().x(), 1e-2f);
        assertEquals(DEPART.y(), joueur.position().y(), 1e-3f);
    }

    @Test
    void enDiagonaleLeJoueurNeVaPasPlusVite() {
        Player joueur = joueur(DEPART);
        uneSeconde(joueur, bouger(1f, 1f), FakeWorld.empty());
        assertEquals(VITESSE, joueur.position().distanceTo(DEPART), 1e-2f);
    }

    @Test
    void unMurArreteLeJoueur() {
        Rect mur = new Rect(150f, 0f, 10f, 1000f);
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 1000f, 1000f), List.of(mur));
        Player joueur = joueur(DEPART);
        uneSeconde(joueur, bouger(1f, 0f), monde);
        float bordGaucheDuMur = mur.x() - Player.RADIUS;
        assertTrue(joueur.position().x() <= bordGaucheDuMur, "le joueur est entré dans le mur");
        assertTrue(joueur.position().x() > bordGaucheDuMur - 3f, "le joueur s'est arrêté trop tôt");
    }

    @Test
    void leJoueurNeSortPasDeLArene() {
        FakeWorld monde = new FakeWorld(new Rect(0f, 0f, 200f, 200f), List.of());
        Player joueur = joueur(new Vec2(190f, 100f));
        uneSeconde(joueur, bouger(1f, 0f), monde);
        assertEquals(200f - Player.RADIUS, joueur.position().x(), 1e-3f);
    }

    @Test
    void unCoupEnnemiRetireDesPointsDeVie() {
        Player joueur = joueur(DEPART);
        assertTrue(joueur.receive(new Hit(30, Faction.ENEMY, false, Vec2.ZERO)));
        assertEquals(PlayerStats.defaults().maxHp() - 30, joueur.hp());
    }

    @Test
    void leTirAmiEstIgnore() {
        Player joueur = joueur(DEPART);
        assertFalse(joueur.receive(new Hit(30, Faction.PLAYER, false, Vec2.ZERO)));
        assertEquals(PlayerStats.defaults().maxHp(), joueur.hp());
    }

    @Test
    void unJoueurMortIgnoreLesCoups() {
        Player joueur = joueur(DEPART);
        joueur.receive(new Hit(1000, Faction.ENEMY, false, Vec2.ZERO));
        assertEquals(0, joueur.hp());
        assertFalse(joueur.isAlive());
        assertFalse(joueur.receive(new Hit(10, Faction.ENEMY, false, Vec2.ZERO)));
    }
}
