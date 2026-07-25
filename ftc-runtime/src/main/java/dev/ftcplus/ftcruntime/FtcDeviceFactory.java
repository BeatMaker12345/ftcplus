package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import dev.ftcplus.core.DeviceFactory;
import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorDelegate;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.servo.CRServoDelegate;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.core.servo.ServoDelegate;
import dev.ftcplus.core.servo.ServoSpec;

import java.util.Objects;

public final class FtcDeviceFactory implements DeviceFactory {

    private final HardwareMap hardwareMap;

    public FtcDeviceFactory(HardwareMap hardwareMap) {
        this.hardwareMap = Objects.requireNonNull(hardwareMap, "hardwareMap");
    }

    @Override
    public MotorDelegate createMotorDelegate(HardwareEntry entry, MotorSpec spec) {
        return new FtcMotor(hardwareMap.get(DcMotor.class, entry.hardwareName()));
    }

    @Override
    public MotorDelegate createMotorDelegate(HardwareEntry entry) {
        return new FtcMotor(hardwareMap.get(DcMotor.class, entry.hardwareName()));
    }

    @Override
    public ServoDelegate createServoDelegate(HardwareEntry entry, ServoSpec spec) {
        return new FtcServo(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo.class, entry.hardwareName())
        );
    }

    @Override
    public ServoDelegate createServoDelegate(HardwareEntry entry) {
        return new FtcServo(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo.class, entry.hardwareName())
        );
    }

    @Override
    public CRServoDelegate createCRServoDelegate(HardwareEntry entry, CRServoSpec spec) {
        return new FtcCRServo(
                hardwareMap.get(com.qualcomm.robotcore.hardware.CRServo.class, entry.hardwareName())
        );
    }

    @Override
    public CRServoDelegate createCRServoDelegate(HardwareEntry entry) {
        return new FtcCRServo(
                hardwareMap.get(com.qualcomm.robotcore.hardware.CRServo.class, entry.hardwareName())
        );
    }
}