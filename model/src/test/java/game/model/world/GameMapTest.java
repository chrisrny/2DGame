package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Hit;
import game.model.combat.Projectile;
import game.model.commun.Circle;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Charger;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de la map de jeu. */
class GameMapTest {

    private final EventBus events = new EventBus();
    private final List<GameEvent> recus = new ArrayList<>();
    private final Player joueur = new Player(MapLayouts.main().center(), PlayerStats.defaults(), new Random(42));
    private final GameMap map = new GameMap(MapLayouts.main(), joueur, events, new Random(42));

    GameMapTest() {
        events.subscribe(recus::add);
    }

    private void pas(int nombre) {
        for (int i = 0; i < nombre; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            events.flush();
        }
    }

    private Vec2 aCoteDuJoueur() {
        return joueur.position().add(new Vec2(10f, 0f));
    }

    private Hit coupMortel() {
        return new Hit(100, Faction.PLAYER, false, Vec2.ZERO);
    }

    @Test
    void targetsInNeRenvoieQueLesVivantsDeLAutreFaction() {
        Prowler vivant = new Prowler(aCoteDuJoueur());
        Prowler mort = new Prowler(aCoteDuJoueur());
        mort.receive(coupMortel());
        map.addEnemy(vivant);
        map.addEnemy(mort);
        Circle zone = new Circle(joueur.position(), 50f);
        assertEquals(List.of(vivant), map.targetsIn(zone, Faction.PLAYER));
        assertEquals(List.of(joueur), map.targetsIn(zone, Faction.ENEMY));
    }

    @Test
    void unEnnemiAuContactNeFrappeQuUneFoisParDelai() {
        map.addEnemy(new Prowler(aCoteDuJoueur()));
        int pvMax = PlayerStats.defaults().maxHp();
        pas(1);
        assertEquals(pvMax - Prowler.CONTACT_DAMAGE, joueur.hp());
        pas(10);
        assertEquals(pvMax - Prowler.CONTACT_DAMAGE, joueur.hp());
        pas(40);
        assertEquals(pvMax - 2 * Prowler.CONTACT_DAMAGE, joueur.hp());
        assertTrue(recus.stream().anyMatch(e -> e instanceof GameEvent.EntityHit));
    }

    @Test
    void unChargeurNeBlesseAuContactQuePendantSaCharge() {
        map.addEnemy(new Charger(aCoteDuJoueur()));
        int pvMax = PlayerStats.defaults().maxHp();
        pas(30);
        assertEquals(pvMax, joueur.hp(), "pendant l'annonce, le toucher est sans danger");
        pas(20);
        assertEquals(pvMax - Charger.CHARGE_DAMAGE, joueur.hp());
    }

    @Test
    void unEnnemiTueEstRetireEtPublieEntityDiedUneSeuleFois() {
        Prowler rodeur = new Prowler(new Vec2(480f, 600f));
        map.addEnemy(rodeur);
        rodeur.receive(coupMortel());
        pas(2);
        assertTrue(map.enemies().isEmpty());
        assertEquals(1, recus.stream().filter(e -> e instanceof GameEvent.EntityDied).count());
    }

    @Test
    void laMapFaitAvancerLesProjectilesEtRetireCeuxQuiTouchent() {
        Vec2 depart = joueur.position().sub(new Vec2(40f, 0f));
        map.spawnProjectile(new Projectile(depart, new Vec2(200f, 0f), 3f, Faction.ENEMY, 8, 3f));
        pas(30);
        assertTrue(map.projectiles().isEmpty());
        assertEquals(PlayerStats.defaults().maxHp() - 8, joueur.hp());
    }

    @Test
    void unArcherSurLaMapTireSurLeJoueur() {
        map.addEnemy(new Archer(joueur.position().add(new Vec2(150f, 0f))));
        pas(95);
        assertEquals(1, map.projectiles().size());
        pas(60);
        assertTrue(joueur.hp() < PlayerStats.defaults().maxHp());
    }

    @Test
    void lesMursSontCeuxDuPlan() {
        assertEquals(MapLayouts.main().walls(), map.walls());
    }

    @Test
    void leBossApparaitDansSonAreneLePlusLoinPossibleDuJoueur() {
        Player aGauche = new Player(new Vec2(500f, 600f), PlayerStats.defaults(), new Random(42));
        GameMap salle = new GameMap(MapLayouts.main(), aGauche, events, new Random(42));

        Vec2 apparition = salle.bossSpawnPoint();

        assertTrue(MapLayouts.BOSS_ARENA.contains(apparition));
        assertTrue(apparition.x() > MapLayouts.main().center().x(), "du côté opposé au joueur");
    }

    @Test
    void unEnnemiEtourdiNeBlessePasAuContact() {
        Prowler rodeur = new Prowler(aCoteDuJoueur());
        rodeur.receive(new Hit(0, Faction.PLAYER, false, Vec2.ZERO, 0.5f));
        map.addEnemy(rodeur);
        int pvMax = PlayerStats.defaults().maxHp();
        pas(20);
        assertEquals(pvMax, joueur.hp());
    }
}
