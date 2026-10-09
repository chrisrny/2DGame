package game.model.world;

import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import java.util.Objects;

/**
 * Le plan d'une map : ses limites, tout ce qui est infranchissable et les points où les
 * ennemis peuvent apparaître. Les maps sont décrites en code dans {@link MapLayouts}.
 *
 * @param arena       limites de la map (un rectangle, même si le contour jouable est irrégulier)
 * @param walls       murs du bord, roche et obstacles
 * @param spawnPoints points où les ennemis peuvent apparaître
 */
public record MapLayout(Rect arena, List<Rect> walls, List<Vec2> spawnPoints) {

    /**
     * Vérifie le plan et fait des copies non modifiables des listes.
     *
     * @throws NullPointerException si un champ est {@code null}
     */
    public MapLayout {
        Objects.requireNonNull(arena, "arena");
        walls = List.copyOf(walls);
        spawnPoints = List.copyOf(spawnPoints);
    }

    /** @return le centre de la map, où apparaît le joueur */
    public Vec2 center() {
        return new Vec2(arena.x() + arena.width() / 2f, arena.y() + arena.height() / 2f);
    }
}
