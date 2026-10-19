package game.core.input;

import game.model.commun.Vec2;
import java.util.Objects;

/**
 * Une manette, vue par le jeu : boutons, sticks, gâchettes, avec la mise en forme commune à toutes
 * les manettes (zone morte, appuis, directions pour les menus).
 *
 * <p>Pattern Template Method : {@link #update()} fixe les étapes (lire l'état brut, garder l'état
 * précédent, appliquer la zone morte, repérer les appuis) ; une sous-classe ne fournit que la
 * lecture brute du matériel ({@link #readConnected()}, {@link #readButton}, {@link #readAxis}).
 * Le module {@code core} ne dépend donc d'aucune bibliothèque de manette : c'est le lanceur
 * ({@code lwjgl3}) qui branche la vraie manette. Les noms suivent la disposition Xbox ; sur une
 * manette PlayStation, A = ✕, B = ○, X = □, Y = △.</p>
 *
 * <p>Comme le clavier, la manette est lue par interrogation, une fois par image.</p>
 */
public abstract class Gamepad {

    /** Les boutons, dans la disposition Xbox. */
    public enum Button {
        A, B, X, Y, LB, RB, BACK, START, L3, R3, DPAD_UP, DPAD_RIGHT, DPAD_DOWN, DPAD_LEFT
    }

    /** Les axes. Le y des sticks est positif vers le haut ; les gâchettes vont de 0 à 1. */
    public enum Axis {
        LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y, LEFT_TRIGGER, RIGHT_TRIGGER
    }

    /** Une direction de menu : croix ou stick gauche poussé franchement. */
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    /** Zone morte des sticks : en dessous, un stick au repos qui dérive un peu ne compte pas. */
    public static final float DEAD_ZONE = 0.22f;

    /** Une gâchette compte comme enfoncée au-delà de ce seuil. */
    public static final float TRIGGER_THRESHOLD = 0.4f;

    // Hystérésis du stick pour les menus : il faut le pousser franchement pour aller d'une ligne
    // à l'autre, et le relâcher nettement pour recommencer. Sinon un stick tenu à mi-course
    // ferait défiler le menu à chaque petite vibration.
    private static final float MENU_ON = 0.6f;
    private static final float MENU_OFF = 0.35f;

    private static final Button[] BUTTONS = Button.values();
    private static final Axis[] AXES = Axis.values();
    private static final Direction[] DIRECTIONS = Direction.values();

    // Déclarée après les tableaux ci-dessus : son constructeur en a besoin.
    /** Aucune manette : jamais branchée, rien n'est jamais enfoncé. */
    public static final Gamepad NONE = new Gamepad() {
        @Override
        protected boolean readConnected() {
            return false;
        }

        @Override
        protected boolean readButton(Button button) {
            return false;
        }

        @Override
        protected float readAxis(Axis axis) {
            return 0f;
        }
    };

    private final boolean[] held = new boolean[BUTTONS.length];
    private final boolean[] before = new boolean[BUTTONS.length];
    private final float[] axes = new float[AXES.length];
    private final boolean[] direction = new boolean[DIRECTIONS.length];
    private final boolean[] directionBefore = new boolean[DIRECTIONS.length];
    private boolean connected;
    private boolean active;

    /**
     * Lit la manette. À appeler une fois par image, avant que les écrans ne la consultent.
     */
    public final void update() {
        System.arraycopy(held, 0, before, 0, held.length);
        System.arraycopy(direction, 0, directionBefore, 0, direction.length);
        connected = readConnected();
        for (int i = 0; i < BUTTONS.length; i++) {
            held[i] = connected && readButton(BUTTONS[i]);
        }
        for (int i = 0; i < AXES.length; i++) {
            float value = connected ? readAxis(AXES[i]) : 0f;
            axes[i] = Float.isFinite(value) ? Math.clamp(value, -1f, 1f) : 0f;
        }
        updateDirections();
        active = connected && (anyButtonJustPressed() || stick(true).length() > 0f || stick(false).length() > 0f
                || trigger(Axis.LEFT_TRIGGER) || trigger(Axis.RIGHT_TRIGGER));
    }

