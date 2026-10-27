package game.core.input;

import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Le clavier et la manette en même temps : on peut passer de l'un à l'autre sans rien régler. */
public final class CombinedInput implements InputSource {

    private final InputSource keyboard;
    private final InputSource gamepad;
    private final BooleanSupplier usingGamepad;

    /**
     * Combine deux sources.
     *
     * @param keyboard le clavier et la souris
     * @param gamepad la manette
     * @param usingGamepad {@code true} quand la manette est la dernière source utilisée
     */
    public CombinedInput(InputSource keyboard, InputSource gamepad, BooleanSupplier usingGamepad) {
        this.keyboard = Objects.requireNonNull(keyboard, "keyboard");
        this.gamepad = Objects.requireNonNull(gamepad, "gamepad");
        this.usingGamepad = Objects.requireNonNull(usingGamepad, "usingGamepad");
    }

    @Override
    public PlayerIntent poll(Vec2 playerPosition) {
        PlayerIntent k = keyboard.poll(playerPosition);
        PlayerIntent g = gamepad.poll(playerPosition);
        Vec2 move = k.move().length() > 0f ? k.move() : g.move();
        Vec2 aim = usingGamepad.getAsBoolean() ? g.aim() : k.aim();
        return new PlayerIntent(move, aim, k.attack() || g.attack(), k.dodge() || g.dodge(),
                k.skill1() || g.skill1(), k.skill2() || g.skill2());
    }
}
