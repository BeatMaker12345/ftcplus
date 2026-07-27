package dev.ftcplus.drivetrains;

import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.core.servo.ServoSpec;

public final class SwerveModule {
    public enum SteerType { SERVO, CR_SERVO }

    final HardwareEntry driveEntry;
    final MotorSpec     driveSpec;

    final HardwareEntry steerEntry;
    final SteerType     steerType;
    final ServoSpec     servoSpec;
    final CRServoSpec crServoSpec;

    double x = Double.NaN;
    double y = Double.NaN;

    SwerveModule(HardwareEntry driveEntry, MotorSpec driveSpec, HardwareEntry steerEntry, ServoSpec servoSpec) {
        this.driveEntry  = driveEntry;
        this.driveSpec   = driveSpec;
        this.steerEntry  = steerEntry;
        this.steerType   = SteerType.SERVO;
        this.servoSpec   = servoSpec;
        this.crServoSpec = null;
    }

    SwerveModule(HardwareEntry driveEntry, HardwareEntry steerEntry, ServoSpec servoSpec) {
        this(driveEntry, null, steerEntry, servoSpec);
    }

    SwerveModule(HardwareEntry driveEntry, MotorSpec driveSpec, HardwareEntry steerEntry, CRServoSpec crServoSpec) {
        this.driveEntry  = driveEntry;
        this.driveSpec   = driveSpec;
        this.steerEntry  = steerEntry;
        this.steerType   = SteerType.CR_SERVO;
        this.servoSpec   = null;
        this.crServoSpec = crServoSpec;
    }

    SwerveModule(HardwareEntry driveEntry, HardwareEntry steerEntry, CRServoSpec crServoSpec) {
        this(driveEntry, null, steerEntry, crServoSpec);
    }

    public SwerveModule position(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }
}