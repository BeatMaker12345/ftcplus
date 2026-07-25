package dev.ftcplus.core.motor;

public final class MotorSpec {
    public final double ticksPerRevolution;
    public final double freeSpeedRpm;
    public final double stallCurrentAmps;
    public final double freeCurrentAmps;
    public final double stallTorqueNm;
    public final double massGrams;

    public MotorSpec(
            double ticksPerRevolution,
            double freeSpeedRpm,
            double stallCurrentAmps,
            double freeCurrentAmps,
            double stallTorqueNm,
            double massGrams
    ) {
        this.ticksPerRevolution = ticksPerRevolution;
        this.freeSpeedRpm = freeSpeedRpm;
        this.stallCurrentAmps = stallCurrentAmps;
        this.freeCurrentAmps = freeCurrentAmps;
        this.stallTorqueNm = stallTorqueNm;
        this.massGrams = massGrams;
    }
}
