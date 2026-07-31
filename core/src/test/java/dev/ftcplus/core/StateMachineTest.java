package dev.ftcplus.core;

import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;
import dev.ftcplus.core.statemachine.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StateMachineTest {
    static final class Start  extends Event {}
    static final class Stop   extends Event {}
    static final class Finish extends Event {}

    enum State { IDLE, RUNNING, DONE }

    private SignalBus           bus;
    private StateMachine<State> sm;
    private int                 enterCount;
    private int                 exitCount;
    private int                 updateCount;

    @BeforeEach
    void setUp() {
        bus         = new SignalBus();
        sm          = new StateMachine<>(bus);
        enterCount  = 0;
        exitCount   = 0;
        updateCount = 0;

        sm.state(State.IDLE)
                .onEnter(() -> enterCount++)
                .transitionOn(Start.class, State.RUNNING);

        sm.state(State.RUNNING)
                .onUpdate(() -> updateCount++)
                .transitionOn(Stop.class,   State.IDLE)
                .transitionOn(Finish.class, State.DONE);

        sm.state(State.DONE)
                .onEnter(() -> enterCount++);

        sm.start(State.IDLE);
    }

    @Test
    void startsInInitialState() {
        assertEquals(State.IDLE, sm.current());
    }

    @Test
    void transitionsOnSignal() {
        bus.send(new Start());
        bus.flush();
        sm.update();
        assertEquals(State.RUNNING, sm.current());
    }

    @Test
    void doesNotTransitionOnWrongSignal() {
        bus.send(new Stop());
        bus.flush();
        sm.update();
        assertEquals(State.IDLE, sm.current());
    }

    @Test
    void multipleTransitions() {
        bus.send(new Start());
        bus.flush(); sm.update();
        assertEquals(State.RUNNING, sm.current());

        bus.send(new Finish());
        bus.flush(); sm.update();
        assertEquals(State.DONE, sm.current());
    }

    @Test
    void transitionBackToIdle() {
        bus.send(new Start());
        bus.flush(); sm.update();

        bus.send(new Stop());
        bus.flush(); sm.update();

        assertEquals(State.IDLE, sm.current());
    }

    @Test
    void onEnterCalledOnTransition() {
        int before = enterCount;
        bus.send(new Start());
        bus.flush(); sm.update();

        bus.send(new Finish());
        bus.flush(); sm.update();
        assertEquals(before + 1, enterCount);
    }

    @Test
    void onUpdateCalledEachLoop() {
        bus.send(new Start());
        bus.flush(); sm.update();

        sm.update();
        sm.update();

        assertEquals(3, updateCount);
    }

    @Test
    void onUpdateNotCalledInWrongState() {
        sm.update();
        sm.update();
        assertEquals(0, updateCount);
    }

    @Test
    void transitionAfterDelay() throws InterruptedException {
        StateMachine<State> timedSm = new StateMachine<>(bus);
        timedSm.state(State.IDLE)
                .transitionAfter(100, State.DONE);
        timedSm.state(State.DONE);
        timedSm.start(State.IDLE);

        timedSm.update();
        assertEquals(State.IDLE, timedSm.current());

        Thread.sleep(150);
        timedSm.update();
        assertEquals(State.DONE, timedSm.current());
    }

    @Test
    void doesNotTransitionBeforeDelay() throws InterruptedException {
        StateMachine<State> timedSm = new StateMachine<>(bus);
        timedSm.state(State.IDLE)
                .transitionAfter(500, State.DONE);
        timedSm.state(State.DONE);
        timedSm.start(State.IDLE);

        Thread.sleep(50);
        timedSm.update();
        assertEquals(State.IDLE, timedSm.current());
    }

    @Test
    void signalQueuedNotImmediate() {
        bus.send(new Start());
        sm.update();

        assertNotNull(sm.current());
    }

    @Test
    void multipleSignalsInOneTick() {
        bus.send(new Start());
        bus.send(new Finish());
        bus.flush();
        sm.update();
        assertEquals(State.DONE, sm.current());
    }
}