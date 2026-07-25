package dev.ftcplus.core;

import dev.ftcplus.core.motor.MotorDelegate;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.servo.CRServoDelegate;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.core.servo.ServoDelegate;
import dev.ftcplus.core.servo.ServoSpec;

public interface DeviceFactory {
    MotorDelegate createMotorDelegate(HardwareEntry entry, MotorSpec spec);
    MotorDelegate createMotorDelegate(HardwareEntry entry);

    ServoDelegate createServoDelegate(HardwareEntry entry, ServoSpec spec);
    ServoDelegate createServoDelegate(HardwareEntry entry);

    CRServoDelegate createCRServoDelegate(HardwareEntry entry, CRServoSpec spec);
    CRServoDelegate createCRServoDelegate(HardwareEntry entry);
}