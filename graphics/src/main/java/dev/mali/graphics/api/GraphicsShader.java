package dev.mali.graphics.api;

/** Compiled shader stage. Failures are recorded, never thrown to the app (§24). */
public abstract class GraphicsShader extends GraphicsResource {
    protected GraphicsShader(String label) { super(label); }

    public abstract GraphicsTypes.ShaderStage stage();
    /** true if compiled+validated successfully. */
    public abstract boolean isValid();
    /** Compiler/driver log (empty when valid). */
    public abstract String infoLog();
    /** Compilation duration in ms (profiler feed, §20). */
    public abstract long compileTimeMs();
}
