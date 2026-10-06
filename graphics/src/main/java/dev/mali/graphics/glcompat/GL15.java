package dev.mali.graphics.glcompat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * GL15 VBO subset — delegates to real GLES buffer objects and tracks the
 * ARRAY_BUFFER/ELEMENT_ARRAY_BUFFER bindings so glVertexAttribPointer can
 * resolve LWJGL2-style integer offsets.
 */
public final class GL15 {

    private GL15() {}

    public static void glGenBuffers(int n, IntBuffer ids) {
        android.opengl.GLES20.glGenBuffers(n, ids);
    }

    public static void glDeleteBuffers(int n, IntBuffer ids) {
        android.opengl.GLES20.glDeleteBuffers(n, ids);
    }

    public static void glBindBuffer(int target, int id) {
        GLContext c = GLContext.get();
        if (target == GL11.GL_ARRAY_BUFFER) c.arrayBuffer = id;
        else if (target == GL11.GL_ELEMENT_ARRAY_BUFFER) c.elementBuffer = id;
        else { c.setErr(GL11.GL_INVALID_ENUM); return; }
        android.opengl.GLES20.glBindBuffer(target, id);
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        android.opengl.GLES20.glBufferData(target, data == null ? 0 : data.remaining(), data, usage);
    }

    public static void glBufferData(int target, ShortBuffer data, int usage) {
        android.opengl.GLES20.glBufferData(target, data == null ? 0 : data.remaining() * 2, data, usage);
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        android.opengl.GLES20.glBufferData(target, data == null ? 0 : data.remaining() * 4, data, usage);
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        android.opengl.GLES20.glBufferData(target, data == null ? 0 : data.remaining() * 4, data, usage);
    }

    /** Size-only upload (null data). */
    public static void glBufferData(int target, long size, int usage) {
        android.opengl.GLES20.glBufferData(target, (int) size, null, usage);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer data) {
        android.opengl.GLES20.glBufferSubData(target, (int) offset, data.remaining(), data);
    }

    public static void glBufferSubData(int target, long offset, FloatBuffer data) {
        android.opengl.GLES20.glBufferSubData(target, (int) offset, data.remaining() * 4, data);
    }

    public static void glBufferSubData(int target, long offset, ShortBuffer data) {
        android.opengl.GLES20.glBufferSubData(target, (int) offset, data.remaining(), data);
    }

    public static boolean glIsBuffer(int id) { return id != 0; }

    /** LWJGL2-style: draws with a client ByteBuffer (direct) or bound VBO. */
    public static void glDrawArrays(int mode, int first, int count) {
        android.opengl.GLES20.glDrawArrays(mode, first, count);
    }

    public static void glDrawElements(int mode, int count, int type, ByteBuffer indices) {
        android.opengl.GLES20.glDrawElements(mode, count, type, indices);
    }

    public static void glDrawElements(int mode, int count, int type, long offset) {
        android.opengl.GLES20.glDrawElements(mode, count, type, (int) offset);
    }

    public static void glDrawRangeElements(int mode, int start, int end, int count, int type, long offset) {
        android.opengl.GLES20.glDrawElements(mode, count, type, (int) offset);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        android.opengl.GLES20.glVertexAttribPointer(0, size, type, false, stride, (int) offset);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        android.opengl.GLES20.glVertexAttribPointer(1, size, type, true, stride, (int) offset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        android.opengl.GLES20.glVertexAttribPointer(2, size, type, false, stride, (int) offset);
    }

    public static void glEnableClientState(int array) {
        // mapped in enablePointerArrays()
    }

    public static void glDisableClientState(int array) {
        // mapped in disablePointerArrays()
    }
}
