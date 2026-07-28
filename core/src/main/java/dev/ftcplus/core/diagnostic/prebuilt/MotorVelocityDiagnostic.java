package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class MotorVelocityDiagnostic {

    private final Motor motor;
    private double targetTicksPerSecond = -1;
    private int    tolerance            = 100;
    private double testPower            = 0.8;
    private long   spinUpMs             = 1500;

    private MotorVelocityDiagnostic(Motor motor) { this.motor = motor; }

    public static MotorVelocityDiagnostic of(Motor motor) {
        return new MotorVelocityDiagnostic(motor);
    }

    public MotorVelocityDiagnostic target(double ticksPerSecond) { this.targetTicksPerSecond = ticksPerSecond; return this; }
    public MotorVelocityDiagnostic tolerance(int ticks)          { this.tolerance = ticks;                     return this; }
    public MotorVelocityDiagnostic testPower(double power)       { this.testPower = power;                     return this; }
    public MotorVelocityDiagnostic spinUpMs(long ms)             { this.spinUpMs = ms;                         return this; }

    public DiagnosticResult check() throws InterruptedException {
        motor.setMode(RunMode.RUN_USING_ENCODER);
        motor.setPower(testPower);
        Thread.sleep(spinUpMs);

        int pos1 = motor.getCurrentPosition();
        Thread.sleep(100);
        int pos2 = motor.getCurrentPosition();
        motor.setPower(0);

        double ticksPerSecond = (pos2 - pos1) * 10.0;

        if (targetTicksPerSecond < 0) {
            return DiagnosticResult.pass("Velocity: " + (int) ticksPerSecond + " ticks/s");
        }

        return new AssertionBuilder(ticksPerSecond)
            .within(tolerance).of(targetTicksPerSecond)
            .otherwise("Motor not reaching target velocity — check battery, motor, and controller.")
            .result();
    }
}