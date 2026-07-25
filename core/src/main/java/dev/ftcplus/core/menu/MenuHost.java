package dev.ftcplus.core.menu;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MenuHost {
    private final Deque<Menu> stack = new ArrayDeque<>();
    private Menu root;
    private Menu current;
    private boolean active = true;

    public void setRoot(Menu menu) {
        this.root = menu;
        this.current = menu;
        stack.clear();
        menu.onSelected();
    }

    public void push(Menu menu) {
        if (current != null) stack.push(current);
        current = menu;
        current.onSelected();
    }

    public void back() {
        if (current == root) {
            active = false;
            return;
        }
        if (!stack.isEmpty()) {
            current = stack.pop();
            current.onSelected();
        } else {
            goToRoot();
        }
    }

    public void goToRoot() {
        if (root == null) return;
        stack.clear();
        current = root;
        current.onSelected();
    }

    public void update(MenuInputSource input) {
        if (current != null) current.update(input, this);
    }

    public boolean isActive() { return active; }
    public Menu current()     { return current; }
}