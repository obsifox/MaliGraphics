package dev.mali.graphics.gles;

import android.opengl.GLES20;

import dev.mali.graphics.api.GraphicsShader;
import dev.mali.graphics.api.GraphicsTypes;

final class GlesShader extends GraphicsShader {
    private final GlesDevice device;
    private final GraphicsTypes.ShaderStage stage;
    private int id = -1;
    private final boolean valid;
    private final String log;
    private final long compileTimeMs;

    GlesShader(GlesDevice device, GraphicsTypes.ShaderStage stage, String source, String label) {
        super(label);
        this.device = device;
        this.stage = stage;
        final long t0 = System.nanoTime();

        int type = stage == GraphicsTypes.ShaderStage.VERTEX ? GLES20.GL_VERTEX_SHADER : GLES20.GL_FRAGMENT_SHADER;
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            this.valid = false;
            this.log = "glCreateShader returned 0 (0x" + Integer.toHexString(GLES20.glGetError()) + ")";
            this.compileTimeMs = (System.nanoTime() - t0) / 1_000_000L;
            device.profilerSafe().onShaderCompile(label, compileTimeMs);
            return;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        String errLog = GLES20.glGetShaderInfoLog(shader);
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader);
            this.valid = false;
            this.log = errLog;
            this.id = -1;
        } else {
            this.valid = true;
            this.log = "";
            this.id = shader;
        }
        this.compileTimeMs = (System.nanoTime() - t0) / 1_000_000L;
        device.profilerSafe().onShaderCompile(label, compileTimeMs);
    }

    int id() { return id; }

    @Override public GraphicsTypes.ShaderStage stage() { return stage; }
    @Override public boolean isValid() { return valid; }
    @Override public String infoLog() { return log; }
    @Override public long compileTimeMs() { return compileTimeMs; }

    @Override protected void onDestroy() {
        if (id > 0 && !device.isContextLost()) {
            GLES20.glDeleteShader(id);
        }
        id = -1;
    }
}
