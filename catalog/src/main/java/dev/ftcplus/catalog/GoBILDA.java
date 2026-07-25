// GoBILDA.java
package dev.ftcplus.catalog;

import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.servo.ServoSpec;

/**
 * Part catalog for goBILDA hardware.
 *
 * Motor specs are at 12VDC. Torque values converted from kg.cm to N.m.
 * Encoder PPR is at the output shaft (post-gearbox).
 * All Yellow Jacket motors share the same RS-555 base motor and stall/free
 * current characteristics — only ratio-dependent values differ.
 *
 * Usage:
 *   motor(Hardware.INTAKE, GoBILDA.YellowJacket.W5203_435RPM)
 */
public final class GoBILDA {

    private GoBILDA() {}

    // 1 kg.cm = 0.0980665 N.m
    private static double kgCmToNm(double kgCm) {
        return kgCm * 0.0980665;
    }

    /**
     * Yellow Jacket Planetary Gear Motors (5202, 5203, 5204 series).
     *
     * All variants share the same RS-555 base motor:
     *   Free current:  0.25A @ 12V
     *   Stall current: 9.2A  @ 12V
     *   Weight:        ~438g
     *
     * Naming convention: W{series}_{freeSpeedRpm}RPM
     * Series:
     *   5202 — 6mm D-shaft, 24mm length
     *   5203 — 8mm REX shaft, 24mm length
     *   5204 — 8mm REX shaft, 80mm length
     */
    public static final class YellowJacket {

        private YellowJacket() {}

        private static MotorSpec make(double rpm, double torqueKgCm, double ppr) {
            return new MotorSpec(
                    ppr,           // ticksPerRevolution
                    rpm,           // freeSpeedRpm
                    9.2,           // stallCurrentAmps
                    0.25,          // freeCurrentAmps
                    kgCmToNm(torqueKgCm), // stallTorqueNm
                    438            // massGrams
            );
        }

        // -------------------------------------------------------------------------
        // 5203 Series — 8mm REX, 24mm shaft (most common in FTC)
        // -------------------------------------------------------------------------

        /** 5203-2402-0001 — 1:1, 6000 RPM, no gearbox */
        public static final MotorSpec W5203_6000RPM =
                make(6000, 1.47, 28);

        /** 5203-2402-0003 — 3.7:1, 1620 RPM */
        public static final MotorSpec W5203_1620RPM =
                make(1620, 5.4, 103.6);

        /** 5203-2402-0005 — 5.2:1, 1150 RPM */
        public static final MotorSpec W5203_1150RPM =
                make(1150, 7.9, 145.6);

        /** 5203-2402-0014 — 13.7:1, 435 RPM */
        public static final MotorSpec W5203_435RPM =
                make(435, 18.7, 384.5);

        /** 5203-2402-0019 — 19.2:1, 312 RPM */
        public static final MotorSpec W5203_312RPM =
                make(312, 24.3, 537.6);

        /** 5203-2402-0027 — 26.9:1, 223 RPM */
        public static final MotorSpec W5203_223RPM =
                make(223, 38, 753.2);

        /** 5203-2402-0051 — 50.9:1, 117 RPM */
        public static final MotorSpec W5203_117RPM =
                make(117, 68.4, 1425.2);

        /** 5203-2402-0071 — 71.2:1, 84 RPM */
        public static final MotorSpec W5203_84RPM =
                make(84, 93.6, 1993.6);

        /** 5203-2402-0100 — 99.5:1, 60 RPM */
        public static final MotorSpec W5203_60RPM =
                make(60, 133.2, 2786);

        /** 5203-2402-0139 — 139:1, 43 RPM */
        public static final MotorSpec W5203_43RPM =
                make(43, 185, 3892);

        /** 5203-2402-0188 — 188:1, 30 RPM */
        public static final MotorSpec W5203_30RPM =
                make(30, 250, 5264);

