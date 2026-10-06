package dev.mali.graphics.gles;


import dev.mali.graphics.api.GraphicsBuffer;
import dev.mali.graphics.api.GraphicsPipeline;
import dev.mali.graphics.api.GraphicsTexture;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.command.GraphicsCommandRecorder;

/**
 * Recorded command list with pooled op slots (docs/command-submission.md).
 * Recording allocates nothing after warm-up; executeNow() runs on the GL thread.
 */
final class GlesCommandBuffer extends dev.mali.graphics.api.GraphicsCommandBuffer {
    private final GlesContext context;
    private GraphicsCommandRecorder.Recorder recorder = new GraphicsCommandRecorder.Recorder(1024);

    GlesCommandBuffer(GlesContext context, String label) {
        super(label);
        this.context = context;
    }

    GraphicsCommandRecorder.Recorder recorder() { return recorder; }

    @Override public void reset() { recorder.reset(); }
    @Override public int opCount() { return recorder.count(); }

    void executeNow() {
        GlesCommandExecutor.execute(recorder.ops(), recorder.count(), null);
        // reset for next frame happens via renderer endFrame
    }

    @Override public void setViewport(int x, int y, int w, int h) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_VIEWPORT);
        op.i0 = x; op.i1 = y; op.i2 = w; op.i3 = h;
    }

    @Override public void setScissor(int x, int y, int w, int h) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_SCISSOR);
        op.i0 = x; op.i1 = y; op.i2 = w; op.i3 = h;
    }

    @Override public void clearScissor() {
        recorder.next(GraphicsCommandRecorder.OP_CLEAR_SCISSOR);
    }

    @Override public void setClearColor(float r, float g, float b, float a) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_CLEAR_COLOR);
        op.f0 = r; op.f1 = g; op.f2 = b; op.f3 = a;
    }

    @Override public void setBlend(GraphicsTypes.BlendMode mode) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_BLEND);
        op.i0 = mode.ordinal();
        op.i1 = Gles.blendSrc(mode);
    }

    @Override public void bindPipeline(GraphicsPipeline pipeline) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_PIPELINE);
        op.a = pipeline;
    }

    @Override public void bindTexture(int unit, GraphicsTexture texture) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_TEXTURE);
        op.i0 = unit;
        op.a = texture;
    }

    @Override public void setUniformMatrix(String name, float[] m4, int offset) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_MATRIX);
        op.a = name;
        op.b = m4;
        op.i0 = offset;
    }

    @Override public void setUniformVec4(String name, float x, float y, float z, float w) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_VEC4);
        op.a = name;
        op.f0 = x; op.f1 = y; op.f2 = z; op.f3 = w;
    }

    @Override public void setUniformFloat(String name, float v) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_FLOAT);
        op.a = name;
        op.f0 = v;
    }

    @Override public void bindVertexBuffers(GraphicsBuffer vertex, GraphicsBuffer index) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_DRAW);
        op.a = vertex;
        op.b = index;
        // quad count carried via i1 set by recorder-based draw below
    }

    /** Records a full draw with quad count (used internally by the renderer). */
    void recordDrawQuads(GraphicsBuffer vertex, GraphicsBuffer index, int quadCount) {
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_DRAW);
        op.a = vertex;
        op.b = index;
        op.i1 = quadCount;
    }

    @Override public void drawQuads(int firstQuad, int quadCount) {
        // handled by recordDrawQuads from the renderer; generic path records count only
        GraphicsCommandRecorder.Op op = recorder.next(GraphicsCommandRecorder.OP_DRAW);
        op.i0 = firstQuad;
        op.i1 = quadCount;
    }

    @Override protected void onDestroy() {
        recorder = null;
    }
}
