package dev.ftcplus.core.calibration;

import dev.ftcplus.core.CalibrationResult;
import dev.ftcplus.core.sensor.Imu;

public final class IMUOffset {

    private final Imu imu;
    private double expectedHeadingDegrees = 0.0;
    private long   settleDurationMs       = 500;
    private int    sampleCount            = 10;
    private String fieldName              = "IMU_HEADING_OFFSET";

    private IMUOffset(Imu imu) { this.imu = imu; }

    public static IMUOffset of(Imu imu) { return new IMUOffset(imu); }

    public IMUOffset expectedHeading(double degrees) { this.expectedHeadingDegrees = degrees; return this; }
    public IMUOffset settleMs(long ms)               { this.settleDurationMs = ms;             return this; }
    public IMUOffset samples(int n)                  { this.sampleCount = n;                   return this; }
    public IMUOffset fieldName(String name)          { this.fieldName = name;                  return this; }

    public CalibrationResult check() throws InterruptedException {
        Thread.sleep(settleDurationMs);

        double sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += imu.getYaw();
            Thread.sleep(20);
        }
        double measuredHeading = sum / sampleCount;
        double offset = expectedHeadingDegrees - measuredHeading;

        while (offset > 180)  offset -= 360;
        while (offset < -180) offset += 360;

        if (Math.abs(measuredHeading) > 45 && expectedHeadingDegrees == 0.0) {
            return CalibrationResult.warn(
                "Measured heading " + fmt(measuredHeading) + "° is far from expected 0° — "
                + "is the robot pointing forward? Check IMU orientation setting.",
                CalibrationResult.value(fieldName, offset, "heading offset (degrees)")
            );
        }

        return CalibrationResult.offset(fieldName, offset);
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}