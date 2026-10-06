package dev.mali.graphics.gles;

import android.opengl.GLES20;

import java.nio.Buffer;

import dev.mali.graphics.api.GraphicsBuffer;
import dev.mali.graphics.api.GraphicsTypes;

final class GlesBuffer extends GraphicsBuffer {
    private final GlesDevice device;
    private final Kind kind;
    private final int capacityBytes;
    private int id = -1;

    GlesBuffer(GlesDevice device, Kind kind, int capacityBytes, String label) {
        super(label);
        this.device = device;
        this.kind = kind;
        this.capacityBytes = capacityBytes;
        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        id = ids[0];
        if (id == 0) throw new IllegalStateException("glGenBuffers failed: 0x"
                + Integer.toHexString(GLES20.glGetError()));
    }

    int id() { return id; }

    @Override public Kind kind() { return kind; }
    @Override public int sizeBytes() { return capacityBytes; }

    private int glTarget() { return kind == Kind.VERTEX ? GLES20.GL_ARRAY_BUFFER : GLES20.GL_ELEMENT_ARRAY_BUFFER; }

    @Override public void upload(Buffer data, int byteCount, GraphicsTypes.BufferUsage usage) {
        GLES20.glBindBuffer(glTarget(), id);
        GLES20.glBufferData(glTarget(), byteCount, data,
                usage == GraphicsTypes.BufferUsage.DYNAMIC ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(glTarget(), 0);
    }

    @Override public void update(Buffer data, int byteOffset, int byteCount) {
        GLES20.glBindBuffer(glTarget(), id);
        GLES20.glBufferSubData(glTarget(), byteOffset, byteCount, data);
        GLES20.glBindBuffer(glTarget(), 0);
    }

    void bind() { GLES20.glBindBuffer(glTarget(), id); }
    void unbind() { GLES20.glBindBuffer(glTarget(), 0); }

    @Override protected void onDestroy() {
        if (id > 0 && !device.isContextLost()) {
            GLES20.glDeleteBuffers(1, new int[]{id}, 0);
        }
        id = -1;
    }
}
