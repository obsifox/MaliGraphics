package dev.mali.graphics.api;

/** Sampling state. On GLES2 this is emulated per-texture (texture params). */
public abstract class GraphicsSampler extends GraphicsResource {
    protected GraphicsSampler(String label) { super(label); }

    public abstract GraphicsTypes.FilterMode minFilter();
    public abstract GraphicsTypes.FilterMode magFilter();
    public abstract GraphicsTypes.WrapMode wrapU();
    public abstract GraphicsTypes.WrapMode wrapV();
}
