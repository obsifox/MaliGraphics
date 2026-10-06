package dev.mali.graphics.glcompat;

import java.nio.FloatBuffer;

/**
 * GL20 shader/program subset. Shader sources pass through {@link GLSLTranslator}
 * (desktop GLSL -> GLSL ES) before reaching GLES20. Uniforms/attribs delegate.
 */
public final class GL20 {

    private GL20() {}

    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_ACTIVE_UNIFORMS = 0x8B86;
    public static final int GL_ACTIVE_ATTRIBUTES = 0x8B89;

    public static int glCreateShader(int type) {
        GLContext c = GLContext.get();
        if (type != GL_VERTEX_SHADER && type != GL_FRAGMENT_SHADER) {
            c.setErr(GL11.GL_INVALID_ENUM);
            return 0;
        }
        return android.opengl.GLES20.glCreateShader(type);
    }

    public static void glShaderSource(int shader, String source) {
        GLContext.pendingSources.put(shader, source);
    }

    public static void glCompileShader(int shader) {
        String src = GLContext.pendingSources.remove(shader);
        if (src == null) src = "";
        String es = GLSLTranslator.toEs(shaderTypeOf(shader), src);
        android.opengl.GLES20.glShaderSource(shader, es);
        android.opengl.GLES20.glCompileShader(shader);
    }

    private static int shaderTypeOf(int shader) {
        int[] t = new int[1];
        android.opengl.GLES20.glGetShaderiv(shader, android.opengl.GLES20.GL_SHADER_TYPE, t, 0);
        return t[0];
    }

    public static void glGetShaderiv(int shader, int pname, int[] params, int offset) {
        android.opengl.GLES20.glGetShaderiv(shader, pname, params, offset);
    }

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return android.opengl.GLES20.glGetShaderInfoLog(shader);
    }

    public static int glCreateProgram() { return android.opengl.GLES20.glCreateProgram(); }

    public static void glDeleteProgram(int program) { android.opengl.GLES20.glDeleteProgram(program); }

    public static void glAttachShader(int program, int shader) {
        android.opengl.GLES20.glAttachShader(program, shader);
    }

    public static void glDetachShader(int program, int shader) {
        android.opengl.GLES20.glDetachShader(program, shader);
    }

    public static void glLinkProgram(int program) { android.opengl.GLES20.glLinkProgram(program); }

    public static void glGetProgramiv(int program, int pname, int[] params, int offset) {
        android.opengl.GLES20.glGetProgramiv(program, pname, params, offset);
    }

    public static String glGetProgramInfoLog(int program, int maxLength) {
        return android.opengl.GLES20.glGetProgramInfoLog(program);
    }

    public static void glUseProgram(int program) { android.opengl.GLES20.glUseProgram(program); }

    public static int glGetAttribLocation(int program, String name) {
        return android.opengl.GLES20.glGetAttribLocation(program, name);
    }

    public static int glGetUniformLocation(int program, String name) {
        return android.opengl.GLES20.glGetUniformLocation(program, name);
    }

    public static void glBindAttribLocation(int program, int index, String name) {
        android.opengl.GLES20.glBindAttribLocation(program, index, name);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, long offset) {
        GLContext c = GLContext.get();
        if (c.arrayBuffer == 0) {
            GLContext.get().setErr(GL11.GL_INVALID_OPERATION);
            return;
        }
        android.opengl.GLES20.glVertexAttribPointer(index, size, type, normalized, stride, (int) offset);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, FloatBuffer data) {
        android.opengl.GLES20.glVertexAttribPointer(index, size, type, normalized, stride, data);
    }

    public static void glEnableVertexAttribArray(int index) {
        android.opengl.GLES20.glEnableVertexAttribArray(index);
    }

    public static void glDisableVertexAttribArray(int index) {
        android.opengl.GLES20.glDisableVertexAttribArray(index);
    }

    public static void glUniform1i(int location, int v) { android.opengl.GLES20.glUniform1i(location, v); }
    public static void glUniform1f(int location, float v) { android.opengl.GLES20.glUniform1f(location, v); }
    public static void glUniform2f(int l, float x, float y) { android.opengl.GLES20.glUniform2f(l, x, y); }
    public static void glUniform3f(int l, float x, float y, float z) { android.opengl.GLES20.glUniform3f(l, x, y, z); }
    public static void glUniform4f(int l, float x, float y, float z, float w) { android.opengl.GLES20.glUniform4f(l, x, y, z, w); }
    public static void glUniform1fv(int l, int count, FloatBuffer v) { android.opengl.GLES20.glUniform1fv(l, count, v); }
    public static void glUniform2fv(int l, int count, FloatBuffer v) { android.opengl.GLES20.glUniform2fv(l, count, v); }
    public static void glUniform3fv(int l, int count, FloatBuffer v) { android.opengl.GLES20.glUniform3fv(l, count, v); }
    public static void glUniform4fv(int l, int count, FloatBuffer v) { android.opengl.GLES20.glUniform4fv(l, count, v); }
    public static void glUniformMatrix4fv(int l, int count, boolean transpose, FloatBuffer m) {
        android.opengl.GLES20.glUniformMatrix4fv(l, count, transpose, m);
    }

    public static void glValidateProgram(int program) { android.opengl.GLES20.glValidateProgram(program); }
}
