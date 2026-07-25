package dev.ftcplus.ftcruntime.menu;

final class RepeatingInput {
    private static final long INITIAL_DELAY_NS = 800_000_000L;
    private static final long REPEAT_INTERVAL_NS = 200_000_000L;

    private boolean held = false;
    private long pressStartTime;
    private long lastRepeatTime;

    boolean update(boolean active) {
        long now = System.nanoTime();

        if (!active) {
            held = false;
            return false;
        }

        if (!held) {
            held = true;
            pressStartTime = now;
            lastRepeatTime = now;
            return true;
        }

        if (now - pressStartTime >= INITIAL_DELAY_NS &&
        now - lastRepeatTime >= REPEAT_INTERVAL_NS) {
            lastRepeatTime = now;
            return true;
        }

        return false;
    }
}