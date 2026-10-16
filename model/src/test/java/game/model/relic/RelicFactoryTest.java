package game.model.relic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests de la fabrique de reliques et de son tirage. */
class RelicFactoryTest {

    private static List<String> noms(List<Relic> reliques) {
        return reliques.stream().map(Relic::name).toList();
    }

    private static long typesDifferents(List<Relic> reliques) {
        return reliques.stream().map(Object::getClass).distinct().count();
    }

    @Test
    void laFabriqueConnaitHuitReliquesDifferentes() {
        assertEquals(8, typesDifferents(RelicFactory.all()));
    }

    @Test
    void leTirageDonneTroisReliquesDifferentes() {
        List<Relic> tirage = RelicFactory.draw(3, new Random(1L), List.of());
        assertEquals(3, tirage.size());
        assertEquals(3, typesDifferents(tirage));
    }

    @Test
    void leTirageNeProposeJamaisUneReliqueDejaPossedee() {
        List<Relic> possedees = List.of(new SharpenedBlade(), new AshBoots(), new EmberHeart(),
                new ShadowFeather(), new HunterEye(), new VampireFang());
        for (long graine = 0; graine < 20; graine++) {
            List<Relic> tirage = RelicFactory.draw(3, new Random(graine), possedees);
            assertEquals(2, tirage.size());
            for (Relic relique : tirage) {
                assertTrue(relique instanceof EmberStep || relique instanceof Echo);
            }
        }
    }

    @Test
    void sansReliqueRestanteLeTirageEstVide() {
        assertTrue(RelicFactory.draw(3, new Random(1L), RelicFactory.all()).isEmpty());
    }

    @Test
    void laMemeGraineDonneLeMemeTirage() {
        assertEquals(noms(RelicFactory.draw(3, new Random(42L), List.of())),
                noms(RelicFactory.draw(3, new Random(42L), List.of())));
    }

    @Test
    void unNombreNegatifEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> RelicFactory.draw(-1, new Random(1L), List.of()));
    }
}
