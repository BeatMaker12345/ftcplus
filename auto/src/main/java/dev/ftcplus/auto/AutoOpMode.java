package dev.ftcplus.auto;

import dev.ftcplus.core.Runtime;
import dev.ftcplus.runtime.AdvancedOpMode;

import java.util.List;

public abstract class AutoOpMode extends AdvancedOpMode {

    private AutoPath selectedPath;

    protected abstract List<AutoPath> availablePaths();
    protected abstract AutoPath pickPath(List<AutoPath> paths);

    @Override
    protected void configure() {
        // no controls for auto
    }

    @Override
    public void onInit() {
        List<AutoPath> paths = availablePaths();
        selectedPath = paths.size() == 1 ? paths.get(0) : pickPath(paths);
    }

    @Override
    public void onRun() {
        if (selectedPath != null) {
            runPath(selectedPath, runtime());
        }
    }

    protected abstract void runPath(AutoPath path, Runtime runtime);
}
