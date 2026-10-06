package dev.mali.graphics.glcompat;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GL11 fixed-function surface (LWJGL2-style static API) translated on the fly
 * to GLES 2.0 through {@link GLContext}. This is the Java equivalent of what
 * gl4es does in C — user decision 2026-10 — staying inside the mission's
 * Java-only rule (§31) while enabling desktop-GL-style renderers (§minecraft).
 *
 * Implemented subset: immediate mode (QUADS/TRIANGLES/STRIP/FAN/LINES/POINTS),
 * matrix stacks, GL_TEXTURE_2D on 2 units (multitexture via GL13), textures,
 * display lists, fog, alpha test, attrib push/pop, blend, depth, clear.
 */
public final class GL11 {

    private GL11() {}

    private static GLContext ctx() { return GLContext.get(); }

    // ---- enums: geometry ----------------------------------------------
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_QUADS = 0x0007;              // compat (desktop value)
    public static final int GL_QUAD_STRIP = 0x0008;         // compat
    public static final int GL_POLYGON = 0x0009;            // compat

    // ---- enums: depth/alpha ---------------------------------------------
    public static final int GL_NEVER = 0x0200;
    public static final int GL_LESS = 0x0201;
    public static final int GL_EQUAL = 0x0202;
    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_GREATER = 0x0204;
    public static final int GL_NOTEQUAL = 0x0205;
    public static final int GL_GEQUAL = 0x0206;
    public static final int GL_ALWAYS = 0x0207;

    // ---- enums: blend ----------------------------------------------------
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 0x0300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_DST_COLOR = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
    public static final int GL_SRC_ALPHA_SATURATE = 0x0308;

    // ---- enums: caps -------------------------------------------------------
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_LIGHTING = 0x0B50;
    public static final int GL_COLOR_MATERIAL = 0x0B57;
    public static final int GL_FOG = 0x0B60;
    public static final int GL_NORMALIZE = 0x0BA1;
    public static final int GL_RESCALE_NORMAL = 0x803A;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_ALPHA_TEST = 0x0BC0;
    public static final int GL_DITHER = 0x0BD0;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_FRONT = 0x0404;
    public static final int GL_BACK = 0x0405;
    public static final int GL_FRONT_AND_BACK = 0x0408;

    // ---- enums: matrix -----------------------------------------------------
    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;
    public static final int GL_TEXTURE = 0x1702;

    // ---- enums: pixel formats/types ----------------------------------------
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;

    // ---- enums: sampler state ------------------------------------------------
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_CLAMP = 0x2900;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;

    // ---- enums: lists / hints ------------------------------------------------
    public static final int GL_COMPILE = 0x1300;
    public static final int GL_COMPILE_AND_EXECUTE = 0x1301;
    public static final int GL_FASTEST = 0x1101;
    public static final int GL_NICEST = 0x1102;
    public static final int GL_DONT_CARE = 0x1100;

    // ---- enums: fog ------------------------------------------------------------
    public static final int GL_EXP = 0x0800;
    public static final int GL_EXP2 = 0x0801;
    public static final int GL_FOG_MODE = 0x0B65;
    public static final int GL_FOG_START = 0x0B63;
    public static final int GL_FOG_END = 0x0B64;
    public static final int GL_FOG_COLOR = 0x0B66;
    public static final int GL_FOG_DENSITY = 0x0B62;

    // ---- enums: buffers / pixel store --------------------------------------------
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    public static final int GL_PACK_ALIGNMENT = 0x0D05;

    // ---- enums: errors / gets ------------------------------------------------------
    public static final int GL_NO_ERROR = 0;
    public static final int GL_INVALID_ENUM = 0x0500;
    public static final int GL_INVALID_VALUE = 0x0501;
    public static final int GL_INVALID_OPERATION = 0x0502;
    public static final int GL_STACK_OVERFLOW = 0x0503;
    public static final int GL_STACK_UNDERFLOW = 0x0504;
    public static final int GL_OUT_OF_MEMORY = 0x0505;
    public static final int GL_MODELVIEW_MATRIX = 0x0BA6;
    public static final int GL_PROJECTION_MATRIX = 0x0BA7;
    public static final int GL_VIEWPORT = 0x0BA2;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;

