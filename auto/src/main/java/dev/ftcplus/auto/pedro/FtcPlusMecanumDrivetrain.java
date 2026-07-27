package dev.ftcplus.auto.pedro;

import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.math.Vector;

import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.core.motor.ZeroPowerBehavior;

public class FtcPlusMecanumDrivetrain extends Drivetrain {

    private final Motor frontLeft;
    private final Motor frontRight;
    private final Motor backLeft;
    private final Motor backRight;

    private double xVelocity = 0;
    private double yVelocity = 0;

    public FtcPlusMecanumDrivetrain(Motor frontLeft, Motor frontRight, Motor backLeft, Motor backRight) {
        this.frontLeft  = frontLeft;
        this.frontRight = frontRight;
        this.backLeft   = backLeft;
        this.backRight  = backRight;

        maxPowerScaling = 1.0;
        voltageCompensation = false;
        nominalVoltage = 12.0;

        Vector fl = new Vector(1, Math.PI / 4);
        vectors = new Vector[]{
            new Vector(fl.getMagnitude(), fl.getTheta()),
            new Vector(fl.getMagnitude(), Math.PI - fl.getTheta()),
            new Vector(fl.getMagnitude(), Math.PI - fl.getTheta()),
            new Vector(fl.getMagnitude(), fl.getTheta())
        };
    }

    @Override
    public double[] calculateDrive(Vector correctivePower, Vector headingPower, Vector pathingPower, double robotHeading) {
        Vector[] inputs = new Vector[]{
            clampVector(correctivePower, 1),
            clampVector(headingPower, 1),
            clampVector(pathingPower, 1)
        };

        double[] powers = new double[4];
        for (Vector input : inputs) {
            powers[0] += input.getXComponent() * vectors[0].getXComponent() + input.getYComponent() * vectors[0].getYComponent();
            powers[1] += input.getXComponent() * vectors[1].getXComponent() + input.getYComponent() * vectors[1].getYComponent();
            powers[2] += input.getXComponent() * vectors[2].getXComponent() + input.getYComponent() * vectors[2].getYComponent();
            powers[3] += input.getXComponent() * vectors[3].getXComponent() + input.getYComponent() * vectors[3].getYComponent();
        }

        double max = 1.0;
        for (double p : powers) max = Math.max(max, Math.abs(p));
        for (int i = 0; i < 4; i++) powers[i] = powers[i] / max * maxPowerScaling;

        return powers;
    }

    @Override
    public void runDrive(double[] drivePowers) {
        frontLeft.setPower(drivePowers[0]);
        backLeft.setPower(drivePowers[1]);
        frontRight.setPower(drivePowers[2]);
        backRight.setPower(drivePowers[3]);
    }

    @Override public void startTeleopDrive()                   { setZeroPowerFloat(); }
    @Override public void startTeleopDrive(boolean brakeMode)  { if (brakeMode) setZeroPowerBrake(); else setZeroPowerFloat(); }
    @Override public void breakFollowing()                     { frontLeft.setPower(0); frontRight.setPower(0); backLeft.setPower(0); backRight.setPower(0); }
    @Override public void updateConstants()                    {}
    @Override public double xVelocity()                        { return xVelocity; }
    @Override public double yVelocity()                        { return yVelocity; }
    @Override public void setXVelocity(double x)               { this.xVelocity = x; }
    @Override public void setYVelocity(double y)               { this.yVelocity = y; }
    @Override public double getVoltage()                       { return nominalVoltage; }
    @Override public String debugString()                      { return "FtcPlusMecanumDrivetrain FL=" + frontLeft.getPower() + " FR=" + frontRight.getPower() + " BL=" + backLeft.getPower() + " BR=" + backRight.getPower(); }

    private void setZeroPowerBrake() { for (Motor m : new Motor[]{frontLeft, frontRight, backLeft, backRight}) m.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE); }
    private void setZeroPowerFloat() { for (Motor m : new Motor[]{frontLeft, frontRight, backLeft, backRight}) m.setZeroPowerBehavior(ZeroPowerBehavior.FLOAT); }

    private static Vector clampVector(Vector v, double max) {
        if (v.getMagnitude() > max) {
            return new Vector(max, v.getTheta());
        }
        return v;
    }
}