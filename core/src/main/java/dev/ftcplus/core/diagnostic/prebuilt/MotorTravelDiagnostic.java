package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.Direction;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.diagnostic.StallPosition;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class MotorTravelDiagnostic {

    private final Motor motor;
    private int    expectedRange = -1;
    private int    tolerance     = 30;
    private double seekPower     = 0.3;
    private long   timeoutMs     = 3000;

    private MotorTravelDiagnostic(Motor motor) { this.motor = motor; }

    public static MotorTravelDiagnostic of(Motor motor) {
        return new MotorTravelDiagnostic(motor);
    }

    public MotorTravelDiagnostic expectedRange(int ticks)  { this.expectedRange = ticks; return this; }
    public MotorTravelDiagnostic tolerance(int ticks)      { this.tolerance = ticks;     return this; }
    public MotorTravelDiagnostic seekPower(double power)   { this.seekPower = power;     return this; }
    public MotorTravelDiagnostic timeout(long ms)          { this.timeoutMs = ms;        return this; }

    public DiagnosticResult check() throws InterruptedException {
        StallPosition min = seekHardStop(Direction.REVERSE);
        if (min.timedOut()) return DiagnosticResult.fail(
            "Timed out seeking minimum — check for obstruction or disconnected motor."
        );

        zeroEncoder();

        StallPosition max = seekHardStop(Direction.FORWARD);
        if (max.timedOut()) return DiagnosticResult.fail(
            "Timed out seeking maximum — check for obstruction."
        );

        seekHardStop(Direction.REVERSE);
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);

        if (expectedRange < 0) {
            return DiagnosticResult.pass("Travel range: " + max.ticks() + " ticks.");
        }

        return new AssertionBuilder(max.ticks())
            .within(tolerance).of(expectedRange)
            .otherwise("Travel range mismatch — check hard stops and mechanical binding.")
            .result();
    }

    private StallPosition seekHardStop(Direction direction) throws InterruptedException {
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(direction == Direction.FORWARD ? seekPower : -seekPower);

        long start = System.currentTimeMillis();
        long stallStart = -1;
        int lastPos = motor.getCurrentPosition();

        while (System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(50);
            int currentPos = motor.getCurrentPosition();
            boolean stalled = Math.abs(currentPos - lastPos) <= 3;
            lastPos = currentPos;

            if (stalled) {
                if (stallStart < 0) stallStart = System.currentTimeMillis();
                if (System.currentTimeMillis() - stallStart >= 200) {
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

    private void zeroEncoder() {
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
    }
}