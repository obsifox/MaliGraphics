package dev.mali.graphics.gles;

import android.opengl.GLES20;

import java.util.HashMap;
import java.util.Map;

import dev.mali.graphics.api.GraphicsPipeline;
import dev.mali.graphics.api.GraphicsShader;
import dev.mali.graphics.api.GraphicsTypes;

public final class GlesPipeline extends GraphicsPipeline {
    private final GlesDevice device;
    private final GlesShader vs, fs;
    private final GraphicsTypes.BlendMode blend;
    private int program = -1;
    private final boolean valid;
    private final String log;
    private final long linkTimeMs;
    private final Map<String, Integer> uniforms = new HashMap<>();

    GlesPipeline(GlesDevice device, GlesShader vs, GlesShader fs,
                 GraphicsTypes.BlendMode blend, String label) {
        super(label);
        this.device = device;
        this.vs = vs;
        this.fs = fs;
        this.blend = blend;
        final long t0 = System.nanoTime();

        if (!vs.isValid() || !fs.isValid()) {
            this.valid = false;
            this.log = "shader stage invalid: vs=" + vs.isValid() + " fs=" + fs.isValid()
                    + " [" + vs.infoLog() + fs.infoLog() + "]";
            this.linkTimeMs = (System.nanoTime() - t0) / 1_000_000L;
            return;
        }
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs.id());
        GLES20.glAttachShader(prog, fs.id());
        GLES20.glLinkProgram(prog);
        int[] status = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0);
        String linkLog = GLES20.glGetProgramInfoLog(prog);
        if (status[0] == 0) {
            GLES20.glDeleteProgram(prog);
            this.valid = false;
            this.log = linkLog;
            this.program = -1;
        } else {
            this.valid = true;
            this.log = "";
            this.program = prog;
        }
        this.linkTimeMs = (System.nanoTime() - t0) / 1_000_000L;
    }

    int program() { return program; }

    @Override public GraphicsShader vertexShader() { return vs; }
    @Override public GraphicsShader fragmentShader() { return fs; }
    @Override public GraphicsTypes.BlendMode blendMode() { return blend; }
    @Override public boolean isValid() { return valid; }
    @Override public String infoLog() { return log; }
    @Override public long linkTimeMs() { return linkTimeMs; }

    @Override public int uniformLocation(String name) {
        Integer cached = uniforms.get(name);
        if (cached != null) return cached;
        int loc = valid ? GLES20.glGetUniformLocation(program, name) : -1;
        uniforms.put(name, loc);
        return loc;
    }

    /** Cached attribute locations (low CPU overhead rule §29 — never query per draw). */
    public int attribLocation(String name) {
        Integer cached = uniforms.get("@a" + name);
        if (cached != null) return cached;
        int loc = valid ? GLES20.glGetAttribLocation(program, name) : -1;
        uniforms.put("@a" + name, loc);
        return loc;
    }

    void use() {
        if (valid) GLES20.glUseProgram(program);
    }

    @Override protected void onDestroy() {
        if (program > 0 && !device.isContextLost()) {
            GLES20.glDeleteProgram(program);
        }
        program = -1;
    }
}
