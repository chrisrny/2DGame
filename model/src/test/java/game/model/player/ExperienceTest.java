package game.model.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.Faction;
import game.model.commun.Vec2;
import game.model.event.EventBus;
import game.model.event.GameEvent;
import org.junit.jupiter.api.Test;

/** Tests de l'expérience : gain par ennemi tué, paliers, niveaux en file. */
class ExperienceTest {

    private final EventBus bus = new EventBus();
    private final Experience experience = new Experience(bus);

    private void tuer(int ennemis) {
        for (int i = 0; i < ennemis; i++) {
            bus.publish(new GameEvent.EntityDied(Vec2.ZERO, Faction.ENEMY));
        }
        bus.flush();
    }

    @Test
    void unJoueurCommenceAuNiveau1SansExperience() {
        assertEquals(1, experience.level());
        assertEquals(0, experience.xp());
        assertEquals(5, experience.xpToNextLevel());
    }

    @Test
    void chaqueEnnemiTueDonneUnPoint() {
        tuer(4);
        assertEquals(1, experience.level());
        assertEquals(4, experience.xp());
    }

    @Test
    void cinqEnnemisTuesFontPasserAuNiveau2() {
        tuer(5);
        assertEquals(2, experience.level());
        assertEquals(0, experience.xp());
        assertEquals(8, experience.xpToNextLevel());
        assertEquals(1, experience.pendingLevelUps());
    }

    @Test
    void lesPaliersSuivantsSont8Puis11() {
        tuer(5 + 8);
        assertEquals(3, experience.level());
        assertEquals(11, experience.xpToNextLevel());
    }

    @Test
    void laMortDuJoueurNeDonneRien() {
        bus.publish(new GameEvent.EntityDied(Vec2.ZERO, Faction.PLAYER));
        bus.flush();
        assertEquals(0, experience.xp());
    }

    @Test
    void lesAutresEvenementsSontIgnores() {
        bus.publish(new GameEvent.PlayerDied());
        bus.publish(new GameEvent.EntityHit(Vec2.ZERO, 10, false, Faction.ENEMY));
        bus.flush();
        assertEquals(0, experience.xp());
    }

    @Test
    void plusieursNiveauxGagnesDUnCoupSontMisEnFile() {
        tuer(13);
        assertEquals(2, experience.pendingLevelUps());
        assertTrue(experience.consumeLevelUp());
        assertTrue(experience.consumeLevelUp());
        assertFalse(experience.consumeLevelUp());
    }

    @Test
    void unGainNulOuNegatifNeFaitRien() {
        experience.gain(0);
        experience.gain(-3);
        assertEquals(0, experience.xp());
    }
}