        // -------------------------------------------------------------------------
        // 5202 Series — 6mm D-shaft, 24mm shaft
        // -------------------------------------------------------------------------

        /** 5202-2402-0003 — 3.7:1, 1620 RPM */
        public static final MotorSpec W5202_1620RPM =
                make(1620, 5.4, 103.6);

        /** 5202-2402-0005 — 5.2:1, 1150 RPM */
        public static final MotorSpec W5202_1150RPM =
                make(1150, 7.9, 145.6);

        /** 5202-2402-0014 — 13.7:1, 435 RPM */
        public static final MotorSpec W5202_435RPM =
                make(435, 18.7, 384.5);

        /** 5202-2402-0019 — 19.2:1, 312 RPM */
        public static final MotorSpec W5202_312RPM =
                make(312, 24.3, 537.6);

        /** 5202-2402-0027 — 26.9:1, 223 RPM */
        public static final MotorSpec W5202_223RPM =
                make(223, 38, 753.2);

        /** 5202-2402-0051 — 50.9:1, 117 RPM */
        public static final MotorSpec W5202_117RPM =
                make(117, 68.4, 1425.2);

        /** 5202-2402-0071 — 71.2:1, 84 RPM */
        public static final MotorSpec W5202_84RPM =
                make(84, 93.6, 1993.6);

        /** 5202-2402-0100 — 99.5:1, 60 RPM */
        public static final MotorSpec W5202_60RPM =
                make(60, 133.2, 2786);

        /** 5202-2402-0139 — 139:1, 43 RPM */
        public static final MotorSpec W5202_43RPM =
                make(43, 185, 3892);

        /** 5202-2402-0188 — 188:1, 30 RPM */
        public static final MotorSpec W5202_30RPM =
                make(30, 250, 5264);

        // -------------------------------------------------------------------------
        // 5204 Series — 8mm REX, 80mm shaft (same specs as 5203, different shaft)
        // -------------------------------------------------------------------------

        /** 5204-8002-0003 — 3.7:1, 1620 RPM */
        public static final MotorSpec W5204_1620RPM =
                make(1620, 5.4, 103.6);

        /** 5204-8002-0005 — 5.2:1, 1150 RPM */
        public static final MotorSpec W5204_1150RPM =
                make(1150, 7.9, 145.6);

        /** 5204-8002-0014 — 13.7:1, 435 RPM */
        public static final MotorSpec W5204_435RPM =
                make(435, 18.7, 384.5);

        /** 5204-8002-0019 — 19.2:1, 312 RPM */
        public static final MotorSpec W5204_312RPM =
                make(312, 24.3, 537.6);

        /** 5204-8002-0027 — 26.9:1, 223 RPM */
        public static final MotorSpec W5204_223RPM =
                make(223, 38, 753.2);

        /** 5204-8002-0051 — 50.9:1, 117 RPM */
        public static final MotorSpec W5204_117RPM =
                make(117, 68.4, 1425.2);

        /** 5204-8002-0071 — 71.2:1, 84 RPM */
        public static final MotorSpec W5204_84RPM =
                make(84, 93.6, 1993.6);

        /** 5204-8002-0100 — 99.5:1, 60 RPM */
        public static final MotorSpec W5204_60RPM =
                make(60, 133.2, 2786);

        /** 5204-8002-0139 — 139:1, 43 RPM */
        public static final MotorSpec W5204_43RPM =
                make(43, 185, 3892);

        /** 5204-8002-0188 — 188:1, 30 RPM */
        public static final MotorSpec W5204_30RPM =
                make(30, 250, 5264);
    }


    /**
     * 2000 Series Dual Mode Servos and Proton Servos.
     *
     * Specs are at 6V nominal (standard FTC servo voltage).
     * All Dual Mode servos support 300° in default mode and continuous rotation
     * when programmed with the goBILDA servo programmer.
     * 5-Turn variants support 1800° in default mode.
     * Proton servos are 180° only, not dual-mode.
     *
     * Stall current is 2500mA @ 6V across all Dual Mode variants.
     * Free current varies slightly by ratio — listed per servo.
     */
    public static final class Servo {

        private Servo() {}

        // 1 kg.cm = 0.0980665 N.m
        private static double kgCmToNm(double kgCm) {
            return kgCm * 0.0980665;
        }

        // -------------------------------------------------------------------------
        // 2000 Series Dual Mode — 300° default, programmable continuous
        // -------------------------------------------------------------------------

        /**
         * 2000-0025-0002 — Torque (300:1)
         * 50 RPM, 21.6 kg.cm stall @ 6V
         */
        public static final ServoSpec DUAL_MODE_TORQUE = new ServoSpec(
                300,           // travelDegrees
                0.5,           // centerPosition
                2.5,           // stallCurrentAmps @ 6V
                0.20,          // speedSecondsPerSixtyDegrees @ 6V
                kgCmToNm(21.6),// stallTorqueNm @ 6V
                60             // massGrams
        );

        /**
         * 2000-0025-0003 — Speed (125:1)
         * 115 RPM, 9.3 kg.cm stall @ 6V
         */
        public static final ServoSpec DUAL_MODE_SPEED = new ServoSpec(
                300,
                0.5,
                2.5,
                0.09,
                kgCmToNm(9.3),
                60
        );

        /**
         * 2000-0025-0004 — Super Speed (67:1)
         * 230 RPM, 4.7 kg.cm stall @ 6V
         */
        public static final ServoSpec DUAL_MODE_SUPER_SPEED = new ServoSpec(
                300,
                0.5,
                2.5,
                0.043,
                kgCmToNm(4.7),
                58
        );

        // -------------------------------------------------------------------------
        // 2000 Series 5-Turn Dual Mode — 1800° default, programmable continuous
        // Same electrical specs as standard Dual Mode, 6x the range
        // -------------------------------------------------------------------------

        /**
         * 2000-0025-0502 — 5-Turn Torque
         * 50 RPM, 21.6 kg.cm stall @ 6V, 1800° travel
         */
        public static final ServoSpec DUAL_MODE_5TURN_TORQUE = new ServoSpec(
                1800,
                0.5,
                2.5,
                0.20,
                kgCmToNm(21.6),
                60
        );

        /**
         * 2000-0025-0503 — 5-Turn Speed
         * 115 RPM, 9.3 kg.cm stall @ 6V, 1800° travel
         */
        public static final ServoSpec DUAL_MODE_5TURN_SPEED = new ServoSpec(
                1800,
                0.5,
                2.5,
                0.09,
                kgCmToNm(9.3),
                60
        );

        /**
         * 2000-0025-0504 — 5-Turn Super Speed
         * 230 RPM, 4.7 kg.cm stall @ 6V, 1800° travel
         */
        public static final ServoSpec DUAL_MODE_5TURN_SUPER_SPEED = new ServoSpec(
                1800,
                0.5,
                2.5,
                0.043,
                kgCmToNm(4.7),
                58
        );

        // -------------------------------------------------------------------------
        // Proton Servos — 180° only, not dual-mode, budget option
        // -------------------------------------------------------------------------

        /**
         * 2002-0180-0002 — Proton Torque
         * 180° travel, steel gears, not programmable
         */
        public static final ServoSpec PROTON_TORQUE = new ServoSpec(
                180,
                0.5,
                2.0,           // estimated; spec sheet not published
                0.18,          // estimated from power rating
                kgCmToNm(13.5),// estimated from ~0.7W output power
                42
        );

        /**
         * 2002-0180-0003 — Proton Speed
         * 180° travel, steel gears, not programmable
         */
        public static final ServoSpec PROTON_SPEED = new ServoSpec(
                180,
                0.5,
                2.0,
                0.09,
                kgCmToNm(6.0),
                42
        );
    }
}