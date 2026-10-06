package dev.mali.graphics.gles;

import android.view.Surface;

import dev.mali.graphics.api.GraphicsBuffer;
import dev.mali.graphics.api.GraphicsCapabilities;
import dev.mali.graphics.api.GraphicsContext;
import dev.mali.graphics.api.GraphicsDevice;
import dev.mali.graphics.api.GraphicsPipeline;
import dev.mali.graphics.api.GraphicsResource;
import dev.mali.graphics.api.GraphicsSampler;
import dev.mali.graphics.api.GraphicsShader;
import dev.mali.graphics.api.GraphicsSurface;
import dev.mali.graphics.api.GraphicsTexture;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.profiler.ProfilerImpl;

/**
 * GLES backend device (mission §10-11). One logical device per backend;
 * EGL lives in EglCore, contexts are lightweight wrappers around it.
 */
public final class GlesDevice extends GraphicsDevice {

    EglCore egl = new EglCore();
    boolean eglReady = false;
    private ProfilerImpl profiler = new ProfilerImpl(null);
    private boolean contextLost = false;

    public GlesDevice() { super("gles-device"); }

    /** Initializes EGL (render thread). Idempotent. */
    void ensureEgl() {
        if (!eglReady) {
            egl.init();
            eglReady = true;
        }
    }

    ProfilerImpl profilerSafe() { return profiler; }
    void setProfiler(ProfilerImpl p) { this.profiler = p == null ? new ProfilerImpl(null) : p; }

    /** true when the EGL context was created against ES3 (drives GLSL version + tex storage). */
    public boolean isGles3Context() {
        return eglReady && egl.glMajor >= 3;
    }

    boolean isContextLost() { return contextLost; }
    void markContextLost() {
        contextLost = true;
    }

    @Override public GraphicsTypes.Backend backend() { return GraphicsTypes.Backend.GLES; }

    @Override public GraphicsSurface createSurface(SurfaceHolderLike holder, String label) {
        ensureEgl();
        return new GlesSurface(this, (Surface) holder.surface(), holder.width(), holder.height(), label);
    }

    @Override public GraphicsSurface createSurface(Surface surface, int width, int height, String label) {
        ensureEgl();
        return new GlesSurface(this, surface, width, height, label);
    }

    @Override public GraphicsContext createContext(GraphicsSurface surface) {
        ensureEgl();
        return new GlesContext(this, (GlesSurface) surface);
    }

    @Override public GraphicsBuffer createBuffer(GraphicsBuffer.Kind kind, int capacityBytes, String label) {
        return new GlesBuffer(this, kind, capacityBytes, label);
    }

    @Override public GraphicsTexture createTexture(int width, int height, GraphicsTypes.TextureFormat format,
                                                   GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                                                   GraphicsTypes.WrapMode wrap, String label) {
        return new GlesTexture(this, width, height, format, min, mag, wrap, label);
    }

    @Override public GraphicsSampler createSampler(GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                                                   GraphicsTypes.WrapMode u, GraphicsTypes.WrapMode v, String label) {
        return new GlesSampler(min, mag, u, v, label);
    }

    @Override public GraphicsShader createShader(GraphicsTypes.ShaderStage stage, String glsl, String label) {
        return new GlesShader(this, stage, glsl, label);
    }

    @Override public GraphicsPipeline createPipeline(GraphicsShader vs, GraphicsShader fs,
                                                     GraphicsTypes.BlendMode blend, String label) {
        return new GlesPipeline(this, (GlesShader) vs, (GlesShader) fs, blend, label);
    }

    /** Creates the capabilities snapshot — must run with the context current. */
    GlesCapabilities snapshotCapabilities() {
        return new GlesCapabilities();
    }

    @Override protected void onDestroy() {
        if (eglReady) {
            egl.release();
            eglReady = false;
        }
    }
}
