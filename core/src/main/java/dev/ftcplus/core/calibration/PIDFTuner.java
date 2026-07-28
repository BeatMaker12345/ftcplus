package dev.ftcplus.core.calibration;

import dev.ftcplus.core.CalibrationResult;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class PIDFTuner {

    private final Motor motor;
    private double targetVelocity = 1000;
    private int    tolerance      = 75;
    private double kPMin          = 0.0001;
    private double kPMax          = 0.005;
    private double kFMin          = 0.0001;
    private double kFMax          = 0.001;
    private int    iterations     = 8;
    private long   settleMs       = 1000;
    private String kPFieldName    = "KP";
    private String kFFieldName    = "KF";

    private PIDFTuner(Motor motor) { this.motor = motor; }

    public static PIDFTuner of(Motor motor) { return new PIDFTuner(motor); }

    public PIDFTuner targetVelocity(double v)    { this.targetVelocity = v;   return this; }
    public PIDFTuner tolerance(int t)            { this.tolerance = t;         return this; }
    public PIDFTuner kPRange(double min, double max) { this.kPMin = min; this.kPMax = max; return this; }
    public PIDFTuner kFRange(double min, double max) { this.kFMin = min; this.kFMax = max; return this; }
    public PIDFTuner iterations(int n)           { this.iterations = n;        return this; }
    public PIDFTuner settleMs(long ms)           { this.settleMs = ms;         return this; }
    public PIDFTuner kPFieldName(String name)    { this.kPFieldName = name;    return this; }
    public PIDFTuner kFFieldName(String name)    { this.kFFieldName = name;    return this; }

    public CalibrationResult check() throws InterruptedException {
        motor.setMode(RunMode.RUN_USING_ENCODER);

        double bestKF = tuneKF();

        double bestKP = tuneKP(bestKF);

        motor.setPower(0);

        double achievedVelocity = measureVelocity(bestKP, bestKF);
        boolean withinTolerance = Math.abs(achievedVelocity - targetVelocity) <= tolerance;

        if (!withinTolerance) {
            return CalibrationResult.warn(
                "Could not reach target within tolerance. Best: kP=" + fmt(bestKP)
                + " kF=" + fmt(bestKF) + " → " + (int) achievedVelocity + " ticks/s",
                CalibrationResult.value(kPFieldName, bestKP, "tuned kP"),
                CalibrationResult.value(kFFieldName, bestKF, "tuned kF")
            );
        }

        return CalibrationResult.values(
            CalibrationResult.value(kPFieldName, bestKP, "tuned kP (ticks/s target: " + (int) targetVelocity + ")"),
            CalibrationResult.value(kFFieldName, bestKF, "tuned kF")
        );
    }

    private double tuneKF() throws InterruptedException {
        double lo = kFMin, hi = kFMax, best = kFMin;

        for (int i = 0; i < iterations; i++) {
            double mid = (lo + hi) / 2;
            double vel = measureVelocity(0, mid);

            if (vel < targetVelocity) {
                lo = mid;
            } else {
                best = mid;
                hi = mid;
            }
        }

        return best;
    }

    private double tuneKP(double kF) throws InterruptedException {
        double lo = kPMin, hi = kPMax, best = kPMin;
        double lastError = targetVelocity;

        for (int i = 0; i < iterations; i++) {
            double mid = (lo + hi) / 2;
            double vel = measureVelocity(mid, kF);
            double error = Math.abs(vel - targetVelocity);

            if (error < lastError) {
                best = mid;
                lastError = error;
            }

            if (vel < targetVelocity) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        return best;
    }

    private double measureVelocity(double kP, double kF) throws InterruptedException {
        long start = System.currentTimeMillis();
        double currentVel = 0;

        while (System.currentTimeMillis() - start < settleMs) {
            double error = targetVelocity - currentVel;
            double power = kP * error + kF * targetVelocity;
            power = Math.max(0, Math.min(1, power));
            motor.setPower(power);
            Thread.sleep(20);

            int pos1 = motor.getCurrentPosition();
            Thread.sleep(50);
            int pos2 = motor.getCurrentPosition();
            currentVel = (pos2 - pos1) * 20.0;
        }

        return currentVel;
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }
}