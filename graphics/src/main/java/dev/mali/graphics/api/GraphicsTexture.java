package dev.mali.graphics.api;

/** 2D texture handle. Pixels are supplied as RGBA8 (or ALPHA8 for glyph atlases). */
public abstract class GraphicsTexture extends GraphicsResource {
    protected GraphicsTexture(String label) { super(label); }

    public abstract int width();
    public abstract int height();

    /** Full upload/replacement. pixels = tightly packed bytes (row-major, origin top-left). */
    public abstract void uploadPixels(byte[] pixels, int w, int h, GraphicsTypes.TextureFormat format);

    /** Partial update (atlas streaming). */
    public abstract void updateRegion(int x, int y, int w, int h, byte[] pixels, GraphicsTypes.TextureFormat format);
}
