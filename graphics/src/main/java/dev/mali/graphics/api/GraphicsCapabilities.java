package dev.mali.graphics.api;

import java.util.Map;

/** Runtime capability snapshot (mission §11, §22). Never hard-code: always query. */
public abstract class GraphicsCapabilities extends GraphicsResource {
    protected GraphicsCapabilities(String label) { super(label); }

    public abstract int glMajorVersion();
    public abstract int glMinorVersion();

    /** Raw GL_VENDOR / GL_RENDERER / GL_VERSION / GL_SHADING_LANGUAGE_VERSION. */
    public abstract String vendor();
    public abstract String renderer();
    public abstract String version();
    public abstract String glslVersion();

    public abstract int maxTextureSize();
    public abstract int maxTextureUnits();
    public abstract int maxVertexAttribs();
    public abstract int maxRenderBufferSize();

    /** Parsed GL_EXTENSIONS. */
    public abstract Map<String, Boolean> extensions();

    public boolean hasExtension(String name) {
        Boolean b = extensions().get(name);
        return b != null && b;
    }

    /** true when the backend is ES3-capable (300 es shaders allowed). */
    public abstract boolean isGles3();
}
