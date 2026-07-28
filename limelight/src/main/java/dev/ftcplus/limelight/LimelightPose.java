package dev.ftcplus.limelight;

public final class LimelightPose {
    public final double x;
    public final double y;
    public final double z;
    public final double yawDegrees;
    public final double pitchDegrees;
    public final double rollDegrees;
    public final double latencyMs;
    public final int    tagCount;

    public LimelightPose(double x, double y, double z, double yawDegrees, double pitchDegrees, double rollDegrees, double latencyMs, int tagCount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.rollDegrees = rollDegrees;
        this.latencyMs = latencyMs;
        this.tagCount = tagCount;
    }

    public boolean isValid() { return tagCount > 0; }
}