package dev.mali.graphics.glcompat;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Software GL state machine for the Java GL->GLES translation layer
 * (mission §12 "backend-neutral", §minecraft-rendering; user decision 2026-10:
 * in-Java translation layer, no native shims). One instance is bound per
 * render thread; all GLES calls it issues hit the EGL context that is current
 * on that thread (our EglCore).
 *
 * Immediate-mode vertex layout (interleaved, 11 floats):
 *   [0..2]  position xyz
 *   [3..6]  color rgba
 *   [7..8]  texunit0 uv
 *   [9..10] texunit1 uv (lightmap-style second unit, MC multitexture)
 */
public final class GLContext {

    /** Bound on first use from the render thread (single render thread by design §8). */
    private static volatile GLContext current;

    /** Pending desktop-GLSL sources keyed by real GLES shader id (GL20.glShaderSource). */
    public static final java.util.Map<Integer, String> pendingSources =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static GLContext get() {
        if (current == null) current = new GLContext();
        return current;
    }

    // ---- matrix ---------------------------------------------------------
    public final GLMatrix modelview = GLMatrix.identity();
    public final GLMatrix projection = GLMatrix.identity();
    public final GLMatrix textureM = GLMatrix.identity();
    public final ArrayDeque<GLMatrix> mvStack = new ArrayDeque<>();
    public final ArrayDeque<GLMatrix> projStack = new ArrayDeque<>();
    public final ArrayDeque<GLMatrix> texStack = new ArrayDeque<>();
    public int matrixMode = GL11.GL_MODELVIEW;
    private static final int STACK_LIMIT = 32;

    // ---- texture units --------------------------------------------------
    public int activeUnit = 0;
    public final int[] boundTex = new int[8];
    public final boolean[] tex2DEnabled = new boolean[8];
    public float uv0x, uv0y, uv1x, uv1y;

    // ---- immediate ------------------------------------------------------
    public boolean inBegin = false;
    public int beginMode;
    public float[] vtx = new float[11 * 2048];
    public int vCount = 0;          // vertices
    private final float[] quadTmp = new float[4 * 11];
    private int quadN = 0;
    public float cr = 1f, cg = 1f, cb = 1f, ca = 1f;

    /** Package-visible vertex scratch for GL11.glVertex*. */
    float[] quadScratch() { return quadTmp; }

    // ---- misc state -----------------------------------------------------
    public final float[] clearColor = {0f, 0f, 0f, 1f};
    public int blendSrc = GL11.GL_SRC_ALPHA, blendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
    public int alphaFunc = GL11.GL_GREATER;
    public float alphaRef = 0.1f;
    public int fogMode = GL11.GL_LINEAR;
    public float fogStart = 1f, fogEnd = 0f, fogDensity = 1f;
    public final float[] fogColor = {0f, 0f, 0f, 0f};
    public int unpackAlignment = 4;
    public int err = 0;
    public final int[] viewport = {0, 0, 1, 1};
    public int arrayBuffer = 0, elementBuffer = 0;
    public final Map<Integer, int[]> texSizes = new HashMap<>();  // id -> {w,h}

    // ---- display lists ----------------------------------------------------
    public final Map<Integer, List<Runnable>> lists = new HashMap<>();
    public List<Runnable> recording = null;
    public int recordingId = 0;
    private int nextListId = 1;

    // ---- light attrib stack ----------------------------------------------
    public static final class Attrib {
        boolean tex0, tex1, blend, fog, alpha, depth, cull;
        int blendSrc, blendDst;
        float alphaRef;
        boolean depthMaskOn;
    }
    public final ArrayDeque<Attrib> attribStack = new ArrayDeque<>();
    public boolean depthMaskOn = true;

    // ---- FF program -------------------------------------------------------
    private boolean ffReady = false;
    private int ffProg = -1;
    private int uMVP, uMV, uTex0, uTex1, uUseTex0, uUseTex1, uUseFog,
            uFogStart, uFogEnd, uFogColor, uAlphaTest;
    private int streamVbo = -1;
    private ByteBuffer streamBuf = ByteBuffer.allocateDirect(11 * 4 * 4096)
            .order(ByteOrder.nativeOrder());
    private final FloatBuffer mvpBuf = ByteBuffer.allocateDirect(16 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final FloatBuffer mvBuf = ByteBuffer.allocateDirect(16 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

    // ---- stats ------------------------------------------------------------
    public int compatDrawCalls = 0;

    // ===================================================================== //
    // errors
    // ===================================================================== //
    public void setErr(int e) { if (err == 0) err = e; }
    public int takeErr() { int e = err; err = 0; return e; }

    // ===================================================================== //
    // display lists
    // ===================================================================== //
    public int genLists(int range) {
        if (range <= 0) { setErr(GL11.GL_INVALID_VALUE); return 0; }
        int base = nextListId;
        for (int i = 0; i < range; i++) lists.put(base + i, new ArrayList<Runnable>());
        nextListId += range;
        return base;
    }

    public void deleteLists(int base, int range) {
        for (int i = 0; i < range; i++) lists.remove(base + i);
    }

    public boolean isList(int id) { return lists.containsKey(id); }

    public void newList(int id, int mode) {
        List<Runnable> l = lists.get(id);
        if (l == null || recording != null) { setErr(GL11.GL_INVALID_OPERATION); return; }
        l.clear();
        recording = l;
        recordingId = id;
    }

    public void endList() {
        if (recording == null) { setErr(GL11.GL_INVALID_OPERATION); return; }
        recording = null;
        recordingId = 0;
    }

    public void callList(int id) {
        List<Runnable> l = lists.get(id);
        if (l == null) { setErr(GL11.GL_INVALID_VALUE); return; }
        if (recording != null) {
            recording.add(() -> callList(id));
            return;
        }
        for (int i = 0, n = l.size(); i < n; i++) l.get(i).run();
    }

    // ===================================================================== //
    // matrix ops (record-safe wrappers live in GL11)
    // ===================================================================== //
    public GLMatrix currentMatrix() {
        switch (matrixMode) {
            case GL11.GL_PROJECTION: return projection;
            case GL11.GL_TEXTURE: return textureM;
            default: return modelview;
        }
    }

    public ArrayDeque<GLMatrix> currentStack() {
        switch (matrixMode) {
            case GL11.GL_PROJECTION: return projStack;
            case GL11.GL_TEXTURE: return texStack;
            default: return mvStack;
        }
    }

    public void pushMatrix() {
        ArrayDeque<GLMatrix> st = currentStack();
        if (st.size() >= STACK_LIMIT) { setErr(GL11.GL_STACK_OVERFLOW); return; }
        st.push(currentMatrix().copy());
    }

    public void popMatrix() {
        ArrayDeque<GLMatrix> st = currentStack();
        GLMatrix top = st.pollFirst();
        if (top == null) { setErr(GL11.GL_STACK_UNDERFLOW); return; }
        currentMatrix().load(top);
    }

    public void getMatrixFloats(float[] out16) {
        System.arraycopy(currentMatrix().m, 0, out16, 0, 16);
    }

    // ===================================================================== //
    // immediate mode
    // ===================================================================== //
    private void ensureVtxCapacity(int extraFloats) {
        if (vCount * 11 + extraFloats > vtx.length) {
            float[] n = new float[Math.max(vtx.length * 2, vCount * 11 + extraFloats)];
            System.arraycopy(vtx, 0, n, 0, vCount * 11);
            vtx = n;
        }
    }

    private void emitVertex() {
        ensureVtxCapacity(11);
        int o = vCount * 11;
        vtx[o] = quadTmp[0]; vtx[o + 1] = quadTmp[1]; vtx[o + 2] = quadTmp[2];
        vtx[o + 3] = quadTmp[3]; vtx[o + 4] = quadTmp[4]; vtx[o + 5] = quadTmp[5]; vtx[o + 6] = quadTmp[6];
        vtx[o + 7] = quadTmp[7]; vtx[o + 8] = quadTmp[8];
        vtx[o + 9] = quadTmp[9]; vtx[o + 10] = quadTmp[10];
        vCount++;
    }

    /** GL_QUADS -> triangles expansion happens here on the 4th vertex. */
    public void accumVertex() {
        if (beginMode == GL11.GL_QUADS) {
            quadN++;
            if (quadN == 4) {
                int s = 11;
                ensureVtxCapacity(66);
                // emit v0,v1,v2 then v0,v2,v3
                float[] v = vtx; int base = vCount * 11;
                System.arraycopy(quadTmp, 0,      v, base,        s);
                System.arraycopy(quadTmp, s,      v, base + s,    s);
                System.arraycopy(quadTmp, 2 * s,  v, base + 2*s,  s);
                System.arraycopy(quadTmp, 0,      v, base + 3*s,  s);
                System.arraycopy(quadTmp, 2 * s,  v, base + 4*s,  s);
                System.arraycopy(quadTmp, 3 * s,  v, base + 5*s,  s);
                vCount += 6;
                quadN = 0;
            }
            return;
        }
        emitVertex();
    }

    public void glBeginImpl(int mode) {
        if (inBegin) { setErr(GL11.GL_INVALID_OPERATION); return; }
        if (mode < GL11.GL_POINTS || mode > GL11.GL_QUADS) { setErr(GL11.GL_INVALID_ENUM); return; }
        inBegin = true;
        beginMode = mode;
        vCount = 0;
        quadN = 0;
    }

    public void glEndImpl() {
        if (!inBegin) { setErr(GL11.GL_INVALID_OPERATION); return; }
        inBegin = false;
        drawImmediate(vtx, vCount, beginMode);
    }

    /** Snapshot used by display-list recording (state applied at replay time). */
    public void drawSnapshot(float[] data, int count, int mode) {
        drawImmediate(data, count, mode);
    }

    private void drawImmediate(float[] data, int count, int mode) {
        if (count <= 0) return;
        ensureFF();
        GLES20.glUseProgram(ffProg);

        mvpBuf.rewind();
        projection.copyMultiplyTo(modelview, mvpBuf);
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpBuf);
        mvBuf.rewind();
        modelview.toBuffer(mvBuf);
        GLES20.glUniformMatrix4fv(uMV, 1, false, mvBuf);

        boolean useT0 = tex2DEnabled[0] && boundTex[0] != 0;
        boolean useT1 = tex2DEnabled[1] && boundTex[1] != 0;
        GLES20.glUniform1f(uUseTex0, useT0 ? 1f : 0f);
        GLES20.glUniform1f(uUseTex1, useT1 ? 1f : 0f);
        GLES20.glUniform1i(uTex0, 0);
        GLES20.glUniform1i(uTex1, 1);
        GLES20.glUniform1f(uAlphaTest,
                isCap(GL11.GL_ALPHA_TEST) && alphaFunc == GL11.GL_GREATER ? alphaRef : 0f);
        boolean fog = isCap(GL11.GL_FOG) && fogMode == GL11.GL_LINEAR && fogEnd > fogStart;
        GLES20.glUniform1f(uUseFog, fog ? 1f : 0f);
        GLES20.glUniform1f(uFogStart, fogStart);
        GLES20.glUniform1f(uFogEnd, fogEnd);
        GLES20.glUniform4fv(uFogColor, 1, fogColor, 0);

        int glMode = mode;
        switch (mode) {
            case GL11.GL_QUADS: glMode = GLES20.GL_TRIANGLES; break;
            case GL11.GL_POLYGON: glMode = GLES20.GL_TRIANGLE_FAN; break;
            case GL11.GL_QUAD_STRIP: glMode = GLES20.GL_TRIANGLE_STRIP; break;
            default: glMode = mode; // GL_POINTS/LINES/.../TRIANGLES identical values
        }

        if (streamVbo == -1) {
            int[] b = new int[1];
            GLES20.glGenBuffers(1, b, 0);
            streamVbo = b[0];
        }
        int bytes = count * 11 * 4;
        if (streamBuf.capacity() < bytes) {
            streamBuf = ByteBuffer.allocateDirect(bytes * 2).order(ByteOrder.nativeOrder());
        }
        streamBuf.clear();
        streamBuf.asFloatBuffer().put(data, 0, count * 11);
        streamBuf.position(0);
        streamBuf.limit(bytes);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, streamVbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bytes, streamBuf, GLES20.GL_STREAM_DRAW);

        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glEnableVertexAttribArray(2);
        GLES20.glEnableVertexAttribArray(3);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 44, 0);
        GLES20.glVertexAttribPointer(1, 4, GLES20.GL_FLOAT, false, 44, 12);
        GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, 44, 28);
        GLES20.glVertexAttribPointer(3, 2, GLES20.GL_FLOAT, false, 44, 36);

        GLES20.glDrawArrays(glMode, 0, count);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        compatDrawCalls++;
    }

    // ===================================================================== //
    // caps / attrib stack
    // ===================================================================== //
    private final boolean[] caps = new boolean[16];
    private static int capIndex(int cap) {
        switch (cap) {
            case GL11.GL_TEXTURE_2D: return 0;   // NOTE: active-unit enable, see enable/disable
            case GL11.GL_BLEND: return 1;
            case GL11.GL_DEPTH_TEST: return 2;
            case GL11.GL_FOG: return 3;
            case GL11.GL_ALPHA_TEST: return 4;
            case GL11.GL_CULL_FACE: return 5;
            default: return -1;
        }
    }

    public boolean isCap(int cap) {
        if (cap == GL11.GL_TEXTURE_2D) return tex2DEnabled[activeUnit];
        int i = capIndex(cap);
        return i >= 0 && caps[i];
    }

    public void enable(int cap) {
        if (cap == GL11.GL_TEXTURE_2D) {
            tex2DEnabled[activeUnit] = true;
            if (activeUnit == 1) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glEnable(GLES20.GL_TEXTURE_2D);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + activeUnit);
            }
            return;
        }
        int i = capIndex(cap);
        if (i >= 0) caps[i] = true;
        Integer es = esCap(cap);
        if (es != null) GLES20.glEnable(es);
        // GL_LIGHTING/LIGHTi/ALPHA_TEST on FF handled in shader; others no-op
    }

    public void disable(int cap) {
        if (cap == GL11.GL_TEXTURE_2D) {
            tex2DEnabled[activeUnit] = false;
            if (activeUnit == 1) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glDisable(GLES20.GL_TEXTURE_2D);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + activeUnit);
            }
            return;
        }
        int i = capIndex(cap);
        if (i >= 0) caps[i] = false;
        Integer es = esCap(cap);
        if (es != null) GLES20.glDisable(es);
    }

    private static Integer esCap(int cap) {
        switch (cap) {
            case GL11.GL_BLEND: return GLES20.GL_BLEND;
            case GL11.GL_DEPTH_TEST: return GLES20.GL_DEPTH_TEST;
            case GL11.GL_CULL_FACE: return GLES20.GL_CULL_FACE;
            default: return null;
        }
    }

    public void pushAttrib() {
        Attrib a = new Attrib();
        a.tex0 = tex2DEnabled[0]; a.tex1 = tex2DEnabled[1];
        a.blend = caps[1]; a.depth = caps[2]; a.fog = caps[3];
        a.alpha = caps[4]; a.cull = caps[5];
        a.blendSrc = blendSrc; a.blendDst = blendDst;
        a.alphaRef = alphaRef; a.depthMaskOn = depthMaskOn;
        if (attribStack.size() >= STACK_LIMIT) { setErr(GL11.GL_STACK_OVERFLOW); return; }
        attribStack.push(a);
    }

    public void popAttrib() {
        Attrib a = attribStack.pollFirst();
        if (a == null) { setErr(GL11.GL_STACK_UNDERFLOW); return; }
        // restore through the public state so ES mirrors it
        activeUnit = 0;
        if (a.tex0) enable(GL11.GL_TEXTURE_2D); else disable(GL11.GL_TEXTURE_2D);
        activeUnit = 1;
        if (a.tex1) enable(GL11.GL_TEXTURE_2D); else disable(GL11.GL_TEXTURE_2D);
        activeUnit = 0;
        if (a.blend) enable(GL11.GL_BLEND); else disable(GL11.GL_BLEND);
        if (a.depth) enable(GL11.GL_DEPTH_TEST); else disable(GL11.GL_DEPTH_TEST);
        if (a.fog) enable(GL11.GL_FOG); else disable(GL11.GL_FOG);
        if (a.alpha) enable(GL11.GL_ALPHA_TEST); else disable(GL11.GL_ALPHA_TEST);
        if (a.cull) enable(GL11.GL_CULL_FACE); else disable(GL11.GL_CULL_FACE);
        glBlendFuncImpl(a.blendSrc, a.blendDst);
        alphaRef = a.alphaRef;
        depthMaskOn = a.depthMaskOn;
        GLES20.glDepthMask(a.depthMaskOn);
    }

    // ===================================================================== //
    // passthrough state (mirrors ES + tracks values)
    // ===================================================================== //
    public void glBlendFuncImpl(int sf, int df) {
        blendSrc = sf; blendDst = df;
        GLES20.glBlendFunc(sf, df);
    }

    public void glBlendFuncSeparateImpl(int srgb, int drgb, int sa, int da) {
        GLES20.glBlendFuncSeparate(srgb, drgb, sa, da);
    }

    public void clearColorImpl(float r, float g, float b, float a) {
        clearColor[0] = r; clearColor[1] = g; clearColor[2] = b; clearColor[3] = a;
        GLES20.glClearColor(r, g, b, a);
    }

    public void viewportImpl(int x, int y, int w, int h) {
        viewport[0] = x; viewport[1] = y; viewport[2] = w; viewport[3] = h;
        GLES20.glViewport(x, y, w, h);
    }

    public void depthMaskImpl(boolean flag) {
        depthMaskOn = flag;
        GLES20.glDepthMask(flag);
    }

    // ===================================================================== //
    // FF program
    // ===================================================================== //
    private void ensureFF() {
        if (ffReady) return;
        String vs =
                "attribute vec4 aPos;\n" +
                "attribute vec4 aColor;\n" +
                "attribute vec2 aUV0;\n" +
                "attribute vec2 aUV1;\n" +
                "uniform mat4 uMVP;\n" +
                "uniform mat4 uMV;\n" +
                "varying vec4 vColor;\n" +
                "varying vec2 vUV0;\n" +
                "varying vec2 vUV1;\n" +
                "varying float vFogDist;\n" +
                "void main() {\n" +
                "  gl_Position = uMVP * aPos;\n" +
                "  vColor = aColor;\n" +
                "  vUV0 = aUV0;\n" +
                "  vUV1 = aUV1;\n" +
                "  vec4 mv = uMV * aPos;\n" +
                "  vFogDist = length(mv.xyz);\n" +
                "}\n";
        String fs =
                "precision mediump float;\n" +
                "varying vec4 vColor;\n" +
                "varying vec2 vUV0;\n" +
                "varying vec2 vUV1;\n" +
                "varying float vFogDist;\n" +
                "uniform sampler2D uTex0;\n" +
                "uniform sampler2D uTex1;\n" +
                "uniform float uUseTex0;\n" +
                "uniform float uUseTex1;\n" +
                "uniform float uUseFog;\n" +
                "uniform float uFogStart;\n" +
                "uniform float uFogEnd;\n" +
                "uniform vec4 uFogColor;\n" +
                "uniform float uAlphaTest;\n" +
                "void main() {\n" +
                "  vec4 c = vColor;\n" +
                "  if (uUseTex0 > 0.5) c *= texture2D(uTex0, vUV0);\n" +
                "  if (uUseTex1 > 0.5) c *= texture2D(uTex1, vUV1);\n" +
                "  if (c.a < uAlphaTest) discard;\n" +
                "  if (uUseFog > 0.5) {\n" +
                "    float f = clamp((uFogEnd - vFogDist) / (uFogEnd - uFogStart), 0.0, 1.0);\n" +
                "    c.rgb = mix(uFogColor.rgb, c.rgb, f);\n" +
                "  }\n" +
                "  gl_FragColor = c;\n" +
                "}\n";
        int v = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(v, vs);
        GLES20.glCompileShader(v);
        int f = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(f, fs);
        GLES20.glCompileShader(f);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(v, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) { setErr(GL11.GL_INVALID_OPERATION); return; }
        GLES20.glGetShaderiv(f, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) { setErr(GL11.GL_INVALID_OPERATION); return; }
        ffProg = GLES20.glCreateProgram();
        GLES20.glAttachShader(ffProg, v);
        GLES20.glAttachShader(ffProg, f);
        GLES20.glBindAttribLocation(ffProg, 0, "aPos");
        GLES20.glBindAttribLocation(ffProg, 1, "aColor");
        GLES20.glBindAttribLocation(ffProg, 2, "aUV0");
        GLES20.glBindAttribLocation(ffProg, 3, "aUV1");
        GLES20.glLinkProgram(ffProg);
        GLES20.glGetProgramiv(ffProg, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) { setErr(GL11.GL_INVALID_OPERATION); return; }
        uMVP = GLES20.glGetUniformLocation(ffProg, "uMVP");
        uMV = GLES20.glGetUniformLocation(ffProg, "uMV");
        uTex0 = GLES20.glGetUniformLocation(ffProg, "uTex0");
        uTex1 = GLES20.glGetUniformLocation(ffProg, "uTex1");
        uUseTex0 = GLES20.glGetUniformLocation(ffProg, "uUseTex0");
        uUseTex1 = GLES20.glGetUniformLocation(ffProg, "uUseTex1");
        uUseFog = GLES20.glGetUniformLocation(ffProg, "uUseFog");
        uFogStart = GLES20.glGetUniformLocation(ffProg, "uFogStart");
        uFogEnd = GLES20.glGetUniformLocation(ffProg, "uFogEnd");
        uFogColor = GLES20.glGetUniformLocation(ffProg, "uFogColor");
        uAlphaTest = GLES20.glGetUniformLocation(ffProg, "uAlphaTest");
        ffReady = true;
    }
}
