package dev.ftcplus.core.calibration;

import dev.ftcplus.core.CalibrationResult;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class WheelDiameter {

    private final Motor motor;
    private double distanceInches = 24.0;
    private double drivePower     = 0.4;
    private String fieldName      = "WHEEL_DIAMETER_INCHES";

    private WheelDiameter(Motor motor) { this.motor = motor; }

    public static WheelDiameter of(Motor motor) { return new WheelDiameter(motor); }

    public WheelDiameter distanceInches(double d) { this.distanceInches = d; return this; }
    public WheelDiameter drivePower(double power) { this.drivePower = power; return this; }
    public WheelDiameter fieldName(String name)   { this.fieldName = name;   return this; }

    public CalibrationResult check() throws InterruptedException {
        if (!motor.hasSpec()) {
            return CalibrationResult.failed(
                "WheelDiameter calibration requires a MotorSpec for TPR. Attach a spec first."
            );
        }

        double tpr = motor.getTicksPerRevolution();

        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);

        int ticksBefore = motor.getCurrentPosition();

        motor.setPower(drivePower);
        Thread.sleep(3000);
        motor.setPower(0);

        int ticksAfter = motor.getCurrentPosition();
        int ticksTraveled = Math.abs(ticksAfter - ticksBefore);

        if (ticksTraveled < 10) {
            return CalibrationResult.failed(
                "Motor did not move — check connection and power."
            );
        }

        double revolutions = ticksTraveled / tpr;
        double circumference = distanceInches / revolutions;
        double diameter = circumference / Math.PI;

        return CalibrationResult.offset(fieldName, diameter);
    }
}