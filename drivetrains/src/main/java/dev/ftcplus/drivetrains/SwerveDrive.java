package dev.ftcplus.drivetrains;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.Subsystem;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;
import dev.ftcplus.core.sensor.Imu;
import dev.ftcplus.core.servo.CRServo;
import dev.ftcplus.core.servo.Servo;
import dev.ftcplus.core.statemachine.StateMachine;

import java.util.ArrayList;
import java.util.List;

public abstract class SwerveDrive extends Subsystem<SwerveDrive.State> {
    public enum State { IDLE, DRIVING }

    private static final double DEFAULT_HALF_WIDTH  = 7.0;
    private static final double DEFAULT_HALF_LENGTH = 7.0;

    private final SwerveConfig config;

    private final List<Motor>   driveMotors   = new ArrayList<>();
    private final List<Servo>   steerServos   = new ArrayList<>();
    private final List<CRServo> steerCRServos = new ArrayList<>();
    private final List<SwerveModule> modules;

    private Imu imu;
    private double[] lastAngles;

    protected SwerveDrive(SwerveConfig config) {
        config.validate();
        config.resolvePositions(DEFAULT_HALF_WIDTH, DEFAULT_HALF_LENGTH);
        this.config = config;
        this.modules = config.modules;
    }

    @Override
    protected State initialState() {
        return State.DRIVING;
    }

    @Override
    protected void defineStates(StateMachine<State> states) {
        states.state(State.DRIVING).onUpdate(this::updateDrive);
        states.state(State.IDLE);
    }

    @Override
    protected void onInitialize() {
        lastAngles = new double[modules.size()];

        for (SwerveModule module : modules) {
            Motor drive = module.driveSpec != null ? new SwerveMotor(module.driveEntry, module.driveSpec) : new SwerveMotor(module.driveEntry);
            drive.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
            drive.setMode(RunMode.RUN_WITHOUT_ENCODER);
            register(drive);
            driveMotors.add(drive);

            if (module.steerType == SwerveModule.SteerType.SERVO) {
                Servo steer = module.servoSpec != null ? new SwerveServo(module.steerEntry, module.servoSpec) : new SwerveServo(module.steerEntry);
                register(steer);
                steerServos.add(steer);
                steerCRServos.add(null);
            } else {
                CRServo steer = module.crServoSpec != null ? new SwerveCRServo(module.steerEntry, module.crServoSpec) : new SwerveCRServo(module.steerEntry);
                register(steer);
                steerCRServos.add(steer);
                steerServos.add(null);
            }
        }

        if (config.imuEntry != null) {
            imu = new SwerveImu(config.imuEntry, config.imuOrientation);
            register(imu);
        }

        super.onInitialize();
    }

    private void updateDrive() {
        if (config.controls == null) return;

        double strafe  =  axisValue(config.controls.strafe);
        double forward = -axisValue(config.controls.forward);
        double turn    =  axisValue(config.controls.turn);

        if (config.mode == DriveMode.FIELD_CENTRIC && imu != null) {
            double heading = Math.toRadians(imu.getYaw());
            double cos = Math.cos(heading);
            double sin = Math.sin(heading);
            double rotatedStrafe  =  strafe * cos + forward * sin;
            double rotatedForward = -strafe * sin + forward * cos;
            strafe = rotatedStrafe;
            forward = rotatedForward;
        }

        double maxSpeed = 1.0;

        double[] speeds = new double[modules.size()];
        double[] angles = new double[modules.size()];

        for (int i = 0; i < modules.size(); i++) {
            SwerveModule m = modules.get(i);

            double vx = strafe  + turn * m.y;
            double vy = forward - turn * m.x;

            speeds[i] = Math.hypot(vx, vy);
            angles[i] = Math.toDegrees(Math.atan2(vx, vy));

            if (speeds[i] > maxSpeed) maxSpeed = speeds[i];
        }

        for (int i = 0; i < modules.size(); i++) {
            speeds[i] /= maxSpeed;
        }

        for (int i = 0; i < modules.size(); i++) {
            double targetAngle = optimizeAngle(angles[i], lastAngles[i]);
            lastAngles[i] = targetAngle;

            driveMotors.get(i).setPower(speeds[i]);
            setSteerAngle(i, targetAngle);
        }
    }


    private double optimizeAngle(double target, double current) {
        double delta = target - current;

        while (delta > 180)  delta -= 360;
        while (delta < -180) delta += 360;

        if (Math.abs(delta) > 90) {
            delta -= Math.signum(delta) * 180;
        }

        return current + delta;
    }

    private void setSteerAngle(int i, double degrees) {
        Servo servo = steerServos.get(i);
        CRServo crServo = steerCRServos.get(i);

        if (servo != null) {
            if (servo.hasSpec()) {
                servo.moveToDegrees(
                        Math.max(0, Math.min(servo.spec().travelDegrees, degrees % servo.spec().travelDegrees))
                );
            } else {
                double position = (degrees % 180) / 180.0;
                servo.setPosition(Math.max(0, Math.min(1, position)));
            }
        } else if (crServo != null) {
            // if execution reaches this line, good luck
            double error = degrees - lastAngles[i];
            while (error > 180) error -= 360;
            while (error < -180) error += 360;
            crServo.setPower(Math.max(-1, Math.min(1, error / 90.0)));
        }
    }

    private double axisValue(GamepadAxis axis) {
        if (axis == null || gamepadFeedback() == null) return 0;
        return gamepadFeedback().axisValue(axis);
    }

    public void stop() {
        for (Motor m : driveMotors) m.setPower(0);
        for (CRServo c : steerCRServos) { if (c != null) c.setPower(0); }
    }

    public void resetHeading() {
        if (imu != null) imu.resetYaw();
    }


    private static final class SwerveMotor extends Motor {
        SwerveMotor(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.motor.MotorSpec s) { super(e, s); }
        SwerveMotor(dev.ftcplus.core.HardwareEntry e) { super(e); }
    }

    private static final class SwerveServo extends Servo {
        SwerveServo(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.servo.ServoSpec s) { super(e, s); }
        SwerveServo(dev.ftcplus.core.HardwareEntry e) { super(e); }
    }

    private static final class SwerveCRServo extends CRServo {
        SwerveCRServo(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.servo.CRServoSpec s) { super(e, s); }
        SwerveCRServo(dev.ftcplus.core.HardwareEntry e) { super(e); }
    }

    private static final class SwerveImu extends Imu {
        SwerveImu(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.sensor.ImuOrientation o) { super(e, o); }
    }
}