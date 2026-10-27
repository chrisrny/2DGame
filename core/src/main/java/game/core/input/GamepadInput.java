package game.core.input;

import game.core.input.Gamepad.Axis;
import game.core.input.Gamepad.Button;
import game.model.commun.PlayerIntent;
import game.model.commun.Vec2;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Entrées manette, façon « twin-stick ». */
public final class GamepadInput implements InputSource {

    /** En dessous, le stick droit est considéré au repos pour la visée. */
    private static final float AIM_STICK_MIN = 0.25f;

    /** En dessous, on ne vise pas dans le sens de la marche (le stick gauche frôle la zone morte). */
    private static final float MOVE_AIM_MIN = 0.3f;

    private final Gamepad pad;
    private final Supplier<List<Vec2>> targets;
    private Vec2 lastAim = new Vec2(1f, 0f);
    // Vrai au départ : le A qui a lancé la partie est peut-être encore enfoncé.
    private boolean waitRelease = true;

    /**
     * Crée les entrées manette.
     *
     * @param pad la manette
     * @param targets donne les positions des ennemis, pour l'aide à la visée
     */
    public GamepadInput(Gamepad pad, Supplier<List<Vec2>> targets) {
        this.pad = Objects.requireNonNull(pad, "pad");
        this.targets = Objects.requireNonNull(targets, "targets");
    }

    @Override
    public PlayerIntent poll(Vec2 playerPosition) {
        Vec2 move = pad.stick(true);
        if (move.length() == 0f) {
            move = Vec2.fromAxes(dpad(Button.DPAD_RIGHT, Button.DPAD_LEFT), dpad(Button.DPAD_UP, Button.DPAD_DOWN));
        }
        Vec2 stick = pad.stick(false);
        if (stick.length() > AIM_STICK_MIN) {
            lastAim = stick.normalized();
        } else if (move.length() > MOVE_AIM_MIN) {
            lastAim = move.normalized();
        }
        Vec2 aim = AimAssist.adjust(lastAim, playerPosition, targets.get());
        boolean attack = pad.held(Button.X) || pad.trigger(Axis.RIGHT_TRIGGER);
        boolean dodge = pad.held(Button.A) || pad.trigger(Axis.LEFT_TRIGGER);
        boolean skill1 = pad.held(Button.RB) || pad.held(Button.Y);
        boolean skill2 = pad.held(Button.LB) || pad.held(Button.B);
        if (waitRelease) {
            if (attack || dodge || skill1 || skill2) {
                return new PlayerIntent(move, aim, false, false, false, false);
            }
            waitRelease = false;
        }
        return new PlayerIntent(move, aim, attack, dodge, skill1, skill2);
    }

    /**
     * Ignore les boutons d'action jusqu'à ce qu'ils soient tous relâchés : à appeler quand un
     * bouton vient de servir à autre chose (valider un menu, choisir une relique).
     */
    public void ignoreUntilReleased() {
        waitRelease = true;
    }

    private int dpad(Button positive, Button negative) {
        return (pad.held(positive) ? 1 : 0) - (pad.held(negative) ? 1 : 0);
    }
}
