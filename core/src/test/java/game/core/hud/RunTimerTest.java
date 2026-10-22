package game.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests du format du chrono (mm:ss), partagé par le chrono en jeu et l'écran de fin. */
class RunTimerTest {

    @Test
    void zeroSecondeDonneDeuxFoisZero() {
        assertEquals("00:00", RunTimer.format(0f));
    }

    @Test
    void soixanteEtUneSecondesDonnentUneMinuteEtUneSeconde() {
        assertEquals("01:01", RunTimer.format(61f));
    }

    @Test
    void uneSecondeEntameeNeComptePas() {
        assertEquals("00:59", RunTimer.format(59.9f));
    }

    @Test
    void dixMinutesPilesSAffichentEnEntier() {
        assertEquals("10:00", RunTimer.format(600f));
    }

    @Test
    void uneDureeNegativeDonneZero() {
        assertEquals("00:00", RunTimer.format(-3f));
    }
}
