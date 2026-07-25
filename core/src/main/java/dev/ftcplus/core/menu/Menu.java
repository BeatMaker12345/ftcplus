package dev.ftcplus.core.menu;

import java.util.ArrayList;
import java.util.List;

public abstract class Menu {
    private final String title;
    private final List<MenuItem> items = new ArrayList<>();
    private int pointer = 0;

    protected Menu(String title) {
        this.title = title;
    }

    public String title() { return title; }

    public void addItem(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> items() {
        return items;
    }

    public int pointer() {
        return pointer;
    }

    public void onSelected() {
        pointer = 0;
    }

    public void update(MenuInputSource input, MenuHost host) {
        if (items.isEmpty()) {
            render(title, items, pointer);
            return;
        }

        if (input.up())      movePointer(-1);
        if (input.down())    movePointer(+1);
        if (input.left())    items.get(pointer).left();
        if (input.right())   items.get(pointer).right();
        if (input.confirm()) {
            if (items.get(pointer).selectable()) {
                items.get(pointer).confirm();
            }
        }
        if (input.back())    host.back();

        render(title, items, pointer);
    }

    protected abstract void render(String title, List<MenuItem> items, int pointer);

    private void movePointer(int delta) {
        if (items.isEmpty()) return;
        int attempts = 0;
        do {
            pointer = (pointer + delta + items.size()) % items.size();
            attempts++;
        } while (attempts <= items.size() && !items.get(pointer).selectable());
    }
}