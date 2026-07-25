package dev.ftcplus.ftcruntime.controls;

import com.qualcomm.robotcore.hardware.Gamepad;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.GamepadFeedback;
import dev.ftcplus.core.GamepadSide;
import dev.ftcplus.core.signal.SignalBus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class Controls implements GamepadFeedback {

    private static final double DEFAULT_TRIGGER_THRESHOLD = 0.5;

    private final Gamepad gamepad1;
    private final Gamepad gamepad2;
    private final SignalBus bus;
    private final List<ControlBinding> bindings = new ArrayList<>();

    // inhibit state
    private boolean inhibitG1 = false;
    private boolean inhibitG2 = false;

    public Controls(Gamepad gamepad1, Gamepad gamepad2, SignalBus bus) {
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
        this.bus = bus;
    }


    public ControlBindingBuilder whenPressed(dev.ftcplus.core.GamepadButton button) {
        return new ControlBindingBuilder(
                heldCondition(button.resolve()),
                bindings, bus
        );
    }

    public ControlBindingBuilder whenHeld(dev.ftcplus.core.GamepadButton button) {
        return new ControlBindingBuilder(
                heldCondition(button.resolve()),
                bindings, bus
        );
    }

    public ControlBindingBuilder whenReleased(dev.ftcplus.core.GamepadButton button) {
        return new ControlBindingBuilder(
                heldCondition(button.resolve()),
                bindings, bus
        );
    }


    public AxisBindingBuilder whenAxis(GamepadAxis axis) {
        return new AxisBindingBuilder(axis, this::axisValue, bindings, bus);
    }


    public AxisEventBuilder withAxis(GamepadAxis axis) {
        return new AxisEventBuilder(axis, this::axisValue, bindings, bus);
    }


    public void vibrate(GamepadSide side, int milliseconds) {
        Gamepad gp = side == GamepadSide.GAMEPAD_1 ? gamepad1 : gamepad2;
        try {
            Method m = gp.getClass().getMethod("rumble", int.class);
            m.invoke(gp, milliseconds);
        } catch (Throwable ignored) {}
    }

    public void setLed(GamepadSide side, double r, double g, double b, int durationMs) {
        Gamepad gp = side == GamepadSide.GAMEPAD_1 ? gamepad1 : gamepad2;
        r = clamp01(r); g = clamp01(g); b = clamp01(b);
        try {
            Method m = gp.getClass().getMethod("setLedColor",
                    double.class, double.class, double.class, int.class);
            m.invoke(gp, r, g, b, durationMs);
        } catch (Throwable ignored) {}
    }


    public void update() {
        updateInhibit();
        for (ControlBinding binding : bindings) {
            binding.update();
        }
    }

    private void updateInhibit() {
        if (gamepad1.start && (gamepad1.a || gamepad1.b)) inhibitG1 = true;
        if (!gamepad1.a && !gamepad1.b) inhibitG1 = false;

        if (gamepad2.start && (gamepad2.a || gamepad2.b)) inhibitG2 = true;
        if (!gamepad2.a && !gamepad2.b) inhibitG2 = false;
    }

    boolean isInhibited(GamepadSide side) {
        return side == GamepadSide.GAMEPAD_1 ? inhibitG1 : inhibitG2;
    }


    private BooleanSupplier heldCondition(dev.ftcplus.core.GamepadButton button) {
        GamepadSide side = button.name().startsWith("G1_")
                ? GamepadSide.GAMEPAD_1
                : GamepadSide.GAMEPAD_2;
        return () -> !isInhibited(side) && isHeld(button);
    }

    private boolean isHeld(dev.ftcplus.core.GamepadButton button) {
        switch (button) {
            case G1_A:          return gamepad1.a;
            case G1_B:          return gamepad1.b;
            case G1_X:          return gamepad1.x;
            case G1_Y:          return gamepad1.y;
            case G1_LB:         return gamepad1.left_bumper;
            case G1_RB:         return gamepad1.right_bumper;
            case G1_LT:         return gamepad1.left_trigger >= DEFAULT_TRIGGER_THRESHOLD;
            case G1_RT:         return gamepad1.right_trigger >= DEFAULT_TRIGGER_THRESHOLD;
            case G1_LS:         return gamepad1.left_stick_button;
            case G1_RS:         return gamepad1.right_stick_button;
            case G1_START:      return gamepad1.start;
            case G1_BACK:       return gamepad1.back;
            case G1_DPAD_UP:    return gamepad1.dpad_up;
            case G1_DPAD_DOWN:  return gamepad1.dpad_down;
            case G1_DPAD_LEFT:  return gamepad1.dpad_left;
            case G1_DPAD_RIGHT: return gamepad1.dpad_right;
            case G2_A:          return gamepad2.a;
            case G2_B:          return gamepad2.b;
            case G2_X:          return gamepad2.x;
            case G2_Y:          return gamepad2.y;
            case G2_LB:         return gamepad2.left_bumper;
            case G2_RB:         return gamepad2.right_bumper;
            case G2_LT:         return gamepad2.left_trigger >= DEFAULT_TRIGGER_THRESHOLD;
            case G2_RT:         return gamepad2.right_trigger >= DEFAULT_TRIGGER_THRESHOLD;
            case G2_LS:         return gamepad2.left_stick_button;
            case G2_RS:         return gamepad2.right_stick_button;
            case G2_START:      return gamepad2.start;
            case G2_BACK:       return gamepad2.back;
            case G2_DPAD_UP:    return gamepad2.dpad_up;
            case G2_DPAD_DOWN:  return gamepad2.dpad_down;
            case G2_DPAD_LEFT:  return gamepad2.dpad_left;
            case G2_DPAD_RIGHT: return gamepad2.dpad_right;
            default:            return false;
        }
    }

    public double axisValue(GamepadAxis axis) {
        switch (axis) {
            case G1_LEFT_STICK_X:   return gamepad1.left_stick_x;
            case G1_LEFT_STICK_Y:   return gamepad1.left_stick_y;
            case G1_RIGHT_STICK_X:  return gamepad1.right_stick_x;
            case G1_RIGHT_STICK_Y:  return gamepad1.right_stick_y;
            case G1_LEFT_TRIGGER:   return gamepad1.left_trigger;
            case G1_RIGHT_TRIGGER:  return gamepad1.right_trigger;
            case G1_TOUCHPAD_1_X:   return getTouchpadField(gamepad1, "touchpad_finger_1", "x");
            case G1_TOUCHPAD_1_Y:   return getTouchpadField(gamepad1, "touchpad_finger_1", "y");
            case G1_TOUCHPAD_2_X:   return getTouchpadField(gamepad1, "touchpad_finger_2", "x");
            case G1_TOUCHPAD_2_Y:   return getTouchpadField(gamepad1, "touchpad_finger_2", "y");
            case G2_LEFT_STICK_X:   return gamepad2.left_stick_x;
            case G2_LEFT_STICK_Y:   return gamepad2.left_stick_y;
            case G2_RIGHT_STICK_X:  return gamepad2.right_stick_x;
            case G2_RIGHT_STICK_Y:  return gamepad2.right_stick_y;
            case G2_LEFT_TRIGGER:   return gamepad2.left_trigger;
            case G2_RIGHT_TRIGGER:  return gamepad2.right_trigger;
            case G2_TOUCHPAD_1_X:   return getTouchpadField(gamepad2, "touchpad_finger_1", "x");
            case G2_TOUCHPAD_1_Y:   return getTouchpadField(gamepad2, "touchpad_finger_1", "y");
            case G2_TOUCHPAD_2_X:   return getTouchpadField(gamepad2, "touchpad_finger_2", "x");
            case G2_TOUCHPAD_2_Y:   return getTouchpadField(gamepad2, "touchpad_finger_2", "y");
            default: return 0;
        }
    }

    private static double getTouchpadField(Gamepad gp, String fingerField, String coord) {
        try {
            Field finger = gp.getClass().getField(fingerField);
            Object fingerObj = finger.get(gp);
            Field f = fingerObj.getClass().getField(coord);
            return f.getFloat(fingerObj);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}