package dev.ftcplus.tests;

import dev.ftcplus.core.*;
import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.statemachine.StateMachine;
import dev.ftcplus.core.motor.Motor;
import dev.ftcplus.runtime.OpMode;
import dev.ftcplus.sim.runner.SimOpModeRunner;
import dev.ftcplus.core.GamepadButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntakeOpModeTest {


    static final class IntakeRequested extends Event {}
    static final class ForceStop extends Event {}
    static final class GamePieceDetected extends Event {}


    static class IntakeMotor extends Motor {
        IntakeMotor() { super(Hardware.INTAKE); }
        void intake() { setPower(1.0); }
        void stop()   { setPower(0.0); }
    }


    static class Intake extends Subsystem<Intake.State> {
        enum State { IDLE, INTAKING }

        final IntakeMotor motor = register(new IntakeMotor());

        @Override protected State initialState() { return State.IDLE; }

        @Override
        protected void defineStates(StateMachine<State> states) {
            states.state(State.IDLE)
                    .onEnter(motor::stop)
                    .transitionOn(IntakeRequested.class, State.INTAKING);

            states.state(State.INTAKING)
                    .onEnter(motor::intake)
                    .transitionOn(GamePieceDetected.class, State.IDLE)
                    .transitionOn(ForceStop.class, State.IDLE);
        }
    }


    enum Hardware implements HardwareEntry {
        INTAKE("intake");

        private final String name;
        Hardware(String name) { this.name = name; }

        @Override public String hardwareName() { return name; }
    }

    static class TestGlobals {}
    static class TestProperties extends RobotProperties {}

    @TeamRobot(name = "TestRobot")
    static class TestRobot extends Robot<Hardware, TestGlobals, TestProperties> {
        final Intake intake = register(new Intake());
        TestRobot() { super(Hardware.class, new TestGlobals(), new TestProperties()); }
    }


    @OpMode.Register("Test TeleOp")
    static class TestTeleOp extends OpMode {
        @Override
        protected void configure() {
            controls()
                    .pressed(GamepadButton.G1_A)
                    .send(IntakeRequested::new);

            controls()
                    .pressed(GamepadButton.G1_B)
                    .send(ForceStop::new);
        }
    }


    private SimOpModeRunner runner;
    private TestRobot robot;

    @BeforeEach
    void setUp() {
        runner = new SimOpModeRunner(new TestTeleOp(), new TestRobot());
        runner.init();
        runner.start();
        robot = (TestRobot) runner.robot();
    }

    @Test
    void startsIdle() {
        assertEquals(Intake.State.IDLE, robot.intake.currentState());
    }

    @Test
    void pressAStartsIntaking() {
        runner.gamepad().pressButton(GamepadButton.G1_A);
        runner.loop(2);
        assertEquals(Intake.State.INTAKING, robot.intake.currentState());
    }

    @Test
    void pressBStopsIntaking() {
        runner.gamepad().pressButton(GamepadButton.G1_A);
        runner.loop(2);

        runner.gamepad().releaseAll();
        runner.gamepad().pressButton(GamepadButton.G1_B);
        runner.loop(2);

        assertEquals(Intake.State.IDLE, robot.intake.currentState());
    }

    @Test
    void gamePieceDetectedStopsIntaking() {
        runner.gamepad().pressButton(GamepadButton.G1_A);
        runner.loop(2);
        assertEquals(Intake.State.INTAKING, robot.intake.currentState());

        runner.runtime().signalBus().send(new GamePieceDetected());
        runner.loop(2);
        assertEquals(Intake.State.IDLE, robot.intake.currentState());
    }

    @Test
    void motorRunsWhileIntaking() {
        runner.gamepad().pressButton(GamepadButton.G1_A);
        runner.loop(2);

        double power = runner.deviceFactory().motors().get(0).getPower();
        assertEquals(1.0, power, 0.01);
    }

    @Test
    void motorStopsWhenIdle() {
        runner.loop(5);
        double power = runner.deviceFactory().motors().get(0).getPower();
        assertEquals(0.0, power, 0.01);
    }
}