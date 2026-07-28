package dev.ftcplus.core.power;

import dev.ftcplus.core.Component;
import dev.ftcplus.core.HardwareDevice;

public class PowerBudget {
    private final Component root;
    private double totalCurrentAmps = 0;
    private double maxCurrentAmps   = 20.0;
    private double voltage          = 13.5;

    public PowerBudget(Component root, double maxCurrentAmps, double voltage) {
        this.root = root;
        this.maxCurrentAmps = maxCurrentAmps;
        this.voltage = voltage;
    }

    public void update() {
        totalCurrentAmps = computeDraw(root);
    }

    private double computeDraw(Component component) {
        double total = 0;

        if (component instanceof HardwareDevice) {
            total += ((HardwareDevice) component).estimatedCurrentDraw();
        }

        for (Component child : component.children()) {
            total += computeDraw(child);
        }

        return total;
    }

    public double totalCurrentAmps()     { return totalCurrentAmps; }
    public double maxCurrentAmps()       { return maxCurrentAmps; }
    public double voltage()              { return voltage; }
    public double remainingAmps()        { return maxCurrentAmps - totalCurrentAmps; }
    public double utilizationPercent()   { return (totalCurrentAmps / maxCurrentAmps) * 100.0; }
    public boolean isOverBudget()        { return totalCurrentAmps > maxCurrentAmps; }

    public boolean canAfford(double additionalAmps) {
        return totalCurrentAmps + additionalAmps <= maxCurrentAmps;
    }
}