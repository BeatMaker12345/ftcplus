package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.hardware.CRServo;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.servo.CRServoDelegate;

final class FtcCRServo implements CRServoDelegate {

    private final CRServo sdkCRServo;

    FtcCRServo(CRServo sdkCRServo) {
        this.sdkCRServo = sdkCRServo;
    }

    @Override public void setPower(double power)  { sdkCRServo.setPower(power); }
    @Override public double getPower()            { return sdkCRServo.getPower(); }
    @Override public String getDeviceName()       { return sdkCRServo.getDeviceName(); }

    @Override
    public void setDirection(Direction direction) {
        sdkCRServo.setDirection(direction == Direction.FORWARD
                ? CRServo.Direction.FORWARD
                : CRServo.Direction.REVERSE);
    }

    @Override
    public Direction getDirection() {
        return sdkCRServo.getDirection() == CRServo.Direction.FORWARD
                ? Direction.FORWARD
                : Direction.REVERSE;
    }
}