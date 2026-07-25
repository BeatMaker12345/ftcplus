package dev.ftcplus.core.statemachine;

import dev.ftcplus.core.signal.Event;

import java.util.HashMap;
import java.util.Map;

public final class StateDefinition<S extends Enum<S>> {
    final S state;
    final StateMachine<S> machine;

    Runnable onEnter;
    Runnable onUpdate;
    Runnable onExit;

    long transitionAfterMs = -1;
    S transitionAfterTarget;

    final Map<Class<? extends Event>, S> eventTransitions = new HashMap<>();

    StateDefinition(S state, StateMachine<S> machine) {
        this.state = state;
        this.machine = machine;
    }

    public StateDefinition<S> onEnter(Runnable action) {
        this.onEnter = action;
        return this;
    }

    public StateDefinition<S> onUpdate(Runnable action) {
        this.onUpdate = action;
        return this;
    }

    public StateDefinition<S> onExit(Runnable action) {
        this.onExit = action;
        return this;
    }

    public StateDefinition<S> transitionOn(Class<? extends Event> event, S next) {
        eventTransitions.put(event, next);
        return this;
    }

    public StateDefinition<S> transitionAfter(long ms, S next) {
        this.transitionAfterMs = ms;
        this.transitionAfterTarget = next;
        return this;
    }
}
