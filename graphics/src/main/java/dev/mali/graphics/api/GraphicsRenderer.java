package dev.mali.graphics.api;

/**
 * High-level renderer API used by scenes/launcher UI (mission §13, §18).
 * Immediate-mode recorder with batching underneath. Render thread only.
 */
public abstract class GraphicsRenderer extends GraphicsResource {
    protected GraphicsRenderer(String label) { super(label); }

    public abstract GraphicsFrame beginFrame();

    /** Screen-space transform: pixels → clip. Call once per resize. */
    public abstract void setCamera(int surfaceWidth, int surfaceHeight);

    public abstract void clearColor(float r, float g, float b, float a);

    public abstract void pushClip(int x, int y, int w, int h);
    public abstract void popClip();

    /** Draws a textured or flat quad (tint multiplies texture when texture != null). */
    public abstract void drawQuad(GraphicsTexture texture,
                                  float x, float y, float w, float h,
                                  float u0, float v0, float u1, float v1,
                                  float r, float g, float b, float a,
                                  float rotationDeg, float pivotX, float pivotY);

    public final void drawTexture(GraphicsTexture tex, float x, float y, float w, float h) {
        drawQuad(tex, x, y, w, h, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0);
    }

    public final void fillRect(float x, float y, float w, float h,
                               float r, float g, float b, float a) {
        drawQuad(null, x, y, w, h, 0, 0, 1, 1, r, g, b, a, 0, 0, 0);
    }

    public abstract void drawText(String text, float x, float y, float sizePx,
                                  float r, float g, float b, float a);

    public abstract void endFrameAndPresent();

    public abstract GraphicsProfiler profiler();
    public abstract GraphicsCapabilities capabilities();
    public abstract GraphicsDevice device();
}
