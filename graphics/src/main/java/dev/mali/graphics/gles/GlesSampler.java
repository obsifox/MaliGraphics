package dev.mali.graphics.gles;

import android.opengl.GLES20;

import dev.mali.graphics.api.GraphicsSampler;
import dev.mali.graphics.api.GraphicsTypes;

/**
 * Stage 1 note (docs/shader-pipeline.md): on GLES2 there are no sampler objects,
 * so a GraphicsSampler is a state template applied to the bound texture.
 * On ES3+ we could use glBindSampler; texture params cover our use.
 */
final class GlesSampler extends GraphicsSampler {
    private final GraphicsTypes.FilterMode min, mag;
    private final GraphicsTypes.WrapMode u, v;

    GlesSampler(GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                GraphicsTypes.WrapMode u, GraphicsTypes.WrapMode v, String label) {
        super(label);
        this.min = min; this.mag = mag; this.u = u; this.v = v;
    }

    /** Applies params to the currently bound GL_TEXTURE_2D. */
    void apply() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, Gles.filter(min));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, Gles.filter(mag));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, Gles.wrap(u));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, Gles.wrap(v));
    }

    @Override public GraphicsTypes.FilterMode minFilter() { return min; }
    @Override public GraphicsTypes.FilterMode magFilter() { return mag; }
    @Override public GraphicsTypes.WrapMode wrapU() { return u; }
    @Override public GraphicsTypes.WrapMode wrapV() { return v; }

    @Override protected void onDestroy() { /* no GL object on ES2 */ }
}