    private void updateDirections() {
        Vec2 left = new Vec2(axes[Axis.LEFT_X.ordinal()], axes[Axis.LEFT_Y.ordinal()]);
        direction[Direction.UP.ordinal()] = held(Button.DPAD_UP) || pushed(Direction.UP, left.y());
        direction[Direction.DOWN.ordinal()] = held(Button.DPAD_DOWN) || pushed(Direction.DOWN, -left.y());
        direction[Direction.RIGHT.ordinal()] = held(Button.DPAD_RIGHT) || pushed(Direction.RIGHT, left.x());
        direction[Direction.LEFT.ordinal()] = held(Button.DPAD_LEFT) || pushed(Direction.LEFT, -left.x());
    }

    // Stick poussé dans une direction, avec hystérésis (voir MENU_ON / MENU_OFF).
    private boolean pushed(Direction dir, float amount) {
        boolean was = directionBefore[dir.ordinal()];
        return was ? amount > MENU_OFF : amount > MENU_ON;
    }

    private boolean anyButtonJustPressed() {
        for (int i = 0; i < held.length; i++) {
            if (held[i] && !before[i]) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} si une manette est branchée */
    public boolean connected() {
        return connected;
    }

    /**
     * @param button le bouton
     * @return {@code true} tant que le bouton est enfoncé
     */
    public boolean held(Button button) {
        return held[Objects.requireNonNull(button, "button").ordinal()];
    }

    /**
     * @param button le bouton
     * @return {@code true} seulement à l'image où le bouton vient d'être enfoncé
     */
    public boolean justPressed(Button button) {
        int i = Objects.requireNonNull(button, "button").ordinal();
        return held[i] && !before[i];
    }

    /**
     * @param dir la direction
     * @return {@code true} seulement à l'image où la croix ou le stick gauche vient d'être poussé
     *         dans cette direction
     */
    public boolean justPressed(Direction dir) {
        int i = Objects.requireNonNull(dir, "dir").ordinal();
        return direction[i] && !directionBefore[i];
    }

    /**
     * @param axis l'axe
     * @return sa valeur brute, sans zone morte, entre -1 et 1 (0 à 1 pour une gâchette)
     */
    public float axis(Axis axis) {
        return axes[Objects.requireNonNull(axis, "axis").ordinal()];
    }

    /**
     * @param axis une gâchette ({@link Axis#LEFT_TRIGGER} ou {@link Axis#RIGHT_TRIGGER})
     * @return {@code true} tant que la gâchette est enfoncée au-delà de {@link #TRIGGER_THRESHOLD}
     */
    public boolean trigger(Axis axis) {
        return axis(axis) > TRIGGER_THRESHOLD;
    }

    /**
     * Un stick, avec une zone morte ronde : au repos il vaut zéro, et la course restante est
     * étalée de 0 à 1, pour qu'un stick à peine sorti de la zone morte avance doucement.
     *
     * @param left {@code true} pour le stick gauche, {@code false} pour le droit
     * @return la direction du stick, de longueur 0 à 1 (y vers le haut)
     */
    public Vec2 stick(boolean left) {
        Axis ax = left ? Axis.LEFT_X : Axis.RIGHT_X;
        Axis ay = left ? Axis.LEFT_Y : Axis.RIGHT_Y;
        return applyDeadZone(new Vec2(axis(ax), axis(ay)));
    }

    /**
     * Applique la zone morte ronde à la position d'un stick.
     *
     * @param raw la position brute
     * @return zéro dans la zone morte, sinon la même direction, de longueur ramenée entre 0 et 1
     */
    public static Vec2 applyDeadZone(Vec2 raw) {
        float length = raw.length();
        if (length <= DEAD_ZONE) {
            return Vec2.ZERO;
        }
        float scaled = Math.min(1f, (length - DEAD_ZONE) / (1f - DEAD_ZONE));
        return raw.scale(scaled / length);
    }

    /**
     * Indique si la manette vient de servir : un bouton enfoncé, un stick ou une gâchette poussé.
     * Sert à savoir si le joueur joue à la manette ou au clavier.
     *
     * @return {@code true} si la manette a servi à cette image
     */
    public boolean active() {
        return active;
    }

    /** @return {@code true} si une manette est branchée en ce moment (lecture du matériel) */
    protected abstract boolean readConnected();

    /**
     * @param button le bouton
     * @return {@code true} s'il est enfoncé en ce moment (lecture du matériel)
     */
    protected abstract boolean readButton(Button button);

    /**
     * @param axis l'axe
     * @return sa valeur brute en ce moment : -1 à 1 pour les sticks (y vers le haut), 0 à 1 pour
     *         les gâchettes
     */
    protected abstract float readAxis(Axis axis);
}
