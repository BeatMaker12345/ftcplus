package dev.ftcplus.core;

import dev.ftcplus.core.signal.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SignalBusTest {

    static final class Ping  extends Event {}
    static final class Pong  extends Event {}

    static final class DataMessage extends Message {
        final int value;
        DataMessage(int value) { this.value = value; }
    }

    private SignalBus bus;

    @BeforeEach
    void setUp() {
        bus = new SignalBus();
    }


    @Test
    void subscriberReceivesEvent() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Ping());
        bus.flush();
        assertEquals(1, received.size());
    }

    @Test
    void subscriberDoesNotReceiveWrongType() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Pong());
        bus.flush();
        assertTrue(received.isEmpty());
    }

    @Test
    void multipleSubscribersReceiveEvent() {
        List<Ping> a = new ArrayList<>(), b = new ArrayList<>();
        bus.subscribe(Ping.class, a::add);
        bus.subscribe(Ping.class, b::add);
        bus.send(new Ping());
        bus.flush();
        assertEquals(1, a.size());
        assertEquals(1, b.size());
    }

    @Test
    void multipleEventsQueued() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Ping());
        bus.send(new Ping());
        bus.send(new Ping());
        bus.flush();
        assertEquals(3, received.size());
    }


    @Test
    void eventsQueuedUntilFlush() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Ping());
        assertTrue(received.isEmpty(), "should not deliver before flush");
        bus.flush();
        assertEquals(1, received.size());
    }


    @Test
    void messagePayloadDelivered() {
        List<DataMessage> received = new ArrayList<>();
        bus.subscribe(DataMessage.class, received::add);
        bus.send(new DataMessage(42));
        bus.flush();
        assertEquals(1, received.size());
        assertEquals(42, received.get(0).value);
    }


    @Test
    void cancelledSubscriptionDoesNotReceive() {
        List<Ping> received = new ArrayList<>();
        Subscription sub = bus.subscribe(Ping.class, received::add);
        sub.cancel();
        bus.send(new Ping());
        bus.flush();
        assertTrue(received.isEmpty());
    }

    @Test
    void otherSubscriptionsUnaffectedByCancel() {
        List<Ping> a = new ArrayList<>(), b = new ArrayList<>();
        Subscription sub = bus.subscribe(Ping.class, a::add);
        bus.subscribe(Ping.class, b::add);
        sub.cancel();
        bus.send(new Ping());
        bus.flush();
        assertTrue(a.isEmpty());
        assertEquals(1, b.size());
    }


    @Test
    void flushClearsQueue() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Ping());
        bus.flush();
        bus.flush();
        assertEquals(1, received.size());
    }

    @Test
    void eventsAfterFlushDeliveredNextFlush() {
        List<Ping> received = new ArrayList<>();
        bus.subscribe(Ping.class, received::add);
        bus.send(new Ping());
        bus.flush();
        bus.send(new Ping());
        bus.flush();
        assertEquals(2, received.size());
    }

    @Immediate
    static final class UrgentPing extends Event {}

    @Test
    void immediateAnnotationDeliversWithoutFlush() {
        List<UrgentPing> received = new ArrayList<>();
        bus.subscribe(UrgentPing.class, received::add);
        bus.send(new UrgentPing());
        assertEquals(1, received.size(), "@Immediate should deliver before flush");
    }
}