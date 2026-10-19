package game.lwjgl3;

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_A;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_B;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_BACK;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_START;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_Y;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadState;
import static org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad;

import game.core.input.Gamepad;
import org.lwjgl.glfw.GLFWGamepadState;

/**
 * La vraie manette, lue avec GLFW, la bibliothèque de fenêtre qu'utilise déjà libGDX sur desktop.
 *
 * <p>GLFW reconnaît les manettes courantes (Xbox, PlayStation, Switch Pro…) et les présente
 * toutes avec la même disposition, celle d'une manette Xbox. On prend la première manette
 * branchée ; si elle est débranchée en cours de partie, on passe à la suivante, ou à aucune.
 * Aucune bibliothèque en plus : GLFW est déjà embarqué par le lanceur.</p>
 *
 * <p>Seule la lecture brute est ici ; la zone morte et les appuis sont gérés par {@link Gamepad}.
 * GLFW ne se lit que depuis le thread principal : c'est le cas, la manette est lue à chaque image
 * par le jeu.</p>
 */
final class GlfwGamepad extends Gamepad {

    private final GLFWGamepadState state = GLFWGamepadState.create();
    private int joystick = -1;

    @Override
    protected boolean readConnected() {
        if (joystick < 0 || !glfwJoystickIsGamepad(joystick)) {
            joystick = findGamepad();
        }
        return joystick >= 0 && glfwGetGamepadState(joystick, state);
    }

    private static int findGamepad() {
        for (int id = GLFW_JOYSTICK_1; id <= GLFW_JOYSTICK_LAST; id++) {
            if (glfwJoystickIsGamepad(id)) {
                return id;
            }
        }
        return -1;
    }

    @Override
    protected boolean readButton(Button button) {
        return state.buttons(glfwButton(button)) == GLFW_PRESS;
    }

    @Override
    protected float readAxis(Axis axis) {
        return switch (axis) {
            // GLFW compte le y vers le bas : on le retourne pour avoir y vers le haut, comme le jeu.
            case LEFT_X -> state.axes(GLFW_GAMEPAD_AXIS_LEFT_X);
            case LEFT_Y -> -state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y);
            case RIGHT_X -> state.axes(GLFW_GAMEPAD_AXIS_RIGHT_X);
            case RIGHT_Y -> -state.axes(GLFW_GAMEPAD_AXIS_RIGHT_Y);
            // Gâchettes : de -1 (relâchée) à 1 (enfoncée) chez GLFW, ramenées de 0 à 1.
            case LEFT_TRIGGER -> (state.axes(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) + 1f) / 2f;
            case RIGHT_TRIGGER -> (state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) + 1f) / 2f;
        };
    }

    private static int glfwButton(Button button) {
        return switch (button) {
            case A -> GLFW_GAMEPAD_BUTTON_A;
            case B -> GLFW_GAMEPAD_BUTTON_B;
            case X -> GLFW_GAMEPAD_BUTTON_X;
            case Y -> GLFW_GAMEPAD_BUTTON_Y;
            case LB -> GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
            case RB -> GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
            case BACK -> GLFW_GAMEPAD_BUTTON_BACK;
            case START -> GLFW_GAMEPAD_BUTTON_START;
            case L3 -> GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
            case R3 -> GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
            case DPAD_UP -> GLFW_GAMEPAD_BUTTON_DPAD_UP;
            case DPAD_RIGHT -> GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
            case DPAD_DOWN -> GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
            case DPAD_LEFT -> GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
        };
    }
}
