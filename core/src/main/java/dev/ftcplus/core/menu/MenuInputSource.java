package dev.ftcplus.core.menu;

public interface MenuInputSource {
    boolean up();
    boolean down();
    boolean left();
    boolean right();
    boolean confirm();
    boolean back();
}