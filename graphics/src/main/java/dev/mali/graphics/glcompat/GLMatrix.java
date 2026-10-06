package dev.mali.graphics.glcompat;

/**
 * Column-major 4x4 matrix (GL layout: m[12]=tx, m[13]=ty, m[14]=tz) for the
 * GL translation layer. Mirrors the fixed-function matrix stack semantics.
 * All matrices are mutated in place; {@link #copy()} clones for stacks.
 */
public final class GLMatrix {
    public final float[] m = new float[16];

    private GLMatrix() {}

    public static GLMatrix identity() {
        GLMatrix r = new GLMatrix();
        r.m[0] = 1; r.m[5] = 1; r.m[10] = 1; r.m[15] = 1;
        return r;
    }

    public GLMatrix copy() {
        GLMatrix r = new GLMatrix();
        System.arraycopy(m, 0, r.m, 0, 16);
        return r;
    }

    public void load(GLMatrix o) { System.arraycopy(o.m, 0, m, 0, 16); }

    public void load(float[] m16) { System.arraycopy(m16, 0, m, 0, 16); }

    public void loadIdentity() {
        java.util.Arrays.fill(m, 0f);
        m[0] = 1; m[5] = 1; m[10] = 1; m[15] = 1;
    }

    /** this = this * o (column-major). */
    public void multiply(GLMatrix o) {
        float[] a = new float[16];
        System.arraycopy(m, 0, a, 0, 16);
        float[] b = o.m;
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m[c * 4 + r] = a[r] * b[c * 4]
                        + a[4 + r] * b[c * 4 + 1]
                        + a[8 + r] * b[c * 4 + 2]
                        + a[12 + r] * b[c * 4 + 3];
            }
        }
    }

    public void multiply(float[] b) {
        float[] a = new float[16];
        System.arraycopy(m, 0, a, 0, 16);
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m[c * 4 + r] = a[r] * b[c * 4]
                        + a[4 + r] * b[c * 4 + 1]
                        + a[8 + r] * b[c * 4 + 2]
                        + a[12 + r] * b[c * 4 + 3];
            }
        }
    }

    public void translate(float x, float y, float z) {
        GLMatrix t = identity();
        t.m[12] = x; t.m[13] = y; t.m[14] = z;
        multiply(t);
    }

    public void scale(float x, float y, float z) {
        GLMatrix s = identity();
        s.m[0] = x; s.m[5] = y; s.m[10] = z;
        multiply(s);
    }

    /** Angle in degrees around axis (x,y,z) — unit axes or arbitrary. */
    public void rotate(float deg, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0f) return;
        x /= len; y /= len; z /= len;
        double rad = Math.toRadians(deg);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        float ic = 1f - c;
        GLMatrix r = identity();
        r.m[0] = x * x * ic + c;      r.m[4] = x * y * ic - z * s;  r.m[8]  = x * z * ic + y * s;
        r.m[1] = y * x * ic + z * s;  r.m[5] = y * y * ic + c;      r.m[9]  = y * z * ic - x * s;
        r.m[2] = z * x * ic - y * s;  r.m[6] = z * y * ic + x * s;  r.m[10] = z * z * ic + c;
        multiply(r);
    }

    public void frustum(float l, float r, float b, float t, float n, float f) {
        GLMatrix f2 = new GLMatrix();
        f2.m[0] = 2f * n / (r - l);
        f2.m[5] = 2f * n / (t - b);
        f2.m[8] = (r + l) / (r - l);
        f2.m[9] = (t + b) / (t - b);
        f2.m[10] = -(f + n) / (f - n);
        f2.m[11] = -1f;
        f2.m[14] = -2f * f * n / (f - n);
        multiply(f2);
    }

    public void ortho(float l, float r, float b, float t, float n, float f) {
        GLMatrix o = identity();
        o.m[0] = 2f / (r - l);
        o.m[5] = 2f / (t - b);
        o.m[10] = -2f / (f - n);
        o.m[12] = -(r + l) / (r - l);
        o.m[13] = -(t + b) / (t - b);
        o.m[14] = -(f + n) / (f - n);
        multiply(o);
    }

    public void perspective(float fovyDeg, float aspect, float zNear, float zFar) {
        float fh = (float) Math.tan(Math.toRadians(fovyDeg) / 2.0) * zNear;
        float fw = fh * aspect;
        frustum(-fw, fw, -fh, fh, zNear, zFar);
    }

    /** Writes column-major values into the buffer at its current position, then flips. */
    public void toBuffer(java.nio.FloatBuffer fb) {
        fb.put(m);
        fb.flip();
    }

    private static final float[] TMP = new float[16];

    /** Computes this * other and writes the result into the buffer (then flips). */
    public void copyMultiplyTo(GLMatrix other, java.nio.FloatBuffer fb) {
        System.arraycopy(m, 0, TMP, 0, 16);
        float[] b = other.m;
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m[c * 4 + r] = TMP[r] * b[c * 4]
                        + TMP[4 + r] * b[c * 4 + 1]
                        + TMP[8 + r] * b[c * 4 + 2]
                        + TMP[12 + r] * b[c * 4 + 3];
            }
        }
        toBuffer(fb);
        // restore (m was overwritten): copy back from TMP
        System.arraycopy(TMP, 0, m, 0, 16);
    }
}
