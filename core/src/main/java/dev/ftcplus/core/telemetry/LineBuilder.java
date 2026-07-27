package dev.ftcplus.core.telemetry;

public interface LineBuilder {
    LineBuilder color(String hex);
    LineBuilder bold();
    LineBuilder italic();
    LineBuilder size(float scale);
}