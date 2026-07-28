package dev.ftcplus.core;

public class RobotProperties {
    public double trackWidthInches() { return 12.0; }
    public double wheelDiameterInches() { return 3.78; }
    public double wheelbaseInches() { return 12.0; }
    public double massKg() { return 10.0; }

    public double maxLinearVelocityCmPerSecond() { return 45.0; }
    public double maxAngularVelocityDegreesPerSecond() { return 180.0; }

    public double nominalVoltage() { return 13.5; }
    public double maxCurrentAmps() { return 20.0; }

    public static double cmPerSecond(double value) { return value; }
    public static double inchesPerSecond(double value) { return value * 2.54; }
    public static double metersPerSecond(double value) { return value * 100.0; }
    public static double degreesPerSecond(double value) { return value; }
    public static double radiansPerSecond(double value) { return Math.toDegrees(value); }
}