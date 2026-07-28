package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.servo.Servo;

public final class ServoCenterDiagnostic {

    private final Servo servo;
    private long   settleDurationMs  = 500;
    private double positionTolerance = 0.05;

    private ServoCenterDiagnostic(Servo servo) { this.servo = servo; }

    public static ServoCenterDiagnostic of(Servo servo) {
        return new ServoCenterDiagnostic(servo);
    }

    public ServoCenterDiagnostic settleMs(long ms)     { this.settleDurationMs = ms;   return this; }
    public ServoCenterDiagnostic tolerance(double t)   { this.positionTolerance = t;   return this; }

    public DiagnosticResult check() throws InterruptedException {
        servo.setPosition(0.0);
        Thread.sleep(settleDurationMs);
        servo.setPosition(0.5);
        Thread.sleep(settleDurationMs);
        double pos = servo.getPosition();

        return new AssertionBuilder(pos)
            .within(positionTolerance).of(0.5)
            .otherwise("Servo not responding — check connection, port, and control hub config name.")
            .result();
    }
}