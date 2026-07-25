package dev.ftcplus.drivetrains;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.Subsystem;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;
import dev.ftcplus.core.sensor.Imu;
import dev.ftcplus.core.statemachine.StateMachine;

public abstract class MecanumDrive extends Subsystem<MecanumDrive.State> {
    public enum State { IDLE, DRIVING }

    private final MecanumConfig config;

    private Motor frontLeft;
    private Motor frontRight;
    private Motor backLeft;
    private Motor backRight;
    private Imu   imu;

    protected MecanumDrive(MecanumConfig config) {
        config.validate();
        this.config = config;
    }

    @Override
    protected State initialState() {
        return State.DRIVING;
    }

    @Override
    protected void defineStates(StateMachine<State> states) {
        states.state(State.DRIVING)
                .onUpdate(this::updateDrive);

        states.state(State.IDLE);
    }

    @Override
    protected void onInitialize() {
        frontLeft  = registerMotor(config.frontLeft,  config.frontLeftSpec);
        frontRight = registerMotor(config.frontRight, config.frontRightSpec);
        backLeft   = registerMotor(config.backLeft, config.backLeftSpec);
        backRight  = registerMotor(config.backRight, config.backRightSpec);

        frontRight.setDirection(Direction.REVERSE);
        backRight.setDirection(Direction.REVERSE);

        for (Motor m : new Motor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
            m.setMode(RunMode.RUN_WITHOUT_ENCODER);
        }

        if (config.mode == DriveMode.FIELD_CENTRIC && config.imuEntry != null) {
            imu = new DriveImu(config.imuEntry, config.imuOrientation);
            register(imu);
        }

        super.onInitialize();
    }

    private Motor registerMotor(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.motor.MotorSpec spec) {
        Motor m = spec != null ? new DriveMotor(entry, spec) : new DriveMotor(entry);
        register(m);
        return m;
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
            strafe  = rotatedStrafe;
            forward = rotatedForward;
        }

        double fl = forward + strafe + turn;
        double fr = forward - strafe - turn;
        double bl = forward - strafe + turn;
        double br = forward + strafe - turn;

        double max = Math.max(1.0, Math.max(Math.abs(fl), Math.max(Math.abs(fr), Math.max(Math.abs(bl), Math.abs(br)))));

        frontLeft.setPower(fl / max);
        frontRight.setPower(fr / max);
        backLeft.setPower(bl / max);
        backRight.setPower(br / max);
    }

    private double axisValue(GamepadAxis axis) {
        if (gamepadFeedback() == null) return 0;
        return gamepadFeedback().axisValue(axis);
    }

    public void resetHeading() {
        if (imu != null) imu.resetYaw();
    }

    public void stop() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }


    private static final class DriveMotor extends Motor {
        DriveMotor(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.motor.MotorSpec spec) { super(entry, spec); }
        DriveMotor(dev.ftcplus.core.HardwareEntry entry) { super(entry); }
    }

    private static final class DriveImu extends Imu {
        DriveImu(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.sensor.ImuOrientation orientation) {
            super(entry, orientation);
        }
    }
}