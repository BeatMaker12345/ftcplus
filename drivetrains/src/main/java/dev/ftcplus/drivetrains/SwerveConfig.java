package dev.ftcplus.drivetrains;

import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.sensor.ImuOrientation;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.core.servo.ServoSpec;

import java.util.ArrayList;
import java.util.List;

public final class SwerveConfig {
    final List<SwerveModule> modules = new ArrayList<>();

    DriveMode      mode           = DriveMode.ROBOT_CENTRIC;
    DriveControls  controls       = null;
    HardwareEntry  imuEntry       = null;
    ImuOrientation imuOrientation = ImuOrientation.LOGO_FACING_UP;

    public SwerveConfig module(HardwareEntry drive, MotorSpec driveSpec, HardwareEntry steer, ServoSpec steerSpec) {
        modules.add(new SwerveModule(drive, driveSpec, steer, steerSpec));
        return this;
    }

    public SwerveConfig module(HardwareEntry drive, HardwareEntry steer, ServoSpec steerSpec) {
        modules.add(new SwerveModule(drive, steer, steerSpec));
        return this;
    }

    public SwerveConfig module(HardwareEntry drive, MotorSpec driveSpec, HardwareEntry steer, CRServoSpec steerSpec) {
        modules.add(new SwerveModule(drive, driveSpec, steer, steerSpec));
        return this;
    }

    public SwerveConfig module(HardwareEntry drive, HardwareEntry steer, CRServoSpec steerSpec) {
        modules.add(new SwerveModule(drive, steer, steerSpec));
        return this;
    }

    public SwerveConfig mode(DriveMode mode) {
        this.mode = mode; return this;
    }

    public SwerveConfig controls(DriveControls controls) {
        this.controls = controls; return this;
    }

    public SwerveConfig imu(HardwareEntry entry, ImuOrientation orientation) {
        this.imuEntry = entry; this.imuOrientation = orientation; return this;
    }

    public SwerveConfig imu(HardwareEntry entry) {
        this.imuEntry = entry; return this;
    }

    public void validate() {
        if (modules.isEmpty()) {
            throw new IllegalStateException("At least one swerve module must be configured");
        }
        if (controls == null) {
            throw new IllegalStateException("Drive controls must be configured");
        }
    }


    void resolvePositions(double halfWidth, double halfLength) {
        int n = modules.size();
        for (int i = 0; i < n; i++) {
            SwerveModule m = modules.get(i);
            if (!Double.isNaN(m.x) && !Double.isNaN(m.y)) continue;

            double angle = 2 * Math.PI * i / n;
            m.x = halfWidth  * Math.cos(angle);
            m.y = halfLength  * Math.sin(angle);
        }
    }
}