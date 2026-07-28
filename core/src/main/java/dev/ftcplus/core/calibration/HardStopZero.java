package dev.ftcplus.core.calibration;

import dev.ftcplus.core.CalibrationResult;
import dev.ftcplus.core.Direction;
import dev.ftcplus.core.diagnostic.StallPosition;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.RunMode;

public final class HardStopZero {

    private final Motor motor;
    private Direction direction  = Direction.REVERSE;
    private double    seekPower  = 0.3;
    private long      timeoutMs  = 3000;
    private String    fieldName  = "ZERO_OFFSET";

    private HardStopZero(Motor motor) { this.motor = motor; }

    public static HardStopZero of(Motor motor) { return new HardStopZero(motor); }

    public HardStopZero direction(Direction d)  { this.direction = d;   return this; }
    public HardStopZero seekPower(double power)  { this.seekPower = power; return this; }
    public HardStopZero timeout(long ms)         { this.timeoutMs = ms;  return this; }
    public HardStopZero fieldName(String name)   { this.fieldName = name; return this; }

    public CalibrationResult check() throws InterruptedException {
        StallPosition pos = seekHardStop();

        if (pos.timedOut()) {
            return CalibrationResult.failed(
                "Timed out seeking hard stop — check for obstruction or disconnected motor."
            );
        }

        zeroEncoder();

        return CalibrationResult.offset(fieldName, pos.ticks());
    }

    private StallPosition seekHardStop() throws InterruptedException {
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(direction == Direction.FORWARD ? seekPower : -seekPower);

        long start = System.currentTimeMillis();
        long stallStart = -1;
        int lastPos = motor.getCurrentPosition();

        while (System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(50);
            int currentPos = motor.getCurrentPosition();
            boolean stalled = Math.abs(currentPos - lastPos) <= 3;
            lastPos = currentPos;

            if (stalled) {
                if (stallStart < 0) stallStart = System.currentTimeMillis();
                if (System.currentTimeMillis() - stallStart >= 200) {
                    motor.setPower(0);
                    return new StallPosition(currentPos, false);
                }
            } else {
                stallStart = -1;
            }
        }

        motor.setPower(0);
        return new StallPosition(motor.getCurrentPosition(), true);
    }

    private void zeroEncoder() {
        motor.setMode(RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(RunMode.RUN_WITHOUT_ENCODER);
    }
}