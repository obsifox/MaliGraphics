package dev.mali.graphics.api;

/**
 * Submission queue. On GLES this is the implicit render-thread stream;
 * the interface exists so the Vulkan backend (Stage 5) maps 1:1 onto VkQueue.
 */
public abstract class GraphicsQueue extends GraphicsResource {
    protected GraphicsQueue(String label) { super(label); }

    /** Executes a recorded command list now (render thread). */
    public abstract void submit(GraphicsCommandBuffer commands);

    /** Blocks until GPU idle (benchmarks/tests only — never in frame loop). */
    public abstract void waitIdle();

    /** Creates a fence signaled after currently queued work completes. */
    public abstract GraphicsFence createFence(String label);
}
