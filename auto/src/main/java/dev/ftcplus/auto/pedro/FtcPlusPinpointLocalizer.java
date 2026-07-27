package dev.ftcplus.auto.pedro;

import com.pedropathing.geometry.Pose;
import com.pedropathing.localization.Localizer;
import com.pedropathing.math.MathFunctions;
import com.pedropathing.math.Vector;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

public class FtcPlusPinpointLocalizer implements Localizer {

    private final GoBildaPinpointDriver odo;
    private Pose startPose;
    private Pose currentVelocity = new Pose();
    private double totalHeading = 0;
    private double previousHeading = 0;

    public FtcPlusPinpointLocalizer(
            HardwareMap hardwareMap,
            String deviceName,
            double forwardPodYInches,
            double strafePodXInches,
            GoBildaPinpointDriver.GoBildaOdometryPods podType
    ) {
        this(hardwareMap, deviceName, forwardPodYInches, strafePodXInches, podType, new Pose());
    }

    public FtcPlusPinpointLocalizer(
            HardwareMap hardwareMap,
            String deviceName,
            double forwardPodYInches,
            double strafePodXInches,
            GoBildaPinpointDriver.GoBildaOdometryPods podType,
            Pose startPose
    ) {
        odo = hardwareMap.get(GoBildaPinpointDriver.class, deviceName);
        odo.setOffsets(forwardPodYInches, strafePodXInches, DistanceUnit.INCH);
        odo.setEncoderResolution(podType);
        odo.setEncoderDirections(
                GoBildaPinpointDriver.EncoderDirection.FORWARD,
                GoBildaPinpointDriver.EncoderDirection.FORWARD
        );
        setStartPose(startPose);
    }

    @Override
    public Pose getPose() {
        Pose2D pos = odo.getPosition();
        return new Pose(
                pos.getX(DistanceUnit.INCH),
                pos.getY(DistanceUnit.INCH),
                pos.getHeading(AngleUnit.RADIANS)
        ).plus(startPose);
    }

    @Override
    public Pose getVelocity() { return currentVelocity; }

    @Override
    public Vector getVelocityVector() {
        return new Vector(
                Math.hypot(currentVelocity.getX(), currentVelocity.getY()),
                Math.atan2(currentVelocity.getY(), currentVelocity.getX())
        );
    }

    @Override
    public void setStartPose(Pose setStart) {
        this.startPose = setStart;
        previousHeading = setStart.getHeading();
        odo.resetPosAndIMU();
    }

    @Override
    public void setPose(Pose setPose) {
        startPose = setPose.minus(getPose()).plus(startPose);
    }

    @Override
    public void update() {
        odo.update();
        double heading = getPose().getHeading();
        totalHeading += MathFunctions.getSmallestAngleDifference(heading, previousHeading);
        previousHeading = heading;
        currentVelocity = new Pose(
                odo.getVelX(DistanceUnit.INCH),
                odo.getVelY(DistanceUnit.INCH),
                odo.getHeadingVelocity(org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit.RADIANS)
        );
    }

    @Override public double getTotalHeading()      { return totalHeading; }
    @Override public double getForwardMultiplier() { return 1; }
    @Override public double getLateralMultiplier() { return 1; }
    @Override public double getTurningMultiplier() { return 1; }
    @Override public void resetIMU()               { odo.resetPosAndIMU(); }
    @Override public double getIMUHeading()        { return getPose().getHeading(); }

    @Override
    public boolean isNAN() {
        Pose p = getPose();
        return Double.isNaN(p.getX()) || Double.isNaN(p.getY()) || Double.isNaN(p.getHeading());
    }
}