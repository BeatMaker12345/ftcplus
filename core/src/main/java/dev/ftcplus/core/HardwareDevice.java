package dev.ftcplus.core;

import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.diagnostic.StallPosition;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public class HardwareDevice extends Component {
    private static final long   STALL_CHECK_INTERVAL_MS = 50;
    private static final int    STALL_POSITION_THRESHOLD = 3;
    private static final long   STALL_CONFIRM_DURATION_MS = 200;
    private static final long   DEFAULT_TIMEOUT_MS = 3000;

    protected final StallPosition seekHardStop(Motor motor, Direction direction, double power) throws InterruptedException {
        return seekHardStop(motor, direction, power, DEFAULT_TIMEOUT_MS);
    }

    protected final StallPosition seekHardStop(Motor motor, Direction direction, double power, long timeoutMs) throws InterruptedException {
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(direction == Direction.FORWARD ? power : -power);

        long start = System.currentTimeMillis();
        long stallStart = -1;
        int lastPos = motor.getCurrentPosition();

        while (System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(STALL_CHECK_INTERVAL_MS);
            int currentPos = motor.getCurrentPosition();
            boolean stalled = isStalled(motor, lastPos, currentPos, power);
            lastPos = currentPos;

            if (stalled) {
                if (stallStart < 0) stallStart = System.currentTimeMillis();
                if (System.currentTimeMillis() - stallStart >=  STALL_CONFIRM_DURATION_MS) {
                    motor.setPower(0);
                    return new StallPosition(currentPos, false);
                }
            } else {
                stallStart = -1;
            }
        }

        motor.setPower(0);
        return new StallPosition(motor.getCurrentPosition(), true);
    }

    protected final void zeroEncoder(Motor motor) {
        RunMode prev = motor.getMode();
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(prev == RunMode.STOP_AND_RESET_ENCODER ? RunMode.RUN_WITHOUT_ENCODER : prev);
    }

    protected final void waitForStall(Motor motor, double power, long timeoutMs) throws InterruptedException {
        seekHardStop(motor, power >= 0 ? Direction.FORWARD : Direction.REVERSE, Math.abs(power), timeoutMs);
    }

    protected final void moveTo(Motor motor, int ticks, double power) throws InterruptedException {
        motor.setTargetPosition(ticks);
        motor.setMode(RunMode.RUN_TO_POSITION);
        motor.setPower(power);
        while (motor.isBusy()) {
            Thread.sleep(20);
        }
        motor.setPower(0);
    }

    protected boolean isStalled(Motor motor, int previousPos, int currentPos, double power) {
        if (stallDetectedByVelocity(motor, power)) return true;
        if(stallDetectedByCurrent(motor, power))   return true;
        return Math.abs(currentPos - previousPos) <= STALL_POSITION_THRESHOLD;
    }

    protected boolean stallDetectedByVelocity(Motor motor, double power) { return false; }
    protected boolean stallDetectedByCurrent(Motor motor, double power) { return false; }

    protected final AssertionBuilder expect(double actual) {
        return new AssertionBuilder(actual);
    }

    protected final AssertionBuilder expect(int actual) {
        return new AssertionBuilder((double) actual);
    }

    public double estimatedCurrentDraw() { return 0; }
}