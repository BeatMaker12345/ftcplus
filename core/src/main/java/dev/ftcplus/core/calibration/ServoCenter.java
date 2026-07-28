package dev.ftcplus.core.calibration;

import dev.ftcplus.core.CalibrationResult;
import dev.ftcplus.core.servo.Servo;

public final class ServoCenter {

    private final Servo servo;
    private long   settleDurationMs = 500;
    private String fieldName        = "CENTER_POSITION";

    private ServoCenter(Servo servo) { this.servo = servo; }

    public static ServoCenter of(Servo servo) { return new ServoCenter(servo); }

    public ServoCenter settleMs(long ms)      { this.settleDurationMs = ms; return this; }
    public ServoCenter fieldName(String name) { this.fieldName = name;      return this; }

    public CalibrationResult check() throws InterruptedException {
        servo.setPosition(0.0);
        Thread.sleep(settleDurationMs);
        double atMin = servo.getPosition();

        servo.setPosition(1.0);
        Thread.sleep(settleDurationMs);
        double atMax = servo.getPosition();

        double center = (atMin + atMax) / 2.0;

        servo.setPosition(center);
        Thread.sleep(settleDurationMs);
        double atCenter = servo.getPosition();

        if (Math.abs(atCenter - center) > 0.05) {
            return CalibrationResult.warn(
                "Center position may be inaccurate — servo response inconsistent.",
                CalibrationResult.value(fieldName, center, "measured center position")
            );
        }

        return CalibrationResult.offset(fieldName, center);
    }
}