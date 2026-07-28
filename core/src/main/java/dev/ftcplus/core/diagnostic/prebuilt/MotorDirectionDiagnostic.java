package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class MotorDirectionDiagnostic {

    private final Motor motor;
    private double testPower       = 0.3;
    private long   testDurationMs  = 300;

    private MotorDirectionDiagnostic(Motor motor) { this.motor = motor; }

    public static MotorDirectionDiagnostic of(Motor motor) {
        return new MotorDirectionDiagnostic(motor);
    }

    public MotorDirectionDiagnostic testPower(double power)  { this.testPower = power;      return this; }
    public MotorDirectionDiagnostic duration(long ms)        { this.testDurationMs = ms;    return this; }

    public DiagnosticResult check() throws InterruptedException {
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);

        int before = motor.getCurrentPosition();
        motor.setPower(testPower);
        Thread.sleep(testDurationMs);
        motor.setPower(0);
        int after = motor.getCurrentPosition();

        if (after > before) {
            return DiagnosticResult.pass("Direction correct (" + (after - before) + " ticks forward)");
        } else if (after < before) {
            return DiagnosticResult.fail(
                "Motor runs in reverse when commanded forward — check Direction setting or wiring."
            );
        } else {
            return DiagnosticResult.fail("Motor did not move — check connection and power.");
        }
    }
}