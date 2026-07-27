package dev.ftcplus.drivetrains;

import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.sensor.ImuOrientation;

public class HDriveConfig {
    HardwareEntry left;
    MotorSpec leftSpec;
    HardwareEntry right;
    MotorSpec rightSpec;
    HardwareEntry center;
    MotorSpec centerSpec;

    DriveMode      mode           = DriveMode.ROBOT_CENTRIC;
    DriveControls  controls       = null;
    HardwareEntry  imuEntry       = null;
    ImuOrientation imuOrientation = ImuOrientation.LOGO_FACING_UP;

    public HDriveConfig left(HardwareEntry entry, MotorSpec spec) {
        this.left = entry; this.leftSpec = spec; return this;
    }

    public HDriveConfig left(HardwareEntry entry) {
        this.left = entry; return this;
    }

    public HDriveConfig right(HardwareEntry entry, MotorSpec spec) {
        this.right = entry; this.rightSpec = spec; return this;
    }

    public HDriveConfig right(HardwareEntry entry) {
        this.right = entry; return this;
    }

    public HDriveConfig center(HardwareEntry entry, MotorSpec spec) {
        this.center = entry; this.centerSpec = spec; return this;
    }

    public HDriveConfig center(HardwareEntry entry) {
        this.center = entry; return this;
    }

    public HDriveConfig mode(DriveMode mode) {
        this.mode = mode; return this;
    }

    public HDriveConfig controls(DriveControls controls) {
        this.controls = controls; return this;
    }

    public HDriveConfig imu(HardwareEntry entry, ImuOrientation orientation) {
        this.imuEntry = entry; this.imuOrientation = orientation; return this;
    }

    public HDriveConfig imu(HardwareEntry entry) {
        this.imuEntry = entry; return this;
    }

    public void validate() {
        if (left == null || right == null || center == null) {
            throw new IllegalStateException("Left, right, and center motors must be configured");
        }
        if (controls == null) {
            throw new IllegalStateException("Drive controls must be configured");
        }
    }

}