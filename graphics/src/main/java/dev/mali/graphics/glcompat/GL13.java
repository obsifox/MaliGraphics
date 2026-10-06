package dev.mali.graphics.glcompat;

/**
 * GL13 multitexture subset (MC-style lightmap on unit 1).
 * Only 2 units are fed into the immediate stream (uv0/uv1); other units
 * accept binds for state compatibility.
 */
public final class GL13 {

    private GL13() {}

    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;

    public static void glActiveTexture(int unit) {
        GLContext c = GLContext.get();
        int idx = unit - GL_TEXTURE0;
        if (idx < 0 || idx > 7) { c.setErr(GL11.GL_INVALID_ENUM); return; }
        c.activeUnit = idx;
        android.opengl.GLES20.glActiveTexture(unit);
    }

    /** Second unit texcoords go into the lightmap slot of the immediate stream. */
    public static void glMultiTexCoord2f(int unit, float s, float t) {
        GLContext c = GLContext.get();
        if (unit == GL_TEXTURE0) {
            c.uv0x = s; c.uv0y = t;
        } else if (unit == GL_TEXTURE1) {
            c.uv1x = s; c.uv1y = t;
        }
        // units 2+ have no immediate-stream slot; ignored (documented limitation)
    }

    public static void glClientActiveTexture(int unit) { /* fixed two-uv stream */ }
}
