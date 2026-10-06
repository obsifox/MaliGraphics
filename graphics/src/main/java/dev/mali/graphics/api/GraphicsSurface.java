package dev.mali.graphics.api;

import android.view.Surface;

/**
 * Presentation surface. Wraps an Android Surface (from SurfaceView/TextureView/
 * SurfaceTexture) without exposing backend internals (§9).
 */
public abstract class GraphicsSurface extends GraphicsResource {
    protected GraphicsSurface(String label) { super(label); }

    public abstract Surface surface();
    public abstract int width();
    public abstract int height();

    /** Called by the host view when the underlying size changes. */
    public abstract void resize(int width, int height);
}
