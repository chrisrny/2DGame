package game.model.world;

import game.model.commun.Rect;
import game.model.commun.Vec2;
import java.util.List;
import java.util.Objects;

/**
 * Le plan d'une salle : son arène, ses obstacles, ses points d'apparition et ses vagues.
 * Les salles sont décrites en code (P1-B5).
 *
 * @param id          identifiant de la salle, par exemple {@code "room1"}
 * @param arena       limites de l'arène
 * @param obstacles   obstacles infranchissables à l'intérieur de l'arène
 * @param spawnPoints points où les ennemis peuvent apparaître
 * @param waves       vagues de la salle, dans l'ordre
 */
public record RoomLayout(String id, Rect arena, List<Rect> obstacles, List<Vec2> spawnPoints, List<Wave> waves) {

    /**
     * Vérifie le plan et fait des copies non modifiables des listes.
     *
     * @throws NullPointerException si un champ est {@code null}
     */
    public RoomLayout {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(arena, "arena");
        obstacles = List.copyOf(obstacles);
        spawnPoints = List.copyOf(spawnPoints);
        waves = List.copyOf(waves);
    }
}