    // ===================================================================== //
    // error
    // ===================================================================== //
    public static int glGetError() { return ctx().takeErr(); }

    public static String glGetString(int name) { return android.opengl.GLES20.glGetString(name); }

    // ===================================================================== //
    // immediate mode
    // ===================================================================== //
    public static void glBegin(int mode) {
        GLContext c = ctx();
        if (c.recording != null) {
            c.recording.add(() -> c.glBeginImpl(mode));
            c.inBegin = false;
            return;
        }
        c.glBeginImpl(mode);
    }

    public static void glEnd() {
        GLContext c = ctx();
        if (c.recording != null) {
            final float[] data = new float[c.vCount * 11];
            System.arraycopy(c.vtx, 0, data, 0, c.vCount * 11);
            final int count = c.vCount, mode = c.beginMode;
            c.recording.add(() -> c.drawSnapshot(data, count, mode));
            c.vCount = 0;
            c.inBegin = false;
            return;
        }
        c.glEndImpl();
    }

    public static void glVertex2f(float x, float y) { glVertex3f(x, y, 0f); }

    public static void glVertex3f(float x, float y, float z) {
        GLContext c = ctx();
        float[] q = c.quadScratch();
        q[0] = x; q[1] = y; q[2] = z;
        q[3] = c.cr; q[4] = c.cg; q[5] = c.cb; q[6] = c.ca;
        q[7] = c.uv0x; q[8] = c.uv0y;
        q[9] = c.uv1x; q[10] = c.uv1y;
        c.accumVertex();
    }

    public static void glVertex2i(int x, int y) { glVertex3f(x, y, 0f); }
    public static void glVertex3i(int x, int y, int z) { glVertex3f(x, y, z); }

    public static void glColor3f(float r, float g, float b) { glColor4f(r, g, b, 1f); }

    public static void glColor4f(float r, float g, float b, float a) {
        GLContext c = ctx();
        c.cr = r; c.cg = g; c.cb = b; c.ca = a;
    }

    public static void glColor4ub(byte r, byte g, byte b, byte a) {
        glColor4f((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, (a & 0xFF) / 255f);
    }

    public static void glTexCoord2f(float s, float t) {
        GLContext c = ctx();
        c.uv0x = s; c.uv0y = t;
    }

    public static void glNormal3f(float nx, float ny, float nz) { /* no-op: FF lighting unsupported */ }

    // ===================================================================== //
    // matrix
    // ===================================================================== //
    public static void glMatrixMode(int mode) {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(() -> c.matrixMode = mode); return; }
        c.matrixMode = mode;
    }

