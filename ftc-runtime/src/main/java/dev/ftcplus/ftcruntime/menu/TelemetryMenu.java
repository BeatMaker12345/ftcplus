package dev.ftcplus.ftcruntime.menu;

import dev.ftcplus.core.menu.Menu;
import dev.ftcplus.core.menu.MenuItem;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.List;

public class TelemetryMenu extends Menu {
    private final Telemetry telemetry;

    public TelemetryMenu(String title, Telemetry telemetry) {
        super(title);
        this.telemetry = telemetry;
    }

    @Override
    protected void render(String title, List<MenuItem> items, int pointer) {
        telemetry.clearAll();
        telemetry.addLine("====== " + title + " ======");
        telemetry.addLine();

        if (items.isEmpty()) {
            telemetry.addLine("(no items)");
        } else {
            for (int i = 0; i < items.size(); i++) {
                String prefix = (i == pointer) ? "> " : "  ";
                telemetry.addLine(prefix + items.get(i).label());
            }
        }

        telemetry.update();
    }
}