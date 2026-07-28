package dev.ftcplus.core.diagnostic.prebuilt;

import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.diagnostic.AssertionBuilder;
import dev.ftcplus.core.servo.CRServo;

public final class CRServoRespondsDiagnostic {

    private final CRServo crServo;
    private long testDurationMs = 300;

    private CRServoRespondsDiagnostic(CRServo crServo) { this.crServo = crServo; }

    public static CRServoRespondsDiagnostic of(CRServo crServo) {
        return new CRServoRespondsDiagnostic(crServo);
    }

    public CRServoRespondsDiagnostic duration(long ms) { this.testDurationMs = ms; return this; }

    public DiagnosticResult check() throws InterruptedException {
        try {
            crServo.setPower(0.3);
            Thread.sleep(testDurationMs);
            crServo.setPower(0);

            return new AssertionBuilder(Math.abs(crServo.getPower()))
                .lessThan(0.05)
                .otherwise("CRServo not responding — check connection and config name.")
                .result();
        } catch (Exception e) {
            return DiagnosticResult.fail(
                "CRServo threw exception: " + e.getMessage()
                + " — check control hub config name."
            );
        }
    }
}