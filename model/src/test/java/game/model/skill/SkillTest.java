package game.model.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.model.commun.PlayerStats;
import game.model.commun.Vec2;
import game.model.entity.Player;
import game.model.world.FakeWorld;
import game.model.world.World;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de la recharge commune à toutes les compétences. */
class SkillTest {

    /** Compétence de test qui compte ses utilisations. */
    private static final class Compteur extends Skill {
        int utilisations;

        Compteur(float recharge) {
            super("Compteur", recharge);
        }

        @Override
        protected void activate(Player player, World world) {
            utilisations++;
        }
    }

    private final Player joueur = new Player(new Vec2(100f, 100f), PlayerStats.defaults(), new Random(1L));
    private final FakeWorld monde = FakeWorld.empty();
    private final Compteur competence = new Compteur(2f);

    @Test
    void uneCompetenceNeuveEstPrete() {
        assertTrue(competence.isReady());
        assertEquals(0f, competence.cooldownRatio());
    }

    @Test
    void utiliserProduitLEffetEtLanceLaRecharge() {
        assertTrue(competence.tryActivate(joueur, monde));
        assertEquals(1, competence.utilisations);
        assertFalse(competence.isReady());
        assertEquals(1f, competence.cooldownRatio());
    }

    @Test
    void pendantLaRechargeLaCompetenceEstRefusee() {
        competence.tryActivate(joueur, monde);
        competence.update(1f);
        assertFalse(competence.tryActivate(joueur, monde));
        assertEquals(1, competence.utilisations);
        assertEquals(0.5f, competence.cooldownRatio(), 1e-4f);
    }

    @Test
    void apresLaRechargeLaCompetenceEstAcceptee() {
        competence.tryActivate(joueur, monde);
        competence.update(2f);
        assertTrue(competence.tryActivate(joueur, monde));
        assertEquals(2, competence.utilisations);
    }

    @Test
    void leTempsDepuisLaDerniereUtilisationRepartDeZero() {
        assertEquals(Float.POSITIVE_INFINITY, competence.timeSinceActivation());
        competence.tryActivate(joueur, monde);
        competence.update(0.1f);
        assertEquals(0.1f, competence.timeSinceActivation(), 1e-4f);
    }

    @Test
    void uneRechargeNulleEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> new Compteur(0f));
    }
}
