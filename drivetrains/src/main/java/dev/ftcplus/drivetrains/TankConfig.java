package dev.ftcplus.drivetrains;

import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.sensor.ImuOrientation;

public final class TankConfig {
    HardwareEntry left;
    MotorSpec     leftSpec;
    HardwareEntry right;
    MotorSpec     rightSpec;

    DriveMode      mode           = DriveMode.ROBOT_CENTRIC;
    DriveControls  controls       = null;
    HardwareEntry  imuEntry       = null;
    ImuOrientation imuOrientation = ImuOrientation.LOGO_FACING_UP;

    public TankConfig left(HardwareEntry entry, MotorSpec spec) {
        this.left = entry; this.leftSpec = spec; return this;
    }

    public TankConfig left(HardwareEntry entry) {
        this.left = entry; return this;
    }

    public TankConfig right(HardwareEntry entry, MotorSpec spec) {
        this.right = entry; this.rightSpec = spec; return this;
    }

    public TankConfig right(HardwareEntry entry) {
        this.right = entry; return this;
    }

    public TankConfig mode(DriveMode mode) {
        this.mode = mode; return this;
    }

    public TankConfig controls(DriveControls controls) {
        this.controls = controls; return this;
    }

    public TankConfig imu(HardwareEntry entry, ImuOrientation orientation) {
        this.imuEntry = entry; this.imuOrientation = orientation; return this;
    }

    public TankConfig imu(HardwareEntry entry) {
        this.imuEntry = entry; return this;
    }

    public void validate() {
        if (left == null || right == null) {
            throw new IllegalStateException("Left and right motors must be configured");
        }
        if (controls == null) {
            throw new IllegalStateException("Drive controls must be configured");
        }
    }
}