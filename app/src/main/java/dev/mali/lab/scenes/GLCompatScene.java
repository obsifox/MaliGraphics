package dev.mali.lab.scenes;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.glcompat.GL11;
import dev.mali.graphics.glcompat.GL13;
import dev.mali.graphics.glcompat.GL15;
import dev.mali.graphics.glcompat.GL20;
import dev.mali.graphics.glcompat.GLU;
import dev.mali.graphics.glcompat.GLContext;
import dev.mali.graphics.gles.GlesRendererImpl;

/**
 * Test 13 — GL Compat Layer: renders the same frame path desktop-GL renderers
 * use (immediate quads + multitexture + fog + display list + GLSL 120 shader)
 * entirely through the Java GL->GLES translation layer.
 */
public final class GLCompatScene implements Scene {

    private int w, h;
    private int checkerTex = -1, lightTex = -1;
    private int quadList = -1;
    private int userProg = -1, uTime = -1, uAspect = -1;
    private FloatBuffer quadVerts, quadColors;
    private int vbo = -1;

    public static GLCompatScene single() { return new GLCompatScene(); }

    @Override public String name() { return "Test 13 — GL Compat Layer"; }

    @Override public void init(GlesRendererImpl renderer, int width, int height) {
        w = width; h = height;
        checkerTex = makeChecker(64);
        lightTex = makeLightGradient(16);

        // ---- display list of a 8x8 quad grid (MC-terrain style, unit0+unit1) ----
        quadList = GL11.glGenLists(1);
        GL11.glNewList(quadList, GL11.GL_COMPILE);
        buildGridQuads();
        GL11.glEndList();

        // ---- user shader through the GLSL translator (desktop GLSL 120 style) ----
        String vs = "#version 120\n" +
                "attribute vec4 aPos;\n" +
                "attribute vec4 aColor;\n" +
                "uniform mat4 uMVP2;\n" +
                "varying vec4 vColor;\n" +
                "void main() {\n" +
                "  gl_Position = uMVP2 * aPos;\n" +
                "  vColor = aColor;\n" +
                "}\n";
        String fs = "#version 120\n" +
                "uniform float uTime2;\n" +
                "varying vec4 vColor;\n" +
                "void main() {\n" +
                "  vec4 c = vColor;\n" +
                "  c.r *= 0.5 + 0.5 * sin(uTime2);\n" +
                "  gl_FragColor = c;\n" +
                "}\n";
        int vs2 = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs2, vs);
        GL20.glCompileShader(vs2);
        int fs2 = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs2, fs);
        GL20.glCompileShader(fs2);
        userProg = GL20.glCreateProgram();
        GL20.glAttachShader(userProg, vs2);
        GL20.glAttachShader(userProg, fs2);
        GL20.glBindAttribLocation(userProg, 0, "aPos");
        GL20.glBindAttribLocation(userProg, 1, "aColor");
        GL20.glLinkProgram(userProg);
        uTime = GL20.glGetUniformLocation(userProg, "uTime2");
        uAspect = GL20.glGetUniformLocation(userProg, "uMVP2");

        // ---- ortho quad for shader phase (direct FloatBuffer attribs) ----
        quadVerts = direct(new float[]{
                -0.6f, -0.3f, 0,  0.6f, -0.3f, 0,  0.6f, 0.3f, 0,
                -0.6f, -0.3f, 0,  0.6f, 0.3f, 0,  -0.6f, 0.3f, 0});
        quadColors = direct(new float[]{
                1, 0.3f, 0.3f, 1,  0.3f, 1, 0.3f, 1,  0.3f, 0.3f, 1, 1,
                1, 0.3f, 0.3f, 1,  0.3f, 0.3f, 1, 1,  0.3f, 1, 1, 1});

        int[] bufs = new int[1];
        GLES20.glGenBuffers(1, bufs, 0);
        vbo = bufs[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL11.GL_ARRAY_BUFFER, quadVerts, GL11.GL_STATIC_DRAW);
        int[] cb = new int[1];
        GLES20.glGenBuffers(1, cb, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, cb[0]);
        GL15.glBufferData(GL11.GL_ARRAY_BUFFER, quadColors, GL11.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        colorVbo = cb[0];
    }

    private int colorVbo = -1;

    @Override public void resize(int width, int height) { w = width; h = height; }

    @Override public void draw(GlesRendererImpl renderer, GraphicsFrame frame, long timeMs) {
        float t = timeMs / 1000f;

        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0.04f, 0.06f, 0.08f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // ---- projection + camera (desktop style) ----
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(60f, (float) w / (float) Math.max(1, h), 0.1f, 100f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GLU.gluLookAt(0f, 4.2f, 9.5f, 0f, 0.5f, 0f, 0f, 1f, 0f);

        // ---- fog (MC-linear) ----
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, 6f);
        GL11.glFogf(GL11.GL_FOG_END, 16f);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 1f);

        // ---- textures: unit0 checker, unit1 lightmap ----
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, checkerTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        // ---- animated spinning grid via display list (immediate-mode capture) ----
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, 0f, 0f);
        GL11.glRotatef(t * 20f, 0f, 1f, 0f);
        GL11.glCallList(quadList);
        GL11.glPopMatrix();

        // ---- GL20 user-shader quad (translated source) ----
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL20.glUseProgram(userProg);
        GL20.glUniform1f(uTime, t);
        uploadAspect();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorVbo);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL15.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glUseProgram(0);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void uploadAspect() {
        // simple identity MVP uniform; shader animates color only
        FloatBuffer fb = direct(new float[]{
                1, 0, 0, 0,  0, 1, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1});
        GL20.glUniformMatrix4fv(uAspect, 1, false, fb);
    }

    /** 8x8 grid of quads, immediate mode, two texture units. */
    private void buildGridQuads() {
        for (int gx = -4; gx < 4; gx++) {
            for (int gz = -4; gz < 4; gz++) {
                float x0 = gx * 1.05f, z0 = gz * 1.05f;
                float x1 = x0 + 1f, z1 = z0 + 1f;
                float shade = ((gx + gz) & 1) == 0 ? 1f : 0.75f;
                GL11.glColor4f(shade, shade, shade, 1f);
                GL11.glTexCoord2f(0f, 0f);
                GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, 0f, 0f);
                GL11.glVertex3f(x0, 0f, z0);
                GL11.glTexCoord2f(0f, 1f);
                GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, 0f, 1f);
                GL11.glVertex3f(x0, 0f, z1);
                GL11.glTexCoord2f(1f, 1f);
                GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, 1f, 1f);
                GL11.glVertex3f(x1, 0f, z1);
                GL11.glTexCoord2f(1f, 0f);
                GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, 1f, 0f);
                GL11.glVertex3f(x1, 0f, z0);
            }
        }
    }

    private static int makeChecker(int size) {
        ByteBuffer px = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / 8) + (y / 8)) % 2 == 0;
                px.put((byte) (on ? 230 : 40));
                px.put((byte) (on ? 200 : 120));
                px.put((byte) (on ? 90 : 220));
                px.put((byte) 255);
            }
        }
        px.flip();
        IntBuffer ids = newIntBuffer(1);
        GLES20.glGenTextures(1, ids);
        int id = ids.get(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, size, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
        return id;
    }

    private static int makeLightGradient(int size) {
        ByteBuffer px = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float v = 0.35f + 0.65f * (1f - (float) y / size);
                px.put((byte) 255).put((byte) 255).put((byte) (int) (200 * v)).put((byte) 255);
            }
        }
        px.flip();
        IntBuffer ids = newIntBuffer(1);
        GLES20.glGenTextures(1, ids);
        int id = ids.get(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, size, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
        return id;
    }

    private static IntBuffer newIntBuffer(int n) {
        return ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    private static FloatBuffer direct(float[] data) {
        FloatBuffer fb = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(data);
        fb.flip();
        return fb;
    }

    @Override public String statsLine() {
        GLContext c = GLContext.get();
        return "GL-compat draws/frame: " + c.compatDrawCalls
                + " | quads: 64 (display list) | units: 2";
    }
}
