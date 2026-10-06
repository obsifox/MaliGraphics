package dev.mali.graphics.api;

/**
 * Backend-neutral context = owning bundle of queue + command buffer + capabilities.
 * Mirrors "device context" in the mission architecture (§8).
 */
public abstract class GraphicsContext extends GraphicsResource {
    protected GraphicsContext(String label) { super(label); }

    public abstract GraphicsQueue queue();
    public abstract GraphicsCommandBuffer createCommandBuffer(String label);
    public abstract GraphicsCapabilities capabilities();

    /** true after a driver/context loss was detected (§ android-graphics.md lifecycle). */
    public abstract boolean wasLost();
}
