package dev.ftcplus.ftcruntime.controls;

public enum GamepadButton {
    // Gamepad 1
    G1_A, G1_B, G1_X, G1_Y,
    G1_LB, G1_RB,
    G1_LT, G1_RT,
    G1_LS, G1_RS,
    G1_START, G1_BACK,
    G1_DPAD_UP, G1_DPAD_DOWN, G1_DPAD_LEFT, G1_DPAD_RIGHT,

    // Gamepad 1 PS aliases
    G1_CROSS     (G1_A),
    G1_CIRCLE    (G1_B),
    G1_SQUARE    (G1_X),
    G1_TRIANGLE  (G1_Y),
    G1_L1        (G1_LB),
    G1_R1        (G1_RB),
    G1_L2        (G1_LT),
    G1_R2        (G1_RT),
    G1_L3        (G1_LS),
    G1_R3        (G1_RS),
    G1_OPTIONS   (G1_START),
    G1_SHARE     (G1_BACK),

    // Gamepad 2
    G2_A, G2_B, G2_X, G2_Y,
    G2_LB, G2_RB,
    G2_LT, G2_RT,
    G2_LS, G2_RS,
    G2_START, G2_BACK,
    G2_DPAD_UP, G2_DPAD_DOWN, G2_DPAD_LEFT, G2_DPAD_RIGHT,

    // Gamepad 2 PS aliases
    G2_CROSS     (G2_A),
    G2_CIRCLE    (G2_B),
    G2_SQUARE    (G2_X),
    G2_TRIANGLE  (G2_Y),
    G2_L1        (G2_LB),
    G2_R1        (G2_RB),
    G2_L2        (G2_LT),
    G2_R2        (G2_RT),
    G2_L3        (G2_LS),
    G2_R3        (G2_RS),
    G2_OPTIONS   (G2_START),
    G2_SHARE     (G2_BACK);

    /** If non-null, this button is an alias that resolves to the canonical one. */
    public final GamepadButton canonical;

    GamepadButton() {
        this.canonical = null;
    }

    GamepadButton(GamepadButton canonical) {
        this.canonical = canonical;
    }

    public GamepadButton resolve() {
        return canonical != null ? canonical : this;
    }
}