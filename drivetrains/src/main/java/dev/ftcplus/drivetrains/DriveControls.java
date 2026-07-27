package dev.ftcplus.drivetrains;

import dev.ftcplus.core.GamepadAxis;

public final class DriveControls {
    public final GamepadAxis strafe;
    public final GamepadAxis forward;
    public final GamepadAxis turn;

    private DriveControls(Builder builder) {
        this.strafe  = builder.strafe;
        this.forward = builder.forward;
        this.turn    = builder.turn;
    }

    private static Builder builder(Builder builder) {
        return new Builder();
    }

    public static final class Builder {
        private GamepadAxis strafe;
        private GamepadAxis forward;
        private GamepadAxis turn;

        public Builder strafe(GamepadAxis axis) {
            this.strafe = axis;
            return this;
        }

        public Builder forward(GamepadAxis axis) {
            this.forward = axis;
            return this;
        }

        public Builder turn(GamepadAxis axis) {
            this.turn = axis;
            return this;
        }

        public DriveControls build() {
            if (forward == null | turn == null) {
                throw new IllegalStateException("strafe, forward, and turn axes are all required");
            }
            return new DriveControls(this);
        }
    }
}