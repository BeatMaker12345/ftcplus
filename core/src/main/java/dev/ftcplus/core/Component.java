package dev.ftcplus.core;

import dev.ftcplus.core.signal.Signal;
import dev.ftcplus.core.signal.SignalBus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class Component {
    private final List<Component> children = new ArrayList<>();
    private SignalBus signalBus;
    private DeviceFactory deviceFactory;
    private GamepadFeedback gamepadFeedback;

    Component parent;
    private String name;
    private String path;

    private LifecycleState state = LifecycleState.CREATED;

    public final LifecycleState state() {
        return state;
    }

    public final Component parent() {
        return parent;
    }

    public final String name() {
        requireResolvedIdentity();
        return name;
    }

    public final String path() {
        requireResolvedIdentity();
        return path;
    }

    protected final <T extends Component> T registerChild(T child) {
        Objects.requireNonNull(child, "child");

        if (state != LifecycleState.CREATED) {
            throw new IllegalStateException(
                    "Components can only be registered before initialization"
            );
        }

        if (child == this) {
            throw new IllegalArgumentException(
                    "A component cannot own itself"
            );
        }

        if (child.parent() != null) {
            throw new IllegalArgumentException(
                    "Component already belongs to another parent"
            );
        }

        child.parent = this;
        children.add(child);

        return child;
    }

    final void resolveIdentityInternal() {
        resolveIdentityInternal("");
    }

    private void resolveIdentityInternal(String parentPath) {
        Set<String> usedNames = new HashSet<>();

        for (Component child : children) {
            String childName = findFieldName(child);

            if (!usedNames.add(childName)) {
                throw new IllegalStateException(
                        "Multiple child components use the field name '" +
                                childName + "' in " + getClass().getName()
                );
            }

            child.name = childName;
            child.path = parentPath.isEmpty()
                    ? childName
                    : parentPath + "." + childName;

            child.resolveIdentityInternal(child.path);
        }
    }

    private String findFieldName(Component child) {
        String matchingName = null;

        for (
                Class<?> type = getClass();
                type != null && Component.class.isAssignableFrom(type);
                type = type.getSuperclass()
        ) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                if (!Component.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                if (!field.trySetAccessible()) {
                    throw new IllegalStateException(
                            "Cannot inspect component field " +
                                    field.getDeclaringClass().getName() +
                                    "." + field.getName()
                    );
                }

                Object value;

                try {
                    value = field.get(this);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException(
                            "Cannot inspect component field " +
                                    field.getDeclaringClass().getName() +
                                    "." + field.getName(),
                            exception
                    );
                }

                if (value != child) {
                    continue;
                }

                if (matchingName != null) {
                    throw new IllegalStateException(
                            "Component is referenced by multiple fields: '" +
                                    matchingName + "' and '" + field.getName() + "'"
                    );
                }

                matchingName = field.getName();
            }
        }

        if (matchingName == null) {
            throw new IllegalStateException(
                    "Registered component " + child.getClass().getName() +
                            " must be stored in a field of " +
                            getClass().getName()
            );
        }

        return matchingName;
    }

    final void initializeInternal() {
        requireState(LifecycleState.CREATED);

        for (Component child : children) {
            child.initializeInternal();
        }

        onInitialize();
        state = LifecycleState.INITIALIZED;
    }

    final void startInternal() {
        requireState(LifecycleState.INITIALIZED);

        for (Component child : children) {
            child.startInternal();
        }

        onStart();
        state = LifecycleState.STARTED;
    }

    final void updateInternal() {
        requireState(LifecycleState.STARTED);

        onUpdate();

        for (Component child : children) {
            child.updateInternal();
        }
    }

    final void stopInternal() {
        requireState(LifecycleState.STARTED);

        onStop();

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).stopInternal();
        }

        state = LifecycleState.STOPPED;
    }

    protected void onInitialize() {}

    protected void onStart() {}

    protected void onUpdate() {}

    protected void onStop() {}

    private void requireResolvedIdentity() {
        if (name == null || path == null) {
            throw new IllegalStateException(
                    "Component identity has not been resolved"
            );
        }
    }

    private void requireState(LifecycleState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Expected " + expected + ", but component was " + state
            );
        }
    }

    final void attachBus(SignalBus bus) {
        this.signalBus = bus;
        for (Component child : children) {
            child.attachBus(bus);
        }
    }

    final void attachDeviceFactory(DeviceFactory factory) {
        this.deviceFactory = factory;
        for (Component child : children) {
            child.attachDeviceFactory(factory);
        }
    }

    protected DeviceFactory deviceFactory() {
        return deviceFactory;
    }

    protected final void send(Signal signal) {
        if (signalBus == null) {
            throw new IllegalStateException("SignalBus not attached");
        }
        signalBus.send(signal);
    }

    public final void attachGamepadFeedback(GamepadFeedback feedback) {
        this.gamepadFeedback = feedback;
        for (Component child : children) {
            child.attachGamepadFeedback(feedback);
        }
    }

    protected final void vibrate(GamepadSide side, int milliseconds) {
        if (gamepadFeedback != null) gamepadFeedback.vibrate(side, milliseconds);
    }

    protected final void setLed(GamepadSide side, double r, double g, double b, int durationMs) {
        if (gamepadFeedback != null) gamepadFeedback.setLed(side, r, g, b, durationMs);
    }

    SignalBus signalBus() { return signalBus; }

    public List<Component> children() {
        return children;
    }
}