package dev.mali.graphics.gles;

import android.opengl.GLES20;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import dev.mali.graphics.api.GraphicsTexture;
import dev.mali.graphics.api.GraphicsTypes;

/** GLES texture — public because the font factory and scenes hold handles. */
public final class GlesTexture extends GraphicsTexture {
    private final GlesDevice device;
    private int id = -1;
    private final int width, height;
    private final boolean gles3;
    private GraphicsTypes.TextureFormat format;
    private GraphicsTypes.FilterMode min, mag;
    private GraphicsTypes.WrapMode wrap;

    GlesTexture(GlesDevice device, int width, int height, GraphicsTypes.TextureFormat format,
                GraphicsTypes.FilterMode min, GraphicsTypes.FilterMode mag,
                GraphicsTypes.WrapMode wrap, String label) {
        super(label);
        this.device = device;
        this.width = width;
        this.height = height;
        this.format = format;
        this.gles3 = device.isGles3Context();
        this.min = min; this.mag = mag; this.wrap = wrap;
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        id = ids[0];
        if (id == 0) throw new IllegalStateException("glGenTextures failed: 0x"
                + Integer.toHexString(GLES20.glGetError()));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        if (gles3) {
            GLES30.glTexStorage2D(GLES20.GL_TEXTURE_2D, 1, Gles.texInternalFormat(format), width, height);
        } else {
            // ES2 (Utgard): immutable storage unavailable — sized texImage2D with null data
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, Gles.texFormat(format),
                    width, height, 0, Gles.texFormat(format), GLES20.GL_UNSIGNED_BYTE, null);
        }
        applyParams();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void applyParams() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, Gles.filter(min));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, Gles.filter(mag));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, Gles.wrap(wrap));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, Gles.wrap(wrap));
    }

    int id() { return id; }

    @Override public int width() { return width; }
    @Override public int height() { return height; }

    @Override public void uploadPixels(byte[] pixels, int w, int h, GraphicsTypes.TextureFormat fmt) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        ByteBuffer bb = ByteBuffer.allocateDirect(pixels.length).order(ByteOrder.nativeOrder());
        bb.put(pixels).position(0);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, w, h,
                Gles.texFormat(fmt), GLES20.GL_UNSIGNED_BYTE, bb);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override public void updateRegion(int x, int y, int w, int h, byte[] pixels, GraphicsTypes.TextureFormat fmt) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        ByteBuffer bb = ByteBuffer.allocateDirect(pixels.length).order(ByteOrder.nativeOrder());
        bb.put(pixels).position(0);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, x, y, w, h,
                Gles.texFormat(fmt), GLES20.GL_UNSIGNED_BYTE, bb);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void bind(int unit) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
    }

    static void unbind(int unit) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override protected void onDestroy() {
        if (id > 0 && !device.isContextLost()) {
            GLES20.glDeleteTextures(1, new int[]{id}, 0);
        }
        id = -1;
    }
}
