package dev.mali.graphics.glcompat;

/**
 * Minimal GLU for the compat layer (gluPerspective/gluLookAt) — implemented on
 * top of GL11 matrix ops, matching the LWJGL2 GLU usage of desktop renderers.
 */
public final class GLU {

    private GLU() {}

    public static void gluPerspective(float fovyDeg, float aspect, float zNear, float zFar) {
        GLContext c = GLContext.get();
        if (c.recording != null) {
            c.recording.add(() -> c.currentMatrix().perspective(fovyDeg, aspect, zNear, zFar));
            return;
        }
        c.currentMatrix().perspective(fovyDeg, aspect, zNear, zFar);
    }

    public static void gluLookAt(float eyeX, float eyeY, float eyeZ,
                                 float cx, float cy, float cz,
                                 float upX, float upY, float upZ) {
        float fx = cx - eyeX, fy = cy - eyeY, fz = cz - eyeZ;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl == 0f) return;
        fx /= fl; fy /= fl; fz /= fl;

        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl != 0f) { sx /= sl; sy /= sl; sz /= sl; }

        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        GLMatrix m = GLMatrix.identity();
        m.m[0] = sx; m.m[4] = sy; m.m[8]  = sz;
        m.m[1] = ux; m.m[5] = uy; m.m[9]  = uz;
        m.m[2] = -fx; m.m[6] = -fy; m.m[10] = -fz;

        // apply immediately (outside display lists; MC uses lookAt outside lists too)
        float[] mf = new float[16];
        System.arraycopy(m.m, 0, mf, 0, 16);
        GLContext c = GLContext.get();
        c.currentMatrix().multiply(mf);
        GL11.glTranslatef(-eyeX, -eyeY, -eyeZ);
    }
}
