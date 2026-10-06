package dev.mali.graphics.api;

/** Backend-neutral enums and value types (mission §9: public API stays backend-independent). */
public final class GraphicsTypes {
    private GraphicsTypes() {}

    /** Backend selection (mission §24 fallback ladder). */
    public enum Backend { AUTO, GLES, VULKAN, ANDROID_NATIVE }

    /** Texture formats supported in stage 1. */
    public enum TextureFormat { RGBA8, RGB8, ALPHA8 }

    public enum FilterMode { NEAREST, LINEAR }

    public enum WrapMode { CLAMP_TO_EDGE, REPEAT }

    /** Standard alpha blend presets. */
    public enum BlendMode { OFF, ALPHA, PREMULTIPLIED, ADDITIVE }

    public enum BufferUsage { STATIC, DYNAMIC }

    public enum ShaderStage { VERTEX, FRAGMENT }

    /** Simple 2D integer rect for clips/atlas packing. */
    public static final class RectI {
        public int x, y, width, height;
        public RectI() {}
        public RectI(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        @Override public String toString() {
            return "RectI(" + x + "," + y + " " + width + "x" + height + ")";
        }
    }
}
