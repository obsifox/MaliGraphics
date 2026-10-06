package dev.mali.lab.scenes;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.gles.GlesRendererImpl;

/**
 * Benchmark scene contract (mission §21). Each test is repeatable and
 * reported through the shared profiler. Implementations stay small by
 * parameterizing QuadScene.
 */
public interface Scene {
    String name();
    void init(GlesRendererImpl renderer, int width, int height);
    void resize(int width, int height);
    void draw(GlesRendererImpl renderer, GraphicsFrame frame, long timeMs);
    /** One-line live stats (drawn by the Lab overlay). */
    String statsLine();
}
