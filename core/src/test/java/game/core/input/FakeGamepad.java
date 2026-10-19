package game.core.input;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Manette de test : on règle à la main ce qu'elle renvoie, puis on appelle {@code update()}. */
final class FakeGamepad extends Gamepad {

    boolean plugged = true;
    final Set<Button> down = EnumSet.noneOf(Button.class);
    final Map<Axis, Float> values = new EnumMap<>(Axis.class);

    FakeGamepad press(Button button) {
        down.add(button);
        return this;
    }

    FakeGamepad release(Button button) {
        down.remove(button);
        return this;
    }

    FakeGamepad set(Axis axis, float value) {
        values.put(axis, value);
        return this;
    }

    @Override
    protected boolean readConnected() {
        return plugged;
    }

    @Override
    protected boolean readButton(Button button) {
        return down.contains(button);
    }

    @Override
    protected float readAxis(Axis axis) {
        return values.getOrDefault(axis, 0f);
    }
}
