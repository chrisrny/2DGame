package game.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.combat.Collisions;
import game.model.combat.Hit;
import game.model.commun.Faction;
import game.model.commun.FixedStep;
import game.model.commun.PlayerIntent;
import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Archer;
import game.model.entity.Enemy;
import game.model.entity.Player;
import game.model.entity.Prowler;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests des stages et des vagues sur la map. */
class StageWavesTest {

    private final EventBus events = new EventBus();
    private final List<GameEvent> recus = new ArrayList<>();
    private Player joueur;
    private GameMap map;

    StageWavesTest() {
        events.subscribe(recus::add);
        creerMap(MapLayouts.main().center(), 42L);
    }

    private void creerMap(Vec2 positionJoueur, long graine) {
        Random random = new Random(graine);
        joueur = new Player(positionJoueur, PlayerStats.defaults(), random);
        map = new GameMap(MapLayouts.main(), joueur, events, random);
    }

    private static Wave vague(float delai, int rodeurs, int archers) {
        return new Wave(Map.of(EnemyType.PROWLER, rodeurs, EnemyType.ARCHER, archers), delai);
    }

    private void avancer(float secondes) {
        int pas = Math.round(secondes / FixedStep.DT);
        for (int i = 0; i < pas; i++) {
            map.update(PlayerIntent.NONE, FixedStep.DT);
            events.flush();
        }
    }

    private void tuer(Enemy ennemi) {
        ennemi.receive(new Hit(10_000, Faction.PLAYER, false, Vec2.ZERO));
    }

    private void tuerTous() {
        List.copyOf(map.enemies()).forEach(this::tuer);
    }

    private long stagesFinis() {
        return recus.stream().filter(e -> e instanceof GameEvent.StageCleared).count();
    }

    @Test
    void sansStageAucuneVagueNApparait() {
        avancer(5f);
        assertTrue(map.enemies().isEmpty());
    }

    @Test
    void laPremiereVagueAttendSonDelai() {
        map.startStage(new StageConfig(1, List.of(vague(1f, 2, 0)), 2, 1f));
        avancer(0.9f);
        assertTrue(map.enemies().isEmpty());
        avancer(0.15f);
        assertEquals(2, map.enemies().size());
        assertTrue(map.enemies().stream().allMatch(e -> e instanceof Prowler));
    }

    @Test
    void laVagueSuivanteArriveSeulementApresLaMortDeTousEtLeDelai() {
        map.startStage(new StageConfig(1, List.of(vague(0.5f, 2, 0), vague(2f, 0, 1)), 3, 1f));
        avancer(0.6f);
        assertEquals(2, map.enemies().size());

        // Un ennemi encore vivant : pas de nouvelle vague, même longtemps après.
        tuer(map.enemies().get(0));
        avancer(3f);
        assertEquals(1, map.enemies().size());
        assertEquals(1, map.wavesSpawned());

        // Tous morts : la vague suivante attend encore son délai de 2 s.
        tuerTous();
        avancer(1.9f);
        assertTrue(map.enemies().isEmpty());
        avancer(0.15f);
        assertEquals(1, map.enemies().size());
        assertTrue(map.enemies().get(0) instanceof Archer);
        assertEquals(2, map.wavesSpawned());
    }

    @Test
    void aucunEnnemiNApparaitAMoinsDe200DuJoueur() {
        // Le joueur se tient juste sur un point d'apparition.
        creerMap(new Vec2(480f, 600f), 7L);
        map.startStage(new StageConfig(1, List.of(vague(0f, 12, 4)), 16, 1f));
        avancer(0.05f);
        assertEquals(16, map.enemies().size());
        for (Enemy ennemi : map.enemies()) {
            assertTrue(ennemi.position().distanceTo(joueur.position()) >= GameMap.MIN_SPAWN_DISTANCE);
        }
    }

    @Test
    void stageClearedEstPublieUneSeuleFoisAvecLeNumeroDuStage() {
        map.startStage(new StageConfig(2, List.of(vague(0.1f, 2, 0)), 2, 1f));
        avancer(0.2f);
        tuerTous();
        avancer(0.1f);
        assertEquals(1, stagesFinis());
        assertTrue(recus.contains(new GameEvent.StageCleared(2)));
        assertTrue(map.isStageCleared());

        // Un ennemi de plus tué après la fin : pas de second événement, et plus de vague.
        map.addEnemy(new Prowler(new Vec2(480f, 600f)));
        tuerTous();
        avancer(3f);
        assertEquals(1, stagesFinis());
        assertTrue(map.enemies().isEmpty());
    }

    @Test
    void laDerniereVagueRevientTantQueLeStageNEstPasFini() {
        map.startStage(new StageConfig(1, List.of(vague(0.1f, 2, 0)), 5, 1f));
        for (int i = 0; i < 2; i++) {
            avancer(0.2f);
            tuerTous();
        }
        avancer(0.2f);
        assertEquals(4, map.kills());
        assertEquals(3, map.wavesSpawned());
        assertEquals(0, stagesFinis());
        tuer(map.enemies().get(0));
        avancer(0.05f);
        assertEquals(1, stagesFinis());
    }

    @Test
    void lesEnnemisDUnStageSontRenforcesParSonMultiplicateur() {
        map.startStage(new StageConfig(2, List.of(vague(0f, 1, 0)), 1, 1.3f));
        avancer(0.05f);
        Enemy rodeur = map.enemies().get(0);
        assertEquals(Math.round(Prowler.MAX_HP * 1.3f), rodeur.maxHp());
        assertEquals(Math.round(Prowler.CONTACT_DAMAGE * 1.3f), rodeur.contactDamage());
    }

    @Test
    void memeGraineMemesApparitions() {
        assertEquals(apparitions(99L), apparitions(99L));
    }

    private List<Vec2> apparitions(long graine) {
        creerMap(MapLayouts.main().center(), graine);
        map.startStage(new StageConfig(1, List.of(vague(0f, 5, 3)), 8, 1f));
        avancer(0.05f);
        return map.enemies().stream().map(Enemy::position).toList();
    }

    @Test
    void uneVagueApparaitHorsDeLEcranMaisPresDuJoueur() {
        creerMap(MapLayouts.main().center(), 11L);
        map.startStage(new StageConfig(1, List.of(vague(0f, 6, 3)), 16, 1f));
        avancer(0.05f);
        assertEquals(9, map.enemies().size());
        for (Enemy ennemi : map.enemies()) {
            float distance = ennemi.position().distanceTo(joueur.position());
            assertTrue(distance >= GameMap.WAVE_SPAWN_MIN - 1e-3f, "hors de l'écran : " + distance);
            assertTrue(distance <= GameMap.WAVE_SPAWN_MAX + 1e-3f, "pas à l'autre bout de la carte : " + distance);
            assertTrue(map.walls().stream().noneMatch(mur -> Collisions.overlaps(ennemi.hitbox(), mur)),
                    "jamais dans un mur ni dans un canal");
        }
    }
}
