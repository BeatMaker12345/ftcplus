package dev.ftcplus.drivetrains;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.Subsystem;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;
import dev.ftcplus.core.sensor.Imu;
import dev.ftcplus.core.statemachine.StateMachine;

public abstract class HDrive extends Subsystem<HDrive.State> {
    public enum State { IDLE, DRIVING }

    private final HDriveConfig config;

    private Motor left;
    private Motor right;
    private Motor center;
    private Imu   imu;

    protected HDrive(HDriveConfig config) {
        config.validate();
        this.config = config;
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
        left   = registerMotor(config.left,   config.leftSpec);
        right  = registerMotor(config.right,  config.rightSpec);
        center = registerMotor(config.center, config.centerSpec);

        right.setDirection(Direction.REVERSE);

        for (Motor m : new Motor[]{left, right, center}) {
            m.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
            m.setMode(RunMode.RUN_WITHOUT_ENCODER);
        }

        if (config.imuEntry != null) {
            imu = new HDriveImu(config.imuEntry, config.imuOrientation);
            register(imu);
        }

        super.onInitialize();
    }

    private Motor registerMotor(dev.ftcplus.core.HardwareEntry entry,
                                dev.ftcplus.core.motor.MotorSpec spec) {
        Motor m = spec != null
                ? new HDriveMotor(entry, spec)
                : new HDriveMotor(entry);
        register(m);
        return m;
    }

    private void updateDrive() {
        if (config.controls == null) return;

        double forward = -axisValue(config.controls.forward);
        double turn    =  axisValue(config.controls.turn);
        double strafe  =  axisValue(config.controls.strafe);

        if (config.mode == DriveMode.FIELD_CENTRIC && imu != null) {
            double heading = Math.toRadians(imu.getYaw());
            double cos = Math.cos(heading);
            double sin = Math.sin(heading);
            double rotatedStrafe  = strafe * cos + forward * sin;
            double rotatedForward = -strafe * sin + forward * cos;
            strafe  = rotatedStrafe;
            forward = rotatedForward;
        }

        double leftPower  = forward + turn;
        double rightPower = forward - turn;

        double max = Math.max(1.0, Math.max(Math.abs(leftPower),
                Math.max(Math.abs(rightPower), Math.abs(strafe))));

        left.setPower(leftPower / max);
        right.setPower(rightPower / max);
        center.setPower(strafe / max);
    }

    private double axisValue(GamepadAxis axis) {
        if (axis == null || gamepadFeedback() == null) return 0;
        return gamepadFeedback().axisValue(axis);
    }

    public void stop() {
        left.setPower(0);
        right.setPower(0);
        center.setPower(0);
    }

    public void resetHeading() {
        if (imu != null) imu.resetYaw();
    }


    private static final class HDriveMotor extends Motor {
        HDriveMotor(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.motor.MotorSpec s) { super(e, s); }
        HDriveMotor(dev.ftcplus.core.HardwareEntry e) { super(e); }
    }

    private static final class HDriveImu extends Imu {
        HDriveImu(dev.ftcplus.core.HardwareEntry e, dev.ftcplus.core.sensor.ImuOrientation o) { super(e, o); }
    }

}