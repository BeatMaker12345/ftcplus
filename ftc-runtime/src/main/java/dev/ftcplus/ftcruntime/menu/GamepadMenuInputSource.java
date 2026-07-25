package dev.ftcplus.ftcruntime.menu;

import com.qualcomm.robotcore.hardware.Gamepad;
import dev.ftcplus.core.menu.MenuInputSource;

public class GamepadMenuInputSource implements MenuInputSource {
    private final Gamepad gamepad1;
    private final Gamepad gamepad2;

    private final RepeatingInput up    = new RepeatingInput();
    private final RepeatingInput down  = new RepeatingInput();
    private final RepeatingInput left  = new RepeatingInput();
    private final RepeatingInput right = new RepeatingInput();

    private boolean prevConfirm = false;
    private boolean prevBack    = false;

    private boolean upFired, downFired, leftFired, rightFired;
    private boolean confirmFired, backFired;

    public GamepadMenuInputSource(Gamepad gamepad1, Gamepad gamepad2) {
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
    }

    public GamepadMenuInputSource(Gamepad gamepad1) {
        this(gamepad1, null);
    }

    public void update() {
        boolean upRaw    = g1g2(gamepad1.dpad_up,    gamepad2 != null && gamepad2.dpad_up);
        boolean downRaw  = g1g2(gamepad1.dpad_down,  gamepad2 != null && gamepad2.dpad_down);
        boolean leftRaw  = g1g2(gamepad1.dpad_left,  gamepad2 != null && gamepad2.dpad_left);
        boolean rightRaw = g1g2(gamepad1.dpad_right, gamepad2 != null && gamepad2.dpad_right);
        boolean confirmRaw = g1g2(gamepad1.a, gamepad2 != null && gamepad2.a);
        boolean backRaw    = g1g2(gamepad1.b, gamepad2 != null && gamepad2.b);

        upFired    = up.update(upRaw);
        downFired  = down.update(downRaw);
        leftFired  = left.update(leftRaw);
        rightFired = right.update(rightRaw);

        confirmFired = confirmRaw && !prevConfirm;
        backFired    = backRaw && !prevBack;

        prevConfirm = confirmRaw;
        prevBack    = backRaw;
    }

    @Override public boolean up()      { return upFired; }
    @Override public boolean down()    { return downFired; }
    @Override public boolean left()    { return leftFired; }
    @Override public boolean right()   { return rightFired; }
    @Override public boolean confirm() { return confirmFired; }
    @Override public boolean back()    { return backFired; }

    private static boolean g1g2(boolean g1, boolean g2) {
        return g1 || g2;
    }
}