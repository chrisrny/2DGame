package game.model.relic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import game.model.commun.PlayerStats;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests des reliques prises une par une : chacune change sa statistique, et seulement elle. */
class RelicTest {

    private static final PlayerStats BASE = PlayerStats.defaults();

    @Test
    void laLameAffuteeAjoute20PourcentDeDegats() {
        assertEquals(BASE.withAttackDamage(14), new SharpenedBlade().apply(BASE));   // 12 × 1,2 arrondi
    }

    @Test
    void lesBottesDeCendreAjoutent15PourcentDeVitesse() {
        PlayerStats apres = new AshBoots().apply(BASE);
        assertEquals(138f, apres.speed(), 1e-3f);
        assertEquals(BASE.withSpeed(apres.speed()), apres);
    }

    @Test
    void leCoeurDeBraiseAjoute20PvMax() {
        assertEquals(BASE.withMaxHp(120), new EmberHeart().apply(BASE));
    }

    @Test
    void laPlumeDOmbreRaccourcitLaRechargeDEsquiveDe20Pourcent() {
        PlayerStats apres = new ShadowFeather().apply(BASE);
        assertEquals(0.48f, apres.dodgeCooldown(), 1e-4f);              // 0,6 × 0,8
        assertEquals(BASE.withDodgeCooldown(apres.dodgeCooldown()), apres);
    }

    @Test
    void lOeilDuChasseurAjoute10PointsDeCritique() {
        PlayerStats apres = new HunterEye().apply(BASE);
        assertEquals(0.20f, apres.critChance(), 1e-4f);
        assertEquals(BASE.withCritChance(apres.critChance()), apres);
    }

    @Test
    void lOeilDuChasseurNeDepassePas100Pourcent() {
        PlayerStats presqueCertain = BASE.withCritChance(0.95f);
        assertEquals(1f, new HunterEye().apply(presqueCertain).critChance());
    }

    @Test
    void leCrocVampireNeChangeAucuneStatistique() {
        assertSame(BASE, new VampireFang().apply(BASE));
    }

    @Test
    void deuxReliquesSeCumulent() {
        PlayerStats apres = new HunterEye().apply(new SharpenedBlade().apply(BASE));
        assertEquals(14, apres.attackDamage());
        assertEquals(0.20f, apres.critChance(), 1e-4f);
    }

    @Test
    void appliquerUneReliqueNeModifiePasLesStatsRecues() {
        new SharpenedBlade().apply(BASE);
        assertEquals(PlayerStats.defaults(), BASE);
    }

    @Test
    void chaqueReliqueAUnNomEtUneDescription() {
        List<Relic> toutes = List.of(new SharpenedBlade(), new AshBoots(), new EmberHeart(),
                new ShadowFeather(), new HunterEye(), new VampireFang());
        for (Relic relique : toutes) {
            assertFalse(relique.name().isBlank());
            assertFalse(relique.description().isBlank());
        }
    }
}
