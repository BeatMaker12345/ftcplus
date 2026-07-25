package dev.ftcplus.core;

import dev.ftcplus.core.statemachine.StateMachine;

public abstract class Subsystem<S extends Enum<S>> extends Component {
    private StateMachine<S> stateMachine;

    protected final <T extends Component> T register(T component) {
        return registerChild(component);
    }

    protected abstract S initialState();

    protected abstract void defineStates(StateMachine<S> states);

    @Override
    protected final void onInitialize() {
        stateMachine = new StateMachine<>(signalBus());
        defineStates(stateMachine);
        stateMachine.start(initialState());
    }

    @Override
    protected final void onUpdate() {
        stateMachine.update();
    }

    public final S currentState() {
        return stateMachine.current();
    }
}