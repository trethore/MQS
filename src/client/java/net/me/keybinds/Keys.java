package net.me.keybinds;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public enum Keys {
    UNBOUND(-1, "Unbound"),
    UNKNOWN(GLFW.GLFW_KEY_UNKNOWN, "Unknown"),

    MOUSE_1(0, "Mouse 1"),
    MOUSE_2(1, "Mouse 2"),
    MOUSE_3(2, "Mouse 3"),
    MOUSE_4(3, "Mouse 4"),
    MOUSE_5(4, "Mouse 5"),
    MOUSE_6(5, "Mouse 6"),
    MOUSE_7(6, "Mouse 7"),
    MOUSE_8(7, "Mouse 8"),
    MOUSE_LAST(GLFW.GLFW_MOUSE_BUTTON_LAST, "Mouse Last"),
    MOUSE_LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT, "Mouse Left"),
    MOUSE_RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT, "Mouse Right"),
    MOUSE_MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "Mouse Middle"),

    SPACE(GLFW.GLFW_KEY_SPACE, "Space"),
    APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE, "Apostrophe"),
    COMMA(GLFW.GLFW_KEY_COMMA, "Comma"),
    MINUS(GLFW.GLFW_KEY_MINUS, "Minus"),
    PERIOD(GLFW.GLFW_KEY_PERIOD, "Period"),
    SLASH(GLFW.GLFW_KEY_SLASH, "Slash"),
    KEY_0(GLFW.GLFW_KEY_0, "0"),
    KEY_1(GLFW.GLFW_KEY_1, "1"),
    KEY_2(GLFW.GLFW_KEY_2, "2"),
    KEY_3(GLFW.GLFW_KEY_3, "3"),
    KEY_4(GLFW.GLFW_KEY_4, "4"),
    KEY_5(GLFW.GLFW_KEY_5, "5"),
    KEY_6(GLFW.GLFW_KEY_6, "6"),
    KEY_7(GLFW.GLFW_KEY_7, "7"),
    KEY_8(GLFW.GLFW_KEY_8, "8"),
    KEY_9(GLFW.GLFW_KEY_9, "9"),
    SEMICOLON(GLFW.GLFW_KEY_SEMICOLON, "Semicolon"),
    EQUAL(GLFW.GLFW_KEY_EQUAL, "Equal"),
    A(GLFW.GLFW_KEY_A, "A"),
    B(GLFW.GLFW_KEY_B, "B"),
    C(GLFW.GLFW_KEY_C, "C"),
    D(GLFW.GLFW_KEY_D, "D"),
    E(GLFW.GLFW_KEY_E, "E"),
    F(GLFW.GLFW_KEY_F, "F"),
    G(GLFW.GLFW_KEY_G, "G"),
    H(GLFW.GLFW_KEY_H, "H"),
    I(GLFW.GLFW_KEY_I, "I"),
    J(GLFW.GLFW_KEY_J, "J"),
    K(GLFW.GLFW_KEY_K, "K"),
    L(GLFW.GLFW_KEY_L, "L"),
    M(GLFW.GLFW_KEY_M, "M"),
    N(GLFW.GLFW_KEY_N, "N"),
    O(GLFW.GLFW_KEY_O, "O"),
    P(GLFW.GLFW_KEY_P, "P"),
    Q(GLFW.GLFW_KEY_Q, "Q"),
    R(GLFW.GLFW_KEY_R, "R"),
    S(GLFW.GLFW_KEY_S, "S"),
    T(GLFW.GLFW_KEY_T, "T"),
    U(GLFW.GLFW_KEY_U, "U"),
    V(GLFW.GLFW_KEY_V, "V"),
    W(GLFW.GLFW_KEY_W, "W"),
    X(GLFW.GLFW_KEY_X, "X"),
    Y(GLFW.GLFW_KEY_Y, "Y"),
    Z(GLFW.GLFW_KEY_Z, "Z"),
    LEFT_BRACKET(GLFW.GLFW_KEY_LEFT_BRACKET, "Left Bracket"),
    BACKSLASH(GLFW.GLFW_KEY_BACKSLASH, "Backslash"),
    RIGHT_BRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET, "Right Bracket"),
    GRAVE_ACCENT(GLFW.GLFW_KEY_GRAVE_ACCENT, "Grave Accent"),
    WORLD_1(GLFW.GLFW_KEY_WORLD_1, "World 1"),
    WORLD_2(GLFW.GLFW_KEY_WORLD_2, "World 2"),

    ESCAPE(GLFW.GLFW_KEY_ESCAPE, "Escape"),
    ENTER(GLFW.GLFW_KEY_ENTER, "Enter"),
    TAB(GLFW.GLFW_KEY_TAB, "Tab"),
    BACKSPACE(GLFW.GLFW_KEY_BACKSPACE, "Backspace"),
    INSERT(GLFW.GLFW_KEY_INSERT, "Insert"),
    DELETE(GLFW.GLFW_KEY_DELETE, "Delete"),
    RIGHT(GLFW.GLFW_KEY_RIGHT, "Right Arrow"),
    LEFT(GLFW.GLFW_KEY_LEFT, "Left Arrow"),
    DOWN(GLFW.GLFW_KEY_DOWN, "Down Arrow"),
    UP(GLFW.GLFW_KEY_UP, "Up Arrow"),
    PAGE_UP(GLFW.GLFW_KEY_PAGE_UP, "Page Up"),
    PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down"),
    HOME(GLFW.GLFW_KEY_HOME, "Home"),
    END(GLFW.GLFW_KEY_END, "End"),
    CAPS_LOCK(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock"),
    SCROLL_LOCK(GLFW.GLFW_KEY_SCROLL_LOCK, "Scroll Lock"),
    NUM_LOCK(GLFW.GLFW_KEY_NUM_LOCK, "Num Lock"),
    PRINT_SCREEN(GLFW.GLFW_KEY_PRINT_SCREEN, "Print Screen"),
    PAUSE(GLFW.GLFW_KEY_PAUSE, "Pause"),
    F1(GLFW.GLFW_KEY_F1, "F1"),
    F2(GLFW.GLFW_KEY_F2, "F2"),
    F3(GLFW.GLFW_KEY_F3, "F3"),
    F4(GLFW.GLFW_KEY_F4, "F4"),
    F5(GLFW.GLFW_KEY_F5, "F5"),
    F6(GLFW.GLFW_KEY_F6, "F6"),
    F7(GLFW.GLFW_KEY_F7, "F7"),
    F8(GLFW.GLFW_KEY_F8, "F8"),
    F9(GLFW.GLFW_KEY_F9, "F9"),
    F10(GLFW.GLFW_KEY_F10, "F10"),
    F11(GLFW.GLFW_KEY_F11, "F11"),
    F12(GLFW.GLFW_KEY_F12, "F12"),
    F13(GLFW.GLFW_KEY_F13, "F13"),
    F14(GLFW.GLFW_KEY_F14, "F14"),
    F15(GLFW.GLFW_KEY_F15, "F15"),
    F16(GLFW.GLFW_KEY_F16, "F16"),
    F17(GLFW.GLFW_KEY_F17, "F17"),
    F18(GLFW.GLFW_KEY_F18, "F18"),
    F19(GLFW.GLFW_KEY_F19, "F19"),
    F20(GLFW.GLFW_KEY_F20, "F20"),
    F21(GLFW.GLFW_KEY_F21, "F21"),
    F22(GLFW.GLFW_KEY_F22, "F22"),
    F23(GLFW.GLFW_KEY_F23, "F23"),
    F24(GLFW.GLFW_KEY_F24, "F24"),
    F25(GLFW.GLFW_KEY_F25, "F25"),
    KP_0(GLFW.GLFW_KEY_KP_0, "Numpad 0"),
    KP_1(GLFW.GLFW_KEY_KP_1, "Numpad 1"),
    KP_2(GLFW.GLFW_KEY_KP_2, "Numpad 2"),
    KP_3(GLFW.GLFW_KEY_KP_3, "Numpad 3"),
    KP_4(GLFW.GLFW_KEY_KP_4, "Numpad 4"),
    KP_5(GLFW.GLFW_KEY_KP_5, "Numpad 5"),
    KP_6(GLFW.GLFW_KEY_KP_6, "Numpad 6"),
    KP_7(GLFW.GLFW_KEY_KP_7, "Numpad 7"),
    KP_8(GLFW.GLFW_KEY_KP_8, "Numpad 8"),
    KP_9(GLFW.GLFW_KEY_KP_9, "Numpad 9"),
    KP_DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL, "Numpad Decimal"),
    KP_DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE, "Numpad Divide"),
    KP_MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY, "Numpad Multiply"),
    KP_SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT, "Numpad Subtract"),
    KP_ADD(GLFW.GLFW_KEY_KP_ADD, "Numpad Add"),
    KP_ENTER(GLFW.GLFW_KEY_KP_ENTER, "Numpad Enter"),
    KP_EQUAL(GLFW.GLFW_KEY_KP_EQUAL, "Numpad Equal"),
    LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT, "Left Shift"),
    LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL, "Left Control"),
    LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT, "Left Alt"),
    LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER, "Left Super"),
    RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift"),
    RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL, "Right Control"),
    RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT, "Right Alt"),
    RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER, "Right Super"),
    MENU(GLFW.GLFW_KEY_MENU, "Menu");

    private static final Map<Integer, Keys> LOOKUP = new HashMap<>();

    static {
        for (Keys key : Keys.values()) {
            LOOKUP.put(key.getCode(), key);
        }
    }

    private final int code;
    private final String friendlyName;

    Keys(int code, String friendlyName) {
        this.code = code;
        this.friendlyName = friendlyName;
    }

    public static Optional<Keys> fromCode(int code) {
        return Optional.ofNullable(LOOKUP.get(code));
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return this.friendlyName;
    }
}