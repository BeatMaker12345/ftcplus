package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class EncoderDiagnostic {

    private final Motor motor;
    private double testPower        = 0.3;
    private long   testDurationMs   = 300;
    private int    minExpectedTicks = 10;

    private EncoderDiagnostic(Motor motor) { this.motor = motor; }

    public static EncoderDiagnostic of(Motor motor) {
        return new EncoderDiagnostic(motor);
    }

    public EncoderDiagnostic testPower(double power)     { this.testPower = power;               return this; }
    public EncoderDiagnostic duration(long ms)           { this.testDurationMs = ms;             return this; }
    public EncoderDiagnostic minTicks(int ticks)         { this.minExpectedTicks = ticks;        return this; }

    public DiagnosticResult check() throws InterruptedException {
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);

        int before = motor.getCurrentPosition();
        motor.setPower(testPower);
        Thread.sleep(testDurationMs);
        motor.setPower(0);
        int after = motor.getCurrentPosition();

        return new AssertionBuilder(Math.abs(after - before))
            .greaterThan(minExpectedTicks)
            .otherwise("Encoder not responding — check encoder cable and port assignment.")
            .result();
    }
}