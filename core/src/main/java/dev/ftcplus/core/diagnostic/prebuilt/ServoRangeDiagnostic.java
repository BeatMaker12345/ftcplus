package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.servo.Servo;

public final class ServoRangeDiagnostic {

    private final Servo servo;
    private long   settleDurationMs  = 500;
    private double positionTolerance = 0.02;

    private ServoRangeDiagnostic(Servo servo) { this.servo = servo; }

    public static ServoRangeDiagnostic of(Servo servo) {
        return new ServoRangeDiagnostic(servo);
    }

    public ServoRangeDiagnostic settleMs(long ms)          { this.settleDurationMs = ms;       return this; }
    public ServoRangeDiagnostic tolerance(double t)        { this.positionTolerance = t;       return this; }

    public DiagnosticResult check() throws InterruptedException {
        servo.setPosition(0.0);
        Thread.sleep(settleDurationMs);
        double atMin = servo.getPosition();

        servo.setPosition(1.0);
        Thread.sleep(settleDurationMs);
        double atMax = servo.getPosition();

        servo.setPosition(0.5);

        boolean minOk = Math.abs(atMin - 0.0) <= positionTolerance;
        boolean maxOk = Math.abs(atMax - 1.0) <= positionTolerance;

        if (minOk && maxOk)   return DiagnosticResult.pass("Servo reaches both endpoints.");
        if (!minOk && !maxOk) return DiagnosticResult.fail("Servo not reaching either endpoint — check connection.");
        if (!minOk)           return DiagnosticResult.fail("Servo not reaching minimum — check for mechanical binding.");
        return                       DiagnosticResult.fail("Servo not reaching maximum — check for mechanical binding.");
    }
}