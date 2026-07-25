package dev.ftcplus.core.menu;

import java.util.function.Supplier;

public final class MenuItem {
    private final Supplier<String> label;
    private final boolean selectable;
    private final Runnable onConfirm;
    private final Runnable onLeft;
    private final Runnable onRight;

    private MenuItem(Builder builder) {
        this.label       = builder.label;
        this.selectable  = builder.selectable;
        this.onConfirm   = builder.onConfirm;
        this.onLeft      = builder.onLeft;
        this.onRight     = builder.onRight;
    }

    public String label()       { return label.get(); }
    public boolean selectable() { return selectable; }
    public void confirm()       { if (onConfirm != null) onConfirm.run(); }
    public void left()          { if (onLeft != null) onLeft.run(); }
    public void right()         { if (onRight != null) onRight.run(); }

    public static Builder builder(Supplier<String> label) {
        return new Builder(label);
    }

    public static Builder builder(String label) {
        return new Builder(() -> label);
    }

    public static final class Builder {
        private final Supplier<String> label;
        private boolean selectable = true;
        private Runnable onConfirm;
        private Runnable onLeft;
        private Runnable onRight;

        private Builder(Supplier<String> label) {
            this.label = label;
        }

        private Builder selectable(boolean selectable) {
            this.selectable = selectable;
            return this;
        }

        public Builder onConfirm(Runnable r) {
            this.onConfirm = r;
            return this;
        }

        public Builder onLeft(Runnable r) {
            this.onLeft = r;
            return this;
        }

        public Builder onRight(Runnable r) {
            this.onRight = r;
            return this;
        }

        public MenuItem build() {
            return new MenuItem(this);
        }
    }

    public static MenuItem info(Supplier<String> label) {
        return builder(label).selectable(false).build();
    }

    public static MenuItem info(String label) {
        return builder(label).selectable(false).build();
    }

    public static MenuItem action(String label, Runnable onConfirm) {
        return builder(label).onConfirm(onConfirm).build();
    }

    public static MenuItem action(Supplier<String> label, Runnable onConfirm) {
        return builder(label).onConfirm(onConfirm).build();
    }

    public static MenuItem toggle(Supplier<String> label, Runnable onConfirm) {
        return builder(label).onConfirm(onConfirm).build();
    }

    public static <E extends Enum<E>> MenuItem enumCycle(
            String prefix,
            Class<E> enumClass,
            Supplier<E> getter,
            java.util.function.Consumer<E> setter
    ) {
        E[] values = enumClass.getEnumConstants();

        return builder(() -> prefix + getter.get())
                .onLeft(() -> {
                    E cur = getter.get();
                    int i = indexOf(values, cur);
                    setter.accept(values[(i - 1 + values.length) % values.length]);
                })
                .onRight(() -> {
                    E cur = getter.get();
                    int i = indexOf(values, cur);
                    setter.accept(values[(i + 1) % values.length]);
                })
                .build();
    }

    public static MenuItem numeric(
            Supplier<String> label,
            Runnable decrement,
            Runnable increment
    ) {
        return builder(label).onLeft(decrement).onRight(increment).build();
    }

    private static <T> int indexOf(T[] arr,T item) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(item)) return i;
        }
        return 0;
    }
}