package dev.mali.graphics.api;

import dev.mali.graphics.api.GraphicsTypes.TextureFormat;

/** GPU vertex/index buffer. Data upload is backend-managed. */
public abstract class GraphicsBuffer extends GraphicsResource {
    protected GraphicsBuffer(String label) { super(label); }

    public enum Kind { VERTEX, INDEX }

    public abstract Kind kind();
    public abstract int sizeBytes();

    /** Replaces buffer contents from a CPU staging buffer. Render thread only. */
    public abstract void upload(java.nio.Buffer data, int byteCount, GraphicsTypes.BufferUsage usage);

    /** Writes a range without reallocating (orphan-rotate strategy is backend's choice). */
    public abstract void update(java.nio.Buffer data, int byteOffset, int byteCount);
}
