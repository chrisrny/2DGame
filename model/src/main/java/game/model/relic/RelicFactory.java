package game.model.relic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fabrique de reliques (pattern Factory) : c'est le seul endroit qui connaît toutes les reliques du
 * jeu et sait les créer.
 */
public final class RelicFactory {

    private RelicFactory() {
        // Classe utilitaire : pas d'instance.
    }

    /** Crée un exemplaire neuf de chaque relique du jeu, toujours dans le même ordre. */
    public static List<Relic> all() {
        return List.of(new SharpenedBlade(), new AshBoots(), new EmberHeart(),
                new ShadowFeather(), new HunterEye(), new VampireFang(),
                new EmberStep(), new Echo());
    }

    /**
     * Tire des reliques différentes que le joueur ne possède pas encore.
     *
     * @param count nombre de reliques voulues
     * @param random le hasard de la partie
     * @param owned les reliques déjà possédées, reconnues par leur classe
     * @return au plus {@code count} reliques ; moins s'il en reste moins, aucune s'il n'en reste
     *     pas
     * @throws IllegalArgumentException si {@code count} est négatif
     */
    public static List<Relic> draw(int count, Random random, Collection<Relic> owned) {
        if (count < 0) {
            throw new IllegalArgumentException("Le nombre de reliques à tirer ne peut pas être négatif");
        }
        Objects.requireNonNull(random, "random");
        Set<Class<?>> ownedTypes = owned.stream().map(Object::getClass).collect(Collectors.toSet());
        List<Relic> candidates = new ArrayList<>();
        for (Relic relic : all()) {
            if (!ownedTypes.contains(relic.getClass())) {
                candidates.add(relic);
            }
        }
        Collections.shuffle(candidates, random);
        return List.copyOf(candidates.subList(0, Math.min(count, candidates.size())));
    }
}
