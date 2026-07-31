package dev.ftcplus.runtime.controls;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.Drive;

public final class DriveBindingBuilder<T extends Drive> {

    private final T drive;
    private final GamepadState gamepadState;
    private GamepadAxis forwardAxis;
    private GamepadAxis strafeAxis;
    private GamepadAxis turnAxis;

    DriveBindingBuilder(T drive, GamepadState gamepadState) {
        this.drive        = drive;
        this.gamepadState = gamepadState;
    }

    public DriveBindingBuilder<T> forward(GamepadAxis axis) { this.forwardAxis = axis; return this; }
    public DriveBindingBuilder<T> strafe(GamepadAxis axis)  { this.strafeAxis  = axis; return this; }
    public DriveBindingBuilder<T> turn(GamepadAxis axis)    { this.turnAxis    = axis; return this; }

    DriveBinding binding() {
        return () -> {
            double forward = forwardAxis != null ? -gamepadState.axisValue(forwardAxis) : 0;
            double strafe  = strafeAxis  != null ?  gamepadState.axisValue(strafeAxis)  : 0;
            double turn    = turnAxis    != null ?  gamepadState.axisValue(turnAxis)    : 0;
            drive.setInputs(forward, strafe, turn);
        };
    }
}