    public static void glLoadIdentity() {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(c.currentMatrix()::loadIdentity); return; }
        c.currentMatrix().loadIdentity();
    }

    public static void glTranslatef(float x, float y, float z) {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(() -> c.currentMatrix().translate(x, y, z)); return; }
        c.currentMatrix().translate(x, y, z);
    }

    public static void glScalef(float x, float y, float z) {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(() -> c.currentMatrix().scale(x, y, z)); return; }
        c.currentMatrix().scale(x, y, z);
    }

    public static void glRotatef(float deg, float x, float y, float z) {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(() -> c.currentMatrix().rotate(deg, x, y, z)); return; }
        c.currentMatrix().rotate(deg, x, y, z);
    }

    public static void glMultMatrixf(FloatBuffer m) {
        final float[] a = new float[16];
        m.get(a);
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(() -> c.currentMatrix().multiply(a)); return; }
        c.currentMatrix().multiply(a);
    }

    public static void glFrustum(double l, double r, double b, double t, double n, double f) {
        GLContext c = ctx();
        if (c.recording != null) {
            c.recording.add(() -> c.currentMatrix().frustum((float) l, (float) r, (float) b, (float) t, (float) n, (float) f));
            return;
        }
        c.currentMatrix().frustum((float) l, (float) r, (float) b, (float) t, (float) n, (float) f);
    }

    public static void glOrtho(double l, double r, double b, double t, double n, double f) {
        GLContext c = ctx();
        if (c.recording != null) {
            c.recording.add(() -> c.currentMatrix().ortho((float) l, (float) r, (float) b, (float) t, (float) n, (float) f));
            return;
        }
        c.currentMatrix().ortho((float) l, (float) r, (float) b, (float) t, (float) n, (float) f);
    }

    public static void glPushMatrix() {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(c::pushMatrix); return; }
        c.pushMatrix();
    }

    public static void glPopMatrix() {
        GLContext c = ctx();
        if (c.recording != null) { c.recording.add(c::popMatrix); return; }
        c.popMatrix();
    }

    // ===================================================================== //
    // clear / viewport / depth / blend
    // ===================================================================== //
    public static void glClearColor(float r, float g, float b, float a) { ctx().clearColorImpl(r, g, b, a); }

    public static void glClear(int mask) { android.opengl.GLES20.glClear(mask); }

    public static void glViewport(int x, int y, int w, int h) { ctx().viewportImpl(x, y, w, h); }

    public static void glEnable(int cap) { ctx().enable(cap); }

    public static void glDisable(int cap) { ctx().disable(cap); }

    public static boolean glIsEnabled(int cap) { return ctx().isCap(cap); }

    public static void glBlendFunc(int sf, int df) { ctx().glBlendFuncImpl(sf, df); }

    public static void glBlendFuncSeparate(int srgb, int drgb, int sa, int da) {
        ctx().glBlendFuncSeparateImpl(srgb, drgb, sa, da);
    }

    public static void glDepthFunc(int func) { android.opengl.GLES20.glDepthFunc(func); }

    public static void glDepthMask(boolean flag) { ctx().depthMaskImpl(flag); }

    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) {
        android.opengl.GLES20.glColorMask(r, g, b, a);
    }

    public static void glCullFace(int mode) { android.opengl.GLES20.glCullFace(mode); }

    public static void glPixelStorei(int pname, int param) {
        if (pname == GL_UNPACK_ALIGNMENT) ctx().unpackAlignment = param;
        android.opengl.GLES20.glPixelStorei(pname, param);
    }

    public static void glFlush() { /* vsync present handles it (§8) */ }

    public static void glFinish() { /* no-op: single queue (§command-submission) */ }

    // ===================================================================== //
    // fog / alpha
    // ===================================================================== //
    public static void glFogf(int pname, float param) {
        GLContext c = ctx();
        switch (pname) {
            case GL_FOG_START: c.fogStart = param; break;
            case GL_FOG_END: c.fogEnd = param; break;
            case GL_FOG_DENSITY: c.fogDensity = param; break;
            case GL_FOG_MODE: c.fogMode = (int) param; break;
            default: break;
        }
    }

    public static void glFogi(int pname, int param) { glFogf(pname, param); }

    public static void glFogfv(int pname, FloatBuffer params) {
        GLContext c = ctx();
        if (pname == GL_FOG_COLOR) {
            c.fogColor[0] = params.get(params.position());
            c.fogColor[1] = params.get(params.position() + 1);
            c.fogColor[2] = params.get(params.position() + 2);
            c.fogColor[3] = params.get(params.position() + 3);
        }
    }

    public static void glAlphaFunc(int func, float ref) {
        GLContext c = ctx();
        c.alphaFunc = func;
        c.alphaRef = ref;
    }

    // ===================================================================== //
    // textures
    // ===================================================================== //
    public static void glGenTextures(int n, IntBuffer ids) {
        android.opengl.GLES20.glGenTextures(n, ids);
    }

    public static void glDeleteTextures(int n, IntBuffer ids) {
        android.opengl.GLES20.glDeleteTextures(n, ids);
    }

    public static void glBindTexture(int target, int id) {
        GLContext c = ctx();
        if (target != GL_TEXTURE_2D) { c.setErr(GL_INVALID_ENUM); return; }
        if (c.recording != null) {
            final int unit = c.activeUnit;
            c.recording.add(() -> {
                c.boundTex[unit] = id;
                android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, id);
            });
            return;
        }
        c.boundTex[c.activeUnit] = id;
        android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, id);
    }

    public static int glGetTextureBound() { return ctx().boundTex[ctx().activeUnit]; }

    public static void glTexImage2D(int target, int level, int internalFormat,
                                    int width, int height, int border,
                                    int format, int type, Buffer pixels) {
        GLContext c = ctx();
        if (level != 0) { c.setErr(GL_INVALID_VALUE); return; }  // no mipmap generation in shim
        if (target != GL_TEXTURE_2D) { c.setErr(GL_INVALID_ENUM); return; }
        android.opengl.GLES20.glTexImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0,
                format, width, height, 0, format, type, pixels);
        c.texSizes.put(c.boundTex[c.activeUnit], new int[]{width, height});
    }

    public static void glTexSubImage2D(int target, int level, int xoff, int yoff,
                                       int width, int height, int format, int type, Buffer pixels) {
        android.opengl.GLES20.glTexSubImage2D(android.opengl.GLES20.GL_TEXTURE_2D,
                level, xoff, yoff, width, height, format, type, pixels);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, pname, param);
    }

    public static int glGetTexSize(int id) {
        int[] wh = ctx().texSizes.get(id);
        return wh == null ? 0 : wh[0] * wh[1];
    }

    // ===================================================================== //
    // display lists
    // ===================================================================== //
    public static int glGenLists(int range) { return ctx().genLists(range); }

    public static void glDeleteLists(int base, int range) { ctx().deleteLists(base, range); }

    public static boolean glIsList(int id) { return ctx().isList(id); }

    public static void glNewList(int id, int mode) {
        GLContext c = ctx();
        c.newList(id, mode);
        if (c.recording == null) return; // error recorded
        if (mode == GL_COMPILE_AND_EXECUTE) {
            // execute live while recording
            c.recordingId = id;
        }
    }

    public static void glEndList() { ctx().endList(); }

    public static void glCallList(int id) { ctx().callList(id); }

    // ===================================================================== //
    // attrib stack (light subset: blend/tex/alpha/fog/depth/cull)
    // ===================================================================== //
    public static void glPushAttrib(int mask) { ctx().pushAttrib(); }

    public static void glPopAttrib() { ctx().popAttrib(); }

    // ===================================================================== //
    // getters (subset)
    // ===================================================================== //
    public static void glGetFloatv(int pname, FloatBuffer params) {
        GLContext c = ctx();
        switch (pname) {
            case GL_MODELVIEW_MATRIX: c.modelview.toBuffer(params); break;
            case GL_PROJECTION_MATRIX: c.projection.toBuffer(params); break;
            default: c.setErr(GL_INVALID_ENUM);
        }
    }

    public static void glGetIntegerv(int pname, IntBuffer params) {
        GLContext c = ctx();
        switch (pname) {
            case GL_VIEWPORT:
                params.put(c.viewport[0]); params.put(c.viewport[1]);
                params.put(c.viewport[2]); params.put(c.viewport[3]);
                params.flip();
                break;
            case GL_ARRAY_BUFFER:
                params.put(c.arrayBuffer); params.flip();
                break;
            default: c.setErr(GL_INVALID_ENUM);
        }
    }

    // ===================================================================== //
    // hints / no-ops (accepted for source compatibility)
    // ===================================================================== //
    public static void glHint(int target, int mode) { }
    public static void glShadeModel(int mode) { }
    public static void glLightModeli(int pname, int param) { }
    public static void glLightModelf(int pname, float param) { }
    public static void glLightfv(int light, int pname, FloatBuffer params) { /* FF lighting unsupported */ }
    public static void glMaterialfv(int face, int pname, FloatBuffer params) { }
    public static void glMaterialf(int face, int pname, float param) { }
    public static void glTexEnvf(int target, int pname, float param) { }
    public static void glTexEnvi(int target, int pname, int param) { }
    public static void glDepthRange(double n, double f) { }
    public static void glLineWidth(float w) { android.opengl.GLES20.glLineWidth(w); }
    public static void glPolygonOffset(float factor, float units) {
        android.opengl.GLES20.glPolygonOffset(factor, units);
    }
}
