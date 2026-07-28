package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class MotorCurrentDiagnostic {

    private final Motor motor;
    private double testPower       = 0.5;
    private long   testDurationMs  = 500;
    private double minAmps         = 0.1;
    private double maxAmps         = 5.0;

    private MotorCurrentDiagnostic(Motor motor) { this.motor = motor; }

    public static MotorCurrentDiagnostic of(Motor motor) {
        return new MotorCurrentDiagnostic(motor);
    }

    public MotorCurrentDiagnostic testPower(double power)  { this.testPower = power;      return this; }
    public MotorCurrentDiagnostic duration(long ms)        { this.testDurationMs = ms;    return this; }
    public MotorCurrentDiagnostic minAmps(double amps)     { this.minAmps = amps;         return this; }
    public MotorCurrentDiagnostic maxAmps(double amps)     { this.maxAmps = amps;         return this; }

    public DiagnosticResult check() throws InterruptedException {
        if (!motor.hasSpec()) {
            return DiagnosticResult.warn(
                "No MotorSpec attached — cannot estimate current draw."
            );
        }

        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(testPower);
        Thread.sleep(testDurationMs);

        double current = motor.estimateCurrentDraw();
        motor.setPower(0);

        return new AssertionBuilder(current)
            .between(minAmps, maxAmps)
            .otherwise("Current draw out of range — motor may be stalled, damaged, or wired incorrectly.")
            .result();
    }
}