package dev.mali.graphics.gles;

import android.opengl.GLES20;
import android.opengl.GLES30;

import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.command.BuiltInShaders;

/** GLES texture/sampler/buffer/shader/pipeline handles + GL call helpers. */
final class Gles {

    private Gles() {}

    static int texInternalFormat(GraphicsTypes.TextureFormat f) {
        // GL_ALPHA8 has no constant in GLES30 — literal 0x803C (ES1/ES2 legacy internal fmt)
        return f == GraphicsTypes.TextureFormat.RGBA8 ? GLES30.GL_RGBA8
                : f == GraphicsTypes.TextureFormat.RGB8 ? GLES30.GL_RGB8
                : 0x803C; // GL_ALPHA8
    }

    static int texFormat(GraphicsTypes.TextureFormat f) {
        return f == GraphicsTypes.TextureFormat.RGBA8 ? GLES20.GL_RGBA
                : f == GraphicsTypes.TextureFormat.RGB8 ? GLES20.GL_RGB
                : GLES20.GL_ALPHA;
    }

    static int filter(GraphicsTypes.FilterMode m) {
        return m == GraphicsTypes.FilterMode.NEAREST ? GLES20.GL_NEAREST : GLES20.GL_LINEAR;
    }

    static int wrap(GraphicsTypes.WrapMode w) {
        return w == GraphicsTypes.WrapMode.REPEAT ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
    }

    static int blendSrc(GraphicsTypes.BlendMode mode) {
        return mode == GraphicsTypes.BlendMode.PREMULTIPLIED ? GLES20.GL_ONE : GLES20.GL_SRC_ALPHA;
    }

    /** ES2 fallback shader sources when device caps are below ES3. */
    static String[] spriteShaderPair(boolean gles3) {
        return new String[]{BuiltInShaders.vertexSource(gles3), BuiltInShaders.fragmentSource(gles3)};
    }
}
