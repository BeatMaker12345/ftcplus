package dev.ftcplus.ftcruntime;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.servo.ServoDelegate;

final class FtcServo implements ServoDelegate {

    private final com.qualcomm.robotcore.hardware.Servo sdkServo;

    FtcServo(com.qualcomm.robotcore.hardware.Servo sdkServo) {
        this.sdkServo = sdkServo;
    }

    @Override public void setPosition(double position)      { sdkServo.setPosition(position); }
    @Override public double getPosition()                   { return sdkServo.getPosition(); }
    @Override public void scaleRange(double min, double max){ sdkServo.scaleRange(min, max); }
    @Override public String getDeviceName()                 { return sdkServo.getDeviceName(); }

    @Override
    public void setDirection(Direction direction) {
        sdkServo.setDirection(direction == Direction.FORWARD
                ? com.qualcomm.robotcore.hardware.Servo.Direction.FORWARD
                : com.qualcomm.robotcore.hardware.Servo.Direction.REVERSE);
    }

    @Override
    public Direction getDirection() {
        return sdkServo.getDirection() == com.qualcomm.robotcore.hardware.Servo.Direction.FORWARD
                ? Direction.FORWARD
                : Direction.REVERSE;
    }
}