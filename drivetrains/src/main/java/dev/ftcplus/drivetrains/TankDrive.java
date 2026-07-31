package dev.ftcplus.drivetrains;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.Drive;
import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;
import dev.ftcplus.core.sensor.Imu;
import dev.ftcplus.core.statemachine.StateMachine;

public abstract class TankDrive extends Drive<TankDrive.State> {
    public enum State {IDLE, DRIVING}

    private final TankConfig config;

    private Motor left;
    private Motor right;
    private Imu imu;
    private double inputForward;
    private double inputTurn;

    protected TankDrive(TankConfig config) {
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
        left = registerMotor(config.left, config.leftSpec);
        right = registerMotor(config.right, config.rightSpec);

        right.setDirection(Direction.REVERSE);

        for (Motor m : new Motor[]{left, right}) {
            m.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
            m.setMode(RunMode.RUN_WITHOUT_ENCODER);
        }

        if (config.imuEntry != null) {
            imu = new TankImu(config.imuEntry, config.imuOrientation);
            register(imu);
        }

        super.onInitialize();
    }

    public void setInputs(double forward, double turn, double strafe) {
        this.inputForward = forward;
        this.inputTurn = turn;
    }

    private Motor registerMotor(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.motor.MotorSpec spec) {
        Motor m = spec != null ? new TankMotor(entry, spec) : new TankMotor(entry);
        register(m);
        return m;
    }

    private void updateDrive() {
        if (config.controls == null) return;

        double forward = inputForward;
        double turn    = inputTurn;

        if (config.mode == DriveMode.FIELD_CENTRIC && imu != null) {
            double heading = Math.toRadians(imu.getYaw());

            double cos = Math.cos(heading);
            double sin = Math.sin(heading);
            double rotatedForward =  forward * cos + turn * sin;
            double rotatedTurn    = -forward * sin + turn * cos;
            forward = rotatedForward;
            turn    = rotatedTurn;
        }

        double leftPower  = forward + turn;
        double rightPower = forward - turn;

        double max = Math.max(1.0, Math.max(Math.abs(leftPower), Math.abs(rightPower)));

        left.setPower(leftPower / max);
        right.setPower(rightPower / max);
    }

    private double axisValue(GamepadAxis axis) {
        if (gamepadFeedback() == null) return 0;
        return gamepadFeedback().axisValue(axis);
    }

    public void stop() {
        left.setPower(0);
        right.setPower(0);
    }

    public void resetHeading() {
        if (imu != null) imu.resetYaw();
    }


    private static final class TankMotor extends Motor {
        TankMotor(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.motor.MotorSpec spec) { super(entry, spec); }
        TankMotor(dev.ftcplus.core.HardwareEntry entry)  { super(entry); }
    }

    private static final class TankImu extends Imu {
        TankImu(dev.ftcplus.core.HardwareEntry entry, dev.ftcplus.core.sensor.ImuOrientation orientation) {
            super(entry, orientation);
        }
    }
}