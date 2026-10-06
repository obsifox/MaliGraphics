package dev.mali.graphics.core.math;

/** Column-major 4x4 matrix, OpenGL layout. Pure Java (unit tested). */
public final class Mat4 {
    public final float[] m = new float[16];

    public static Mat4 identity() {
        Mat4 r = new Mat4();
        r.m[0] = r.m[5] = r.m[10] = r.m[15] = 1f;
        return r;
    }

    /** Orthographic projection with (0,0) at top-left, y down — pixel space for UI. */
    public static Mat4 orthoPixels(int width, int height) {
        Mat4 r = new Mat4();
        if (width <= 0 || height <= 0) { return identity(); }
        final float lr = 1f / width, br = 1f / height;
        r.m[0] = 2f * lr;
        r.m[5] = -2f * br;            // y down
        r.m[10] = -1f;                // clip z: [-1,1] collapsed; UI z unused
        r.m[12] = -1f;
        r.m[13] = 1f;
        r.m[15] = 1f;
        return r;
    }

    /** this = translate(tx,ty) * scale(sx,sy) applied to base (recomputed fresh each call). */
    public static Mat4 trs(float tx, float ty, float sx, float sy, float rotDeg, float px, float py) {
        Mat4 r = identity();
        final double a = Math.toRadians(rotDeg);
        final float c = (float) Math.cos(a), s = (float) Math.sin(a);
        // rotation around pivot (px,py), then translate
        final float ox = -px, oy = -py;
        // columns: rot*scale
        r.m[0] = c * sx;  r.m[1] = s * sx;
        r.m[4] = -s * sy; r.m[5] = c * sy;
        r.m[12] = tx + (ox * c - oy * s) + px;
        r.m[13] = ty + (ox * s + oy * c) + py;
        return r;
    }

    public static Mat4 multiply(Mat4 a, Mat4 b) {
        Mat4 r = new Mat4();
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += a.m[k * 4 + row] * b.m[col * 4 + k];
                }
                r.m[col * 4 + row] = sum;
            }
        }
        return r;
    }
}
