package dev.ftcplus.core.statemachine;

import dev.ftcplus.core.power.PowerBudget;
import dev.ftcplus.core.power.PowerConstraint;
import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;
import dev.ftcplus.core.signal.Subscription;

import java.util.*;

public final class StateMachine<S extends Enum<S>> {
    private final SignalBus bus;
    private final Map<S, StateDefinition<S>> definitions = new HashMap<>();
    private final List<Subscription> activeSubscriptions = new ArrayList<>();

    private S current;
    private long stateEnteredAt;
    private PowerBudget powerBudget;
    private PendingTransition<S> pendingTransition;

    public void attachPowerBudget(PowerBudget budget) {
        this.powerBudget = budget;
    }

    public StateMachine(SignalBus bus) {
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    public StateDefinition<S> state(S state) {
        Objects.requireNonNull(state, "state");
        return definitions.computeIfAbsent(state, k -> new StateDefinition<>(state, this));
    }

    public void start(S initialState) {
        Objects.requireNonNull(initialState, "initialState");
        current = initialState;
        stateEnteredAt = System.currentTimeMillis();
        enter(current);
    }

    public void update() {
        if (current == null) return;

        if (pendingTransition != null) {
            if (pendingTransition.constraint.check(powerBudget)) {
                transitionTo(pendingTransition.target);
                pendingTransition = null;
            } else if (!pendingTransition.constraint.retryUntilPassed()) {
                pendingTransition = null;
            }
        }

        StateDefinition<S> def = definitions.get(current);
        if (def == null) return;

        if (def.onUpdate != null) def.onUpdate.run();

        if (def.transitionAfterMs >= 0) {
            long elapsed = System.currentTimeMillis() - stateEnteredAt;
            if (elapsed >= def.transitionAfterMs) {
                transitionTo(def.transitionAfterTarget);
            }
        }
    }

    public S current() {
        return current;
    }

    void transitionTo(S next) {
        Objects.requireNonNull(next, "next");
        if (next == current) return;

        StateDefinition<S> currentDef = definitions.get(current);
        if (currentDef != null && currentDef.onExit != null) {
            currentDef.onExit.run();
        }

        StateDefinition<S> nextDef = definitions.get(next);

        current = next;
        stateEnteredAt = System.currentTimeMillis();

        activeSubscriptions.forEach(Subscription::cancel);
        activeSubscriptions.clear();
        enter(next);
    }

    private void enter(S state) {
        StateDefinition<S> def = definitions.get(state);
        if (def == null) return;

        if (def.onEnter != null) {
            def.onEnter.run();
        }

        for (Map.Entry<Class<? extends Event>, S> entry : def.eventTransitions.entrySet()) {
            S target = entry.getValue();
            StateDefinition<S> targetDef = definitions.get(target);
            PowerConstraint constraint = targetDef != null ? targetDef.constraint() : null;

            Subscription sub = bus.subscribe(entry.getKey(), e -> {
                if (current != state) return;

                if (constraint == null || powerBudget == null) {
                    transitionTo(target);
                    return;
                }

                if (constraint.check(powerBudget)) {
                    transitionTo(target);
                } else {
                    pendingTransition = new PendingTransition<>(target, constraint, e);
                    Event blocked = constraint.onBlocked();
                    if (blocked != null) bus.send(blocked);
                }
            });
            activeSubscriptions.add(sub);
        }
    }

    private static final class PendingTransition<S> {
        final S target;
        final PowerConstraint constraint;
        final dev.ftcplus.core.signal.Event triggeringEvent;

        PendingTransition(S target, PowerConstraint constraint, dev.ftcplus.core.signal.Event triggeringEvent) {
            this.target          = target;
            this.constraint      = constraint;
            this.triggeringEvent = triggeringEvent;
        }
    }
}