package dev.ftcplus.ftcruntime;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import dev.ftcplus.core.sensor.ImuDelegate;
import dev.ftcplus.core.sensor.ImuOrientation;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

final class FtcImu implements ImuDelegate {
    private final IMU imu;

    FtcImu(HardwareMap hardwareMap, String name, ImuOrientation orientation) {
        imu = hardwareMap.get(IMU.class, name);
        imu.initialize(new IMU.Parameters(toRevOrientation(orientation)));
    }

    @Override public double getYaw()   { return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES); }
    @Override public double getPitch() { return imu.getRobotYawPitchRollAngles().getPitch(AngleUnit.DEGREES); }
    @Override public double getRoll()  { return imu.getRobotYawPitchRollAngles().getRoll(AngleUnit.DEGREES); }
    @Override public void resetYaw()   { imu.resetYaw(); }

    private static RevHubOrientationOnRobot toRevOrientation(ImuOrientation orientation) {
        switch (orientation) {
            case LOGO_FACING_UP:       return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.UP,       RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            case LOGO_FACING_DOWN:     return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.DOWN,     RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            case LOGO_FACING_LEFT:     return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.LEFT,     RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            case LOGO_FACING_RIGHT:    return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,    RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            case LOGO_FACING_FORWARD:  return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.FORWARD,  RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            case LOGO_FACING_BACKWARD: return new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.BACKWARD, RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            default: throw new IllegalArgumentException("Unknown orientation: " + orientation);
        }
    }
}