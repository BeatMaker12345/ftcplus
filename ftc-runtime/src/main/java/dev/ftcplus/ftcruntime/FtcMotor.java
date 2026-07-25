package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.hardware.DcMotor;

import dev.ftcplus.core.motor.MotorDelegate;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;

final class FtcMotor implements MotorDelegate {

    private final DcMotor dcMotor;

    FtcMotor(DcMotor dcMotor) {
        this.dcMotor = dcMotor;
    }

    @Override public void setPower(double power)   { dcMotor.setPower(power); }
    @Override public double getPower()             { return dcMotor.getPower(); }
    @Override public void setTargetPosition(int t) { dcMotor.setTargetPosition(t); }
    @Override public int getTargetPosition()       { return dcMotor.getTargetPosition(); }
    @Override public int getCurrentPosition()      { return dcMotor.getCurrentPosition(); }
    @Override public boolean isBusy()              { return dcMotor.isBusy(); }
    @Override public boolean getPowerFloat()       { return dcMotor.getPowerFloat(); }
    @Override public String getDeviceName()        { return dcMotor.getDeviceName(); }

    @Override
    public void setDirection(dev.ftcplus.core.Direction direction) {
        dcMotor.setDirection(direction == dev.ftcplus.core.Direction.FORWARD
                ? DcMotor.Direction.FORWARD
                : DcMotor.Direction.REVERSE);
    }

    @Override
    public dev.ftcplus.core.Direction getDirection() {
        return dcMotor.getDirection() == DcMotor.Direction.FORWARD
                ? dev.ftcplus.core.Direction.FORWARD
                : dev.ftcplus.core.Direction.REVERSE;
    }

    @Override
    public void setMode(RunMode mode) {
        dcMotor.setMode(toSdkRunMode(mode));
    }

    @Override
    public RunMode getMode() {
        return fromSdkRunMode(dcMotor.getMode());
    }

    @Override
    public void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        dcMotor.setZeroPowerBehavior(toSdkZeroPowerBehavior(behavior));
    }

    @Override
    public ZeroPowerBehavior getZeroPowerBehavior() {
        return fromSdkZeroPowerBehavior(dcMotor.getZeroPowerBehavior());
    }

    private static DcMotor.RunMode toSdkRunMode(RunMode mode) {
        switch (mode) {
            case RUN_WITHOUT_ENCODER:    return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            case RUN_USING_ENCODER:      return DcMotor.RunMode.RUN_USING_ENCODER;
            case RUN_TO_POSITION:        return DcMotor.RunMode.RUN_TO_POSITION;
            case STOP_AND_RESET_ENCODER: return DcMotor.RunMode.STOP_AND_RESET_ENCODER;
            default: throw new IllegalArgumentException("Unknown RunMode: " + mode);
        }
    }

    private static RunMode fromSdkRunMode(DcMotor.RunMode mode) {
        switch (mode) {
            case RUN_WITHOUT_ENCODER:    return RunMode.RUN_WITHOUT_ENCODER;
            case RUN_USING_ENCODER:      return RunMode.RUN_USING_ENCODER;
            case RUN_TO_POSITION:        return RunMode.RUN_TO_POSITION;
            case STOP_AND_RESET_ENCODER: return RunMode.STOP_AND_RESET_ENCODER;
            default: throw new IllegalArgumentException("Unknown SDK RunMode: " + mode);
        }
    }

    private static DcMotor.ZeroPowerBehavior toSdkZeroPowerBehavior(ZeroPowerBehavior b) {
        switch (b) {
            case BRAKE: return DcMotor.ZeroPowerBehavior.BRAKE;
            case FLOAT: return DcMotor.ZeroPowerBehavior.FLOAT;
            default:    return DcMotor.ZeroPowerBehavior.UNKNOWN;
        }
    }

    private static ZeroPowerBehavior fromSdkZeroPowerBehavior(DcMotor.ZeroPowerBehavior b) {
        switch (b) {
            case BRAKE: return ZeroPowerBehavior.BRAKE;
            case FLOAT: return ZeroPowerBehavior.FLOAT;
            default:    return ZeroPowerBehavior.UNKNOWN;
        }
    }
}