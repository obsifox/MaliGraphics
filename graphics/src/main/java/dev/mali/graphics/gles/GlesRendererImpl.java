package dev.mali.graphics.gles;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;

import dev.mali.graphics.api.GraphicsBuffer;
import dev.mali.graphics.api.GraphicsCapabilities;
import dev.mali.graphics.api.GraphicsDevice;
import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.api.GraphicsProfiler;
import dev.mali.graphics.api.GraphicsRenderer;
import dev.mali.graphics.api.GraphicsTexture;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.FrameScheduler;
import dev.mali.graphics.core.command.BuiltInShaders;
import dev.mali.graphics.core.command.GraphicsCommandRecorder;
import dev.mali.graphics.core.math.Mat4;
import dev.mali.graphics.core.profiler.ProfilerImpl;
import dev.mali.graphics.core.renderer.SpriteBatcher;

/**
 * GLES renderer implementation: batching renderer + command recording + present.
 * Implements the §13/§18/§19 contract. Render thread only.
 */
public final class GlesRendererImpl extends GraphicsRenderer {
    static final int MAX_QUADS = 2048;

    private final GlesDevice device;
    private final GlesContext context;
    private final GlesSurface surface;
    private final FrameScheduler scheduler = new FrameScheduler();
    private final ProfilerImpl profiler;
    private final SpriteBatcher batcher = new SpriteBatcher(MAX_QUADS);
    private final GlesCommandBuffer commands;

    private GlesBuffer vbo, ibo;
    private GlesPipeline spritePipeline;
    private float[] mvp = new float[16];
    private int surfaceW = 1, surfaceH = 1;
    private GraphicsFrame currentFrame;
    private final Deque<int[]> clipStack = new ArrayDeque<>();
    private long frameStartNs;

    public GlesRendererImpl(GlesDevice device, GlesContext context, String label) {
        super(label);
        this.device = device;
        this.context = context;
        this.surface = context.surface();
        this.profiler = new ProfilerImpl(scheduler);
        device.setProfiler(profiler);
        this.commands = (GlesCommandBuffer) context.createCommandBuffer("main");

        surface.makeCurrent();
        vbo = (GlesBuffer) device.createBuffer(GraphicsBuffer.Kind.VERTEX,
                MAX_QUADS * SpriteBatcher.VERTICES_PER_QUAD * SpriteBatcher.BYTES_PER_VERT, "sprite-vbo");
        ibo = (GlesBuffer) device.createBuffer(GraphicsBuffer.Kind.INDEX,
                MAX_QUADS * 6 * 2, "sprite-ibo");
        ByteBuffer idx = ByteBuffer.allocateDirect(MAX_QUADS * 6 * 2).order(ByteOrder.nativeOrder());
        idx.asShortBuffer().put(batcher.indices());
        idx.position(0);
        ibo.upload(idx, MAX_QUADS * 6 * 2, GraphicsTypes.BufferUsage.STATIC);

        spritePipeline = buildSpritePipeline();
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private GlesPipeline buildSpritePipeline() {
        GlesShader vs = (GlesShader) device.createShader(GraphicsTypes.ShaderStage.VERTEX,
                BuiltInShaders.vertexSource(context.capabilities().isGles3()), "sprite-vs");
        GlesShader fs = (GlesShader) device.createShader(GraphicsTypes.ShaderStage.FRAGMENT,
                BuiltInShaders.fragmentSource(context.capabilities().isGles3()), "sprite-fs");
        GlesPipeline p = (GlesPipeline) device.createPipeline(vs, fs, GraphicsTypes.BlendMode.ALPHA, "sprite");
        vs.release();
        fs.release();
        return p;
    }

    /** Runtime stress pipeline for Test 12 (shader stress). */
    public GlesPipeline buildStressPipeline() {
        GlesShader vs = (GlesShader) device.createShader(GraphicsTypes.ShaderStage.VERTEX,
                BuiltInShaders.vertexSource(context.capabilities().isGles3()), "stress-vs");
        GlesShader fs = (GlesShader) device.createShader(GraphicsTypes.ShaderStage.FRAGMENT,
                BuiltInShaders.stressFragmentSource(context.capabilities().isGles3()), "stress-fs");
        GlesPipeline p = (GlesPipeline) device.createPipeline(vs, fs, GraphicsTypes.BlendMode.ALPHA, "stress");
        vs.release();
        fs.release();
        return p;
    }

    public SpriteBatcher batcher() { return batcher; }
    public GlesPipeline spritePipeline() { return spritePipeline; }
    public FrameScheduler scheduler() { return scheduler; }
    public ProfilerImpl profilerImpl() { return profiler; }

    @Override public GraphicsFrame beginFrame() {
        frameStartNs = System.nanoTime();
        currentFrame = scheduler.begin(frameStartNs);
        profiler.onFrameBegin(currentFrame);
        commands.reset();
        batcher.reset();
        surface.makeCurrent();
        return currentFrame;
    }

    @Override public void setCamera(int width, int height) {
        this.surfaceW = Math.max(1, width);
        this.surfaceH = Math.max(1, height);
        Mat4 m = Mat4.orthoPixels(surfaceW, surfaceH);
        System.arraycopy(m.m, 0, mvp, 0, 16);
    }

    @Override public void clearColor(float r, float g, float b, float a) {
        GraphicsCommandRecorder.Op op = commands.recorder().next(GraphicsCommandRecorder.OP_CLEAR_COLOR);
        op.f0 = r; op.f1 = g; op.f2 = b; op.f3 = a;
    }

    @Override public void pushClip(int x, int y, int w, int h) {
        clipStack.addLast(new int[]{x, y, w, h});
        GraphicsCommandRecorder.Op op = commands.recorder().next(GraphicsCommandRecorder.OP_SCISSOR);
        op.i0 = x; op.i1 = y; op.i2 = w; op.i3 = h;
        flushBatch();
    }

    @Override public void popClip() {
        if (!clipStack.isEmpty()) clipStack.removeLast();
        commands.recorder().next(GraphicsCommandRecorder.OP_CLEAR_SCISSOR);
        // re-apply parent clip if any
        if (!clipStack.isEmpty()) {
            int[] c = clipStack.peekLast();
            GraphicsCommandRecorder.Op op = commands.recorder().next(GraphicsCommandRecorder.OP_SCISSOR);
            op.i0 = c[0]; op.i1 = c[1]; op.i2 = c[2]; op.i3 = c[3];
        }
    }

    @Override public void drawQuad(GraphicsTexture texture,
                                   float x, float y, float w, float h,
                                   float u0, float v0, float u1, float v1,
                                   float r, float g, float b, float a,
                                   float rotationDeg, float pivotX, float pivotY) {
        int texId = texture != null ? System.identityHashCode(texture) : 0;
        if (batcher.quadCount() > 0 && batcher.batchTextureId() != texId) {
            flushBatch();
        }
        if (batcher.isFull()) flushBatch();
        batcher.pushQuad(x, y, w, h, u0, v0, u1, v1, r, g, b, a, rotationDeg, pivotX, pivotY, texId);
        if (batcher.isFull()) flushBatch();
    }

    @Override public void drawText(String text, float x, float y, float sizePx,
                                   float r, float g, float b, float a) {
        if (fontRenderer != null) fontRenderer.draw(this, text, x, y, sizePx, r, g, b, a);
    }

    /** Plugged by the host after building the glyph atlas (android/FontAtlasFactory). */
    public interface FontRenderer {
        void draw(GlesRendererImpl renderer, String text, float x, float y, float sizePx,
                  float r, float g, float b, float a);
    }
    private FontRenderer fontRenderer;
    public void setFontRenderer(FontRenderer fr) { this.fontRenderer = fr; }

    /** Submits the current batch as one draw. */
    void flushBatch() {
        if (batcher.isEmpty()) return;
        final int quads = batcher.quadCount();
        ByteBuffer verts = batcher.pendingVertices();
        vbo.upload(verts, batcher.pendingBytes(), GraphicsTypes.BufferUsage.DYNAMIC);

        GraphicsCommandRecorder.Recorder rec = commands.recorder();
        if (spritePipeline != null && spritePipeline.isValid()) {
            GraphicsCommandRecorder.Op op = rec.next(GraphicsCommandRecorder.OP_PIPELINE);
            op.a = spritePipeline;
            op = rec.next(GraphicsCommandRecorder.OP_BLEND);
            op.i0 = GraphicsTypes.BlendMode.ALPHA.ordinal();
            op.i1 = Gles.blendSrc(GraphicsTypes.BlendMode.ALPHA);
            op = rec.next(GraphicsCommandRecorder.OP_MATRIX);
            op.a = "uMVP";
            op.b = mvp;
            op = rec.next(GraphicsCommandRecorder.OP_DRAW);
            op.a = vbo;
            op.b = ibo;
            op.i1 = quads;
        }
        batcher.reset();
    }

    @Override public void endFrameAndPresent() {
        flushBatch();
        // viewport per frame (surface may have resized)
        GraphicsCommandRecorder.Op op = commands.recorder().next(GraphicsCommandRecorder.OP_VIEWPORT);
        op.i0 = 0; op.i1 = 0; op.i2 = surfaceW; op.i3 = surfaceH;
        // execute: viewport first — simplest correct ordering: rebuild list with viewport first
        // (recording order: we re-execute with a dedicated pre-pass)
        executeWithViewportFirst();

        final long t0 = System.nanoTime();
        final boolean ok = surface.swap();
        final long presentNs = System.nanoTime() - t0;
        if (!ok && device.egl.isContextLost()) {
            context.markLost();
        }
        if (currentFrame != null) {
            currentFrame.cpuSubmitNs = frameStartNs >= 0 ? cpuMeasure() : 0;
            currentFrame.presentNs = presentNs;
            currentFrame.endNs = System.nanoTime();
            profiler.onFrameEnd(currentFrame);
        }
    }

    private long cpuMeasure() {
        // CPU submit time approximated by time between begin and pre-present (executed in endFrame)
        return preExecuteNs;
    }

    private long preExecuteNs;

    private void executeWithViewportFirst() {
        final long t0 = System.nanoTime();
        // Prepend viewport by direct call (recorded list then runs after)
        GLES20.glViewport(0, 0, surfaceW, surfaceH);
        GlesCommandExecutor.execute(commands.recorder().ops(), commands.recorder().count(), profiler);
        preExecuteNs = System.nanoTime() - t0;
        commands.recorder().reset();
    }

    @Override public GraphicsProfiler profiler() { return profiler; }
    @Override public GraphicsCapabilities capabilities() { return context.capabilities(); }
    @Override public GraphicsDevice device() { return device; }

    public GlesContext context() { return context; }

    @Override protected void onDestroy() {
        if (vbo != null) vbo.destroy();
        if (ibo != null) ibo.destroy();
        if (spritePipeline != null) spritePipeline.destroy();
    }
}
