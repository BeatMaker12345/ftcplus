package dev.ftcplus.drivetrains;

import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.sensor.ImuOrientation;

public final class MecanumConfig {
    HardwareEntry frontLeft;
    MotorSpec     frontLeftSpec;
    HardwareEntry frontRight;
    MotorSpec     frontRightSpec;
    HardwareEntry backLeft;
    MotorSpec     backLeftSpec;
    HardwareEntry backRight;
    MotorSpec     backRightSpec;

    DriveMode      mode           = DriveMode.ROBOT_CENTRIC;
    DriveControls  controls       = null;
    HardwareEntry  imuEntry       = null;
    ImuOrientation imuOrientation = ImuOrientation.LOGO_FACING_UP;

    public MecanumConfig frontLeft(HardwareEntry entry, MotorSpec spec) {
        this.frontLeft = entry; this.frontLeftSpec = spec; return this;
    }

    public MecanumConfig frontLeft(HardwareEntry entry) {
        this.frontLeft = entry; return this;
    }

    public MecanumConfig frontRight(HardwareEntry entry, MotorSpec spec) {
        this.frontRight = entry; this.frontRightSpec = spec; return this;
    }

    public MecanumConfig frontRight(HardwareEntry entry) {
        this.frontRight = entry; return this;
    }

    public MecanumConfig backLeft(HardwareEntry entry, MotorSpec spec) {
        this.backLeft = entry; this.backLeftSpec = spec; return this;
    }

    public MecanumConfig backLeft(HardwareEntry entry) {
        this.backLeft = entry; return this;
    }

    public MecanumConfig backRight(HardwareEntry entry, MotorSpec spec) {
        this.backRight = entry; this.backRightSpec = spec; return this;
    }

    public MecanumConfig backRight(HardwareEntry entry) {
        this.backRight = entry; return this;
    }

    public MecanumConfig mode(DriveMode mode) {
        this.mode = mode; return this;
    }

    public MecanumConfig controls(DriveControls controls) {
        this.controls = controls; return this;
    }

    public MecanumConfig imu(HardwareEntry entry) {
        this.imuEntry = entry; return this;
    }

    public MecanumConfig imu(HardwareEntry entry, ImuOrientation orientation) {
        this.imuEntry = entry; this.imuOrientation = orientation; return this;
    }

    public void validate() {
        if (frontLeft == null || frontRight == null || backLeft == null || backRight == null) {
            throw new IllegalStateException("All four drive motors must be configured");
        }
        if (controls == null) {
            throw new IllegalStateException("Drive controls must be configured");
        }
    }
}