package game.model.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import game.model.commun.Faction;
import game.model.commun.Vec2;
import org.junit.jupiter.api.Test;

/** Tests du coup porté : valeurs valides et étourdissement. */
class HitTest {

    @Test
    void unCoupSansEtourdissementNEtourditPas() {
        assertEquals(0f, new Hit(5, Faction.PLAYER, false, Vec2.ZERO).stun());
    }

    @Test
    void desDegatsNegatifsSontRefuses() {
        assertThrows(IllegalArgumentException.class, () -> new Hit(-1, Faction.PLAYER, false, Vec2.ZERO));
    }

    @Test
    void unEtourdissementNegatifEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Hit(5, Faction.PLAYER, false, Vec2.ZERO, -0.1f));
    }
}
