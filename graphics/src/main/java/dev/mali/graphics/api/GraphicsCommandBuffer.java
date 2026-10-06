package dev.mali.graphics.api;

import java.nio.ByteBuffer;

/**
 * Backend-neutral recorded command list (docs/command-submission.md).
 * Scenes record via GraphicsRenderer; the backend interprets on submit.
 * All op objects are pooled: zero allocation in the frame loop (§29).
 */
public abstract class GraphicsCommandBuffer extends GraphicsResource {
    protected GraphicsCommandBuffer(String label) { super(label); }

    public abstract void reset();

    // --- recording (render thread) ---
    public abstract void setViewport(int x, int y, int w, int h);
    public abstract void setScissor(int x, int y, int w, int h);
    public abstract void clearScissor();
    public abstract void setClearColor(float r, float g, float b, float a);
    public abstract void setBlend(GraphicsTypes.BlendMode mode);
    public abstract void bindPipeline(GraphicsPipeline pipeline);
    public abstract void bindTexture(int unit, GraphicsTexture texture);
    public abstract void setUniformMatrix(String name, float[] m4, int offset);
    public abstract void setUniformVec4(String name, float x, float y, float z, float w);
    public abstract void setUniformFloat(String name, float v);
    public abstract void bindVertexBuffers(GraphicsBuffer vertex, GraphicsBuffer index);
    public abstract void drawQuads(int firstQuad, int quadCount);

    /** Number of recorded ops (profiler metric). */
    public abstract int opCount();
}
