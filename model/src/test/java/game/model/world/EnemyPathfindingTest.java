package game.model.world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Rect;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Les ennemis contournent les obstacles au lieu de rester bloqués derrière. */
class EnemyPathfindingTest {

    private static final Rect ARENE = new Rect(0f, 0f, 600f, 400f);

    private static GameMap map(Player joueur, Rect... obstacles) {
        List<Rect> murs = new ArrayList<>(List.of(
                new Rect(0f, 0f, 600f, 16f), new Rect(0f, 384f, 600f, 16f),
                new Rect(0f, 16f, 16f, 368f), new Rect(584f, 16f, 16f, 368f)));
        murs.addAll(List.of(obstacles));
        return new GameMap(new MapLayout(ARENE, murs, List.of(new Vec2(50f, 50f))), joueur, new EventBus(), new Random(1));
    }

    private static Player joueurEn(Vec2 position) {
        return new Player(position, PlayerStats.defaults(), new Random(1));
    }

    // Plus petite distance atteinte entre l'ennemi et le joueur en un temps donné.
    private static float approchePendant(GameMap map, Enemy ennemi, Player joueur, float secondes) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < Math.round(secondes / FixedStep.DT); i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            min = Math.min(min, ennemi.position().distanceTo(joueur.position()));
        }
        return min;
    }

    @Test
    void leRodeurContourneUnMurAuLieuDeResterBloque() {
        // Mur vertical entre le Rôdeur et le joueur, ouvert seulement en haut.
        Player joueur = joueurEn(new Vec2(450f, 100f));
        GameMap map = map(joueur, new Rect(290f, 16f, 20f, 284f));
        Prowler rodeur = new Prowler(new Vec2(150f, 100f));
        map.addEnemy(rodeur);
        assertTrue(approchePendant(map, rodeur, joueur, 12f) < 20f, "le Rôdeur atteint le joueur");
    }

    @Test
    void leRodeurSortDUnCulDeSacEnU() {
        // Le Rôdeur est dans un U ouvert à gauche, le joueur à droite : il doit d'abord s'éloigner.
        Player joueur = joueurEn(new Vec2(500f, 200f));
        GameMap map = map(joueur,
                new Rect(200f, 120f, 160f, 16f),
                new Rect(200f, 264f, 160f, 16f),
                new Rect(344f, 120f, 16f, 160f));
        Prowler rodeur = new Prowler(new Vec2(300f, 200f));
        map.addEnemy(rodeur);
        assertTrue(approchePendant(map, rodeur, joueur, 12f) < 20f, "le Rôdeur sort du U et atteint le joueur");
    }

    @Test
    void lArcherNeTirePasATraversUnMurEtVientSeMettreEnVue() {
        Player joueur = joueurEn(new Vec2(450f, 100f));
        GameMap map = map(joueur, new Rect(290f, 16f, 20f, 284f));
        Archer archer = new Archer(new Vec2(300f - 160f, 100f));
        map.addEnemy(archer);
        map.update(PlayerIntent.NONE, FixedStep.DT);
        assertTrue(map.projectiles().isEmpty());
        boolean aTire = false;
        for (int i = 0; i < 12 * 60 && !aTire; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            aTire = !map.projectiles().isEmpty();
        }
        assertTrue(aTire, "l'Archer a contourné le mur et tiré");
        assertTrue(map.hasLineOfSight(archer.position(), joueur.position()));
    }

    @Test
    void leChargeurNAnnoncePasSaChargeDerriereUnMur() {
        Player joueur = joueurEn(new Vec2(360f, 100f));
        GameMap map = map(joueur, new Rect(290f, 16f, 20f, 284f));
        Charger chargeur = new Charger(new Vec2(240f, 100f));
        map.addEnemy(chargeur);
        for (int i = 0; i < 30; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            assertTrue(!chargeur.isWindingUp(), "pas d'annonce sans voie libre");
        }
    }
}
