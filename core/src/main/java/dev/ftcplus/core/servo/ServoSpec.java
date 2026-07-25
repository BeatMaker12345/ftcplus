package dev.ftcplus.core.servo;

public final class ServoSpec {
    public final double travelDegrees;
    public final double centerPosition;
    public final double stallCurrentAmps;
    public final double speedSecondsPerSixtyDegrees;
    public final double stallTorqueNm;
    public final double massGrams;

    public ServoSpec(
            double travelDegrees,
            double centerPosition,
            double stallCurrentAmps,
            double speedSecondsPerSixtyDegrees,
            double stallTorqueNm,
            double massGrams
    ) {
        this.travelDegrees = travelDegrees;
        this.centerPosition = centerPosition;
        this.stallCurrentAmps = stallCurrentAmps;
        this.speedSecondsPerSixtyDegrees = speedSecondsPerSixtyDegrees;
        this.stallTorqueNm = stallTorqueNm;
        this.massGrams = massGrams;
    }
}