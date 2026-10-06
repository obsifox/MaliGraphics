package dev.mali.graphics.api;

/**
 * Logical device = backend entry point (§10).
 * Implementations: gles.GlesDevice (Stage 1), vulkan.* (Stage 5).
 */
public abstract class GraphicsDevice extends GraphicsResource {
    protected GraphicsDevice(String label) { super(label); }

    public abstract GraphicsTypes.Backend backend();

    public abstract GraphicsSurface createSurface(SurfaceHolderLike holder, String label);

    /** Create a surface from any Android Surface (TextureView/SurfaceTexture). */
    public abstract GraphicsSurface createSurface(android.view.Surface surface, int width, int height, String label);

    public abstract GraphicsContext createContext(GraphicsSurface surface);

    public abstract GraphicsBuffer createBuffer(GraphicsBuffer.Kind kind, int capacityBytes, String label);
    public abstract GraphicsTexture createTexture(int width, int height, GraphicsTypes.TextureFormat format,
                                                  GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                                                  GraphicsTypes.WrapMode wrap, String label);
    public abstract GraphicsSampler createSampler(GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                                                  GraphicsTypes.WrapMode u, GraphicsTypes.WrapMode v, String label);
    public abstract GraphicsShader createShader(GraphicsTypes.ShaderStage stage, String glsl, String label);
    public abstract GraphicsPipeline createPipeline(GraphicsShader vs, GraphicsShader fs,
                                                    GraphicsTypes.BlendMode blend, String label);

    /** Minimal holder abstraction so core code never imports android.view. */
    public interface SurfaceHolderLike {
        Object surface();   // android.view.Surface at runtime
        int width();
        int height();
    }
}
