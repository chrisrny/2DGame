package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Collisions;
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

/** Les ennemis se repoussent au lieu de s'empiler. */
class EnemySeparationTest {

    private static final Rect ARENE = new Rect(0f, 0f, 800f, 600f);
    private static final Rect MUR_DROIT = new Rect(784f, 16f, 16f, 568f);

    /** Chevauchement toléré : un arrondi de calcul, invisible à l'écran. */
    private static final float SANS_CHEVAUCHEMENT = 0.05f;

    private final Player joueur = new Player(new Vec2(400f, 300f), PlayerStats.defaults().withMaxHp(1_000_000), new Random(1));
    private final GameMap map = new GameMap(new MapLayout(ARENE, List.of(
            new Rect(0f, 0f, 800f, 16f), new Rect(0f, 584f, 800f, 16f), new Rect(0f, 16f, 16f, 568f), MUR_DROIT),
            List.of(new Vec2(50f, 50f))), joueur, new EventBus(), new Random(1));

    private void avancer(float secondes) {
        for (int i = 0; i < Math.round(secondes / FixedStep.DT); i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
        }
    }

    private static float plusPetitEcart(List<Enemy> ennemis) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < ennemis.size(); i++) {
            for (int j = i + 1; j < ennemis.size(); j++) {
                Enemy a = ennemis.get(i);
                Enemy b = ennemis.get(j);
                float ecart = a.position().distanceTo(b.position()) - a.hitbox().radius() - b.hitbox().radius();
                min = Math.min(min, ecart);
            }
        }
        return min;
    }

    @Test
    void deuxRodeursAuMemeEndroitSeSeparent() {
        Prowler a = new Prowler(new Vec2(100f, 300f));
        Prowler b = new Prowler(new Vec2(100f, 300f));
        map.addEnemy(a);
        map.addEnemy(b);
        avancer(0.5f);
        assertTrue(a.position().distanceTo(b.position()) > 2 * Prowler.RADIUS - 1f);
    }

    @Test
    void uneVagueQuiEncercleLeJoueurNeSeChevauchePlus() {
        List<Enemy> vague = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Prowler rodeur = new Prowler(new Vec2(100f + 10f * i, 120f + 3f * i));
            vague.add(rodeur);
            map.addEnemy(rodeur);
        }
        // Dès le premier pas, et pendant toute la poursuite : aucun chevauchement.
        for (int i = 0; i < 8 * 60; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            assertTrue(plusPetitEcart(vague) > -SANS_CHEVAUCHEMENT, "écart minimal : " + plusPetitEcart(vague));
        }
        for (Enemy e : vague) {
            assertTrue(e.position().distanceTo(joueur.position()) < 80f, "chacun a rejoint le joueur");
        }
    }

    @Test
    void unGroupeMelangeAvecDesChargesNeSeChevauchePas() {
        List<Enemy> groupe = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            Enemy ennemi = switch (i % 3) {
                case 0 -> new Charger(new Vec2(60f + 25f * i, 80f));
                case 1 -> new Archer(new Vec2(60f + 25f * i, 520f));
                default -> new Prowler(new Vec2(60f + 25f * i, 300f));
            };
            groupe.add(ennemi);
            map.addEnemy(ennemi);
        }
        for (int i = 0; i < 12 * 60; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            assertTrue(plusPetitEcart(groupe) > -SANS_CHEVAUCHEMENT, "pas " + i + ", écart minimal : " + plusPetitEcart(groupe));
        }
    }

    @Test
    void onzeRodeursApparusAuMemePointSEcartentAuPremierPas() {
        List<Enemy> groupe = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Prowler rodeur = new Prowler(new Vec2(100f, 100f));
            groupe.add(rodeur);
            map.addEnemy(rodeur);
        }
        avancer(0.1f);
        assertTrue(plusPetitEcart(groupe) > -SANS_CHEVAUCHEMENT, "écart minimal : " + plusPetitEcart(groupe));
    }

    @Test
    void laSeparationNePoussePasDansUnMur() {
        // Deux Rôdeurs empilés contre le mur de droite, le joueur juste à côté.
        Player prisContreLeMur = new Player(new Vec2(740f, 300f), PlayerStats.defaults().withMaxHp(1_000_000), new Random(1));
        GameMap contreLeMur = new GameMap(new MapLayout(ARENE, List.of(MUR_DROIT), List.of(new Vec2(50f, 50f))),
                prisContreLeMur, new EventBus(), new Random(1));
        Prowler a = new Prowler(new Vec2(775f, 300f));
        Prowler b = new Prowler(new Vec2(775f, 302f));
        contreLeMur.addEnemy(a);
        contreLeMur.addEnemy(b);
        for (int i = 0; i < 120; i++) {
            contreLeMur.update(PlayerIntent.NONE, FixedStep.DT);
            assertTrue(a.position().x() + Prowler.RADIUS <= MUR_DROIT.x() + Collisions.TOLERANCE);
            assertTrue(b.position().x() + Prowler.RADIUS <= MUR_DROIT.x() + Collisions.TOLERANCE);
        }
    }

    @Test
    void unChargeurEnPleineChargeNEstPresquePasDevie() {
        Charger chargeur = new Charger(new Vec2(250f, 300f));
        map.addEnemy(chargeur);
        // Un Rôdeur sur la trajectoire, entre le Chargeur et le joueur.
        map.addEnemy(new Prowler(new Vec2(320f, 300f)));
        float yDebut = Float.NaN;
        for (int i = 0; i < 300 && !chargeur.isStunned(); i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            if (chargeur.isCharging() && Float.isNaN(yDebut)) {
                yDebut = chargeur.position().y();
            }
        }
        assertFalse(Float.isNaN(yDebut), "le Chargeur a chargé");
        assertTrue(chargeur.isStunned());
        assertEquals(yDebut, chargeur.position().y(), 1f);
    }
}
