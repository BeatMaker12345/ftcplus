package dev.ftcplus.core.servo;

public final class CRServoSpec {
    public final double freeSpeedRpm;
    public final double stallCurrentAmps;
    public final double freeCurrentAmps;
    public final double stallTorqueNm;
    public final double massGrams;

    public CRServoSpec(
            double freeSpeedRpm,
            double stallCurrentAmps,
            double freeCurrentAmps,
            double stallTorqueNm,
            double massGrams
    ) {
        this.freeSpeedRpm = freeSpeedRpm;
        this.stallCurrentAmps = stallCurrentAmps;
        this.freeCurrentAmps = freeCurrentAmps;
        this.stallTorqueNm = stallTorqueNm;
        this.massGrams = massGrams;
    }
}