package dev.mali.graphics.gles;

import dev.mali.graphics.api.GraphicsCapabilities;
import dev.mali.graphics.api.GraphicsCommandBuffer;
import dev.mali.graphics.api.GraphicsContext;
import dev.mali.graphics.api.GraphicsQueue;
import dev.mali.graphics.api.GraphicsFence;
import dev.mali.graphics.api.GraphicsTypes;

/** GLES context: queue + command executor facade around one EGL context. */
public final class GlesContext extends GraphicsContext {
    private final GlesDevice device;
    private final GlesSurface surface;
    private final GlesQueue queue;
    private GlesCapabilities caps;
    private boolean lost;

    GlesContext(GlesDevice device, GlesSurface surface) {
        super("gles-context");
        this.device = device;
        this.surface = surface;
        this.queue = new GlesQueue(device, "gles-queue");
        surface.makeCurrent();
        this.caps = device.snapshotCapabilities();
    }

    public GlesSurface surface() { return surface; }
    public GlesDevice device() { return device; }

    @Override public GraphicsQueue queue() { return queue; }

    @Override public GraphicsCommandBuffer createCommandBuffer(String label) {
        return new GlesCommandBuffer(this, label);
    }

    @Override public GraphicsCapabilities capabilities() { return caps; }

    @Override public boolean wasLost() {
        return lost || device.isContextLost() || device.egl.isContextLost();
    }

    void markLost() { lost = true; }

    @Override protected void onDestroy() {
        queue.destroy();
    }
}
