package dev.mali.graphics.gles;

import android.opengl.GLES20;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.mali.graphics.api.GraphicsCapabilities;

/** GLES capability snapshot (mission §11). Built right after context creation. */
final class GlesCapabilities extends GraphicsCapabilities {
    private final int major, minor;
    private final String vendor, renderer, version, glsl;
    private final int maxTexSize, maxTexUnits, maxVertexAttrs, maxRenderbuffer;
    private final Map<String, Boolean> extensions;
    private boolean lost;

    GlesCapabilities() {
        super("gles-caps");
        String versionStr = GLES20.glGetString(GLES20.GL_VERSION);
        this.version = versionStr == null ? "unknown" : versionStr;
        int mj = 2, mn = 0;
        try {
            // e.g. "OpenGL ES 3.2 V@0615.…" or "OpenGL ES 2.0 ..."
            String[] parts = versionStr.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("OpenGL".equals(parts[i]) && i + 1 < parts.length && "ES".equals(parts[i + 1])) {
                    String[] vm = parts[i + 2].split("\\.");
                    mj = Integer.parseInt(vm[0]);
                    mn = Integer.parseInt(vm[1]);
                    break;
                }
            }
        } catch (Exception e) { mj = 2; mn = 0; }
        this.major = mj;
        this.minor = mn;

        String v = GLES20.glGetString(GLES20.GL_VENDOR);
        String r = GLES20.glGetString(GLES20.GL_RENDERER);
        String g = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION);
        this.vendor = v == null ? "unknown" : v;
        this.renderer = r == null ? "unknown" : r;
        this.glsl = g == null ? "unknown" : g;

        int[] buf = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, buf, 0);
        this.maxTexSize = buf[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buf, 0);
        this.maxTexUnits = buf[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, buf, 0);
        this.maxVertexAttrs = buf[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, buf, 0);
        this.maxRenderbuffer = buf[0];

        this.extensions = new LinkedHashMap<>();
        String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        if (ext != null) {
            for (String token : ext.split("\\s+")) {
                if (!token.isEmpty()) extensions.put(token, Boolean.TRUE);
            }
        }
    }

    @Override public int glMajorVersion() { return major; }
    @Override public int glMinorVersion() { return minor; }
    @Override public String vendor() { return vendor; }
    @Override public String renderer() { return renderer; }
    @Override public String version() { return version; }
    @Override public String glslVersion() { return glsl; }
    @Override public int maxTextureSize() { return maxTexSize; }
    @Override public int maxTextureUnits() { return maxTexUnits; }
    @Override public int maxVertexAttribs() { return maxVertexAttrs; }
    @Override public int maxRenderBufferSize() { return maxRenderbuffer; }
    @Override public Map<String, Boolean> extensions() { return extensions; }
    @Override public boolean isGles3() { return major >= 3; }
    void markLost() { lost = true; }
    @Override protected void onDestroy() { /* snapshot object */ }
}
