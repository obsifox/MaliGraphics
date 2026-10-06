package dev.mali.graphics.core.command;

/**
 * Flat preallocated op pool (docs/command-submission.md § no-GC rule).
 * Pure Java — unit tested for pooling correctness.
 */
public final class GraphicsCommandRecorder {

    // ---- op codes (single source of truth; GLES executor interprets these) ----
    public static final int OP_VIEWPORT = 0;
    public static final int OP_SCISSOR = 1;
    public static final int OP_CLEAR_SCISSOR = 2;
    public static final int OP_CLEAR_COLOR = 3;
    public static final int OP_BLEND = 4;
    public static final int OP_PIPELINE = 5;
    public static final int OP_TEXTURE = 6;
    public static final int OP_MATRIX = 7;
    public static final int OP_VEC4 = 8;
    public static final int OP_FLOAT = 9;
    public static final int OP_DRAW = 10;

    public static final class Op {
        public int code;
        public int i0, i1, i2, i3;
        public float f0, f1, f2, f3;
        public Object a, b;
    }

    public static final class Recorder {
        private Op[] ops;
        private int count;

        public Recorder(int capacity) {
            ops = new Op[capacity];
            for (int i = 0; i < capacity; i++) ops[i] = new Op();
        }

        public Op next(int code) {
            if (count == ops.length) grow();
            Op op = ops[count++];
            op.code = code;
            op.i0 = op.i1 = op.i2 = op.i3 = 0;
            op.f0 = op.f1 = op.f2 = op.f3 = 0f;
            op.a = op.b = null;
            return op;
        }

        private void grow() {
            Op[] bigger = new Op[ops.length * 2];
            System.arraycopy(ops, 0, bigger, 0, ops.length);
            for (int i = ops.length; i < bigger.length; i++) bigger[i] = new Op();
            ops = bigger;
        }

        public void reset() { count = 0; }
        public int count() { return count; }
        public Op[] ops() { return ops; }
    }

    private GraphicsCommandRecorder() {}
}
