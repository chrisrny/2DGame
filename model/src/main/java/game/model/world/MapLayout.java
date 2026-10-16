package game.model.world;

import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import java.util.Objects;

/**
 * Le plan d'une map : ses limites, tout ce qui est infranchissable, les points où les
 * ennemis peuvent apparaître et la zone du combat contre le boss. Les maps sont décrites en
 * code dans {@link MapLayouts}.
 *
 * @param arena       limites de la map (un rectangle, même si le contour jouable est irrégulier)
 * @param walls       murs du bord, roche et obstacles
 * @param spawnPoints points où les ennemis peuvent apparaître
 * @param bossArena   zone dégagée où le boss apparaît, parmi les points d'apparition qu'elle contient
 */
public record MapLayout(Rect arena, List<Rect> walls, List<Vec2> spawnPoints, Rect bossArena) {

    /**
     * Vérifie le plan et fait des copies non modifiables des listes.
     *
     * @throws NullPointerException si un champ est {@code null}
     */
    public MapLayout {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(bossArena, "bossArena");
        walls = List.copyOf(walls);
        spawnPoints = List.copyOf(spawnPoints);
    }

    /**
     * Crée un plan sans arène dédiée : toute la map sert d'arène au boss. Pratique pour les
     * salles de test.
     *
     * @param arena       limites de la map
     * @param walls       murs du bord, roche et obstacles
     * @param spawnPoints points où les ennemis peuvent apparaître
     */
    public MapLayout(Rect arena, List<Rect> walls, List<Vec2> spawnPoints) {
        this(arena, walls, spawnPoints, arena);
    }

    /** @return le centre de la map, où apparaît le joueur */
    public Vec2 center() {
        return new Vec2(arena.x() + arena.width() / 2f, arena.y() + arena.height() / 2f);
    }
}
