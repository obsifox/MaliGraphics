package dev.mali.graphics.api;

/** A pipeline = shaders + fixed state (blend). Stage 5 will add render passes/vertex layouts here. */
public abstract class GraphicsPipeline extends GraphicsResource {
    protected GraphicsPipeline(String label) { super(label); }

    public abstract GraphicsShader vertexShader();
    public abstract GraphicsShader fragmentShader();
    public abstract GraphicsTypes.BlendMode blendMode();

    /** true when linked and ready. */
    public abstract boolean isValid();
    public abstract String infoLog();
    public abstract long linkTimeMs();

    /** Cached uniform locations by name (-1 if absent). */
    public abstract int uniformLocation(String name);
}
