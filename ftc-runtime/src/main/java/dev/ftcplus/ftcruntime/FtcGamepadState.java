package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.hardware.Gamepad;
import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.GamepadButton;
import dev.ftcplus.core.GamepadSide;
import dev.ftcplus.runtime.controls.GamepadState;

public class FtcGamepadState implements GamepadState {
    private final Gamepad g1;
    private final Gamepad g2;

    public FtcGamepadState(Gamepad g1, Gamepad g2) {
        this.g1 = g1;
        this.g2 = g2;
    }

    @Override
    public double axisValue(GamepadAxis axis) {
        return switch (axis) {
            case G1_LEFT_STICK_X  -> g1.left_stick_x;
            case G1_LEFT_STICK_Y  -> g1.left_stick_y;
            case G1_RIGHT_STICK_X -> g1.right_stick_x;
            case G1_RIGHT_STICK_Y -> g1.right_stick_y;
            case G1_LEFT_TRIGGER  -> g1.left_trigger;
            case G1_RIGHT_TRIGGER -> g1.right_trigger;
            case G1_TOUCHPAD_1_X  -> g1.touchpad_finger_1_x;
            case G1_TOUCHPAD_1_Y  -> g1.touchpad_finger_1_y;
            case G1_TOUCHPAD_2_X  -> g1.touchpad_finger_2_x;
            case G1_TOUCHPAD_2_Y  -> g1.touchpad_finger_2_y;
            case G2_LEFT_STICK_X  -> g2.left_stick_x;
            case G2_LEFT_STICK_Y  -> g2.left_stick_y;
            case G2_RIGHT_STICK_X -> g2.right_stick_x;
            case G2_RIGHT_STICK_Y -> g2.right_stick_y;
            case G2_LEFT_TRIGGER  -> g2.left_trigger;
            case G2_RIGHT_TRIGGER -> g2.right_trigger;
            case G2_TOUCHPAD_1_X  -> g2.touchpad_finger_1_x;
            case G2_TOUCHPAD_1_Y  -> g2.touchpad_finger_1_y;
            case G2_TOUCHPAD_2_X  -> g2.touchpad_finger_2_x;
            case G2_TOUCHPAD_2_Y  -> g2.touchpad_finger_2_y;
        };
    }

    @Override
    public boolean buttonValue(GamepadButton button) {
        return switch (button.resolve()) {
            case G1_A          -> g1.a                   || g1.cross;
            case G1_B          -> g1.b                   || g1.circle;
            case G1_X          -> g1.x                   || g1.square;
            case G1_Y          -> g1.y                   || g1.triangle;
            case G1_LB         -> g1.left_bumper;
            case G1_RB         -> g1.right_bumper;
            case G1_LT         -> g1.left_trigger        > 0.5;
            case G1_RT         -> g1.right_trigger       > 0.5;
            case G1_LS         -> g1.left_stick_button;
            case G1_RS         -> g1.right_stick_button;
            case G1_START      -> g1.start               || g1.options;
            case G1_BACK       -> g1.back                || g1.share;
            case G1_DPAD_UP    -> g1.dpad_up;
            case G1_DPAD_DOWN  -> g1.dpad_down;
            case G1_DPAD_LEFT  -> g1.dpad_left;
            case G1_DPAD_RIGHT -> g1.dpad_right;
            case G2_A          -> g2.a                   || g2.cross;
            case G2_B          -> g2.b                   || g2.circle;
            case G2_X          -> g2.x                   || g2.square;
            case G2_Y          -> g2.y                   || g2.triangle;
            case G2_LB         -> g2.left_bumper;
            case G2_RB         -> g2.right_bumper;
            case G2_LT         -> g2.left_trigger        > 0.5;
            case G2_RT         -> g2.right_trigger       > 0.5;
            case G2_LS         -> g2.left_stick_button;
            case G2_RS         -> g2.right_stick_button;
            case G2_START      -> g2.start               || g2.options;
            case G2_BACK       -> g2.back                || g2.share;
            case G2_DPAD_UP    -> g2.dpad_up;
            case G2_DPAD_DOWN  -> g2.dpad_down;
            case G2_DPAD_LEFT  -> g2.dpad_left;
            case G2_DPAD_RIGHT -> g2.dpad_right;
            default -> false;
        };
    }

    @Override
    public void vibrate(GamepadSide side, int durationMs) {
        if (side == GamepadSide.GAMEPAD_1 || side == GamepadSide.BOTH) g1.rumble(durationMs);
        if (side == GamepadSide.GAMEPAD_2 || side == GamepadSide.BOTH) g2.rumble(durationMs);
    }

    @Override
    public void setLed(GamepadSide side, double r, double g, double b, int durationMs) {
        if (side == GamepadSide.GAMEPAD_1 || side == GamepadSide.BOTH) g1.setLedColor(r, g, b, durationMs);
        if (side == GamepadSide.GAMEPAD_2 || side == GamepadSide.BOTH) g2.setLedColor(r, g, b, durationMs);
    }
}