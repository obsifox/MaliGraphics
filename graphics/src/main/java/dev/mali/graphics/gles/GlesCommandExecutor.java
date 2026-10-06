package dev.mali.graphics.gles;

import android.opengl.GLES20;

import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.command.GraphicsCommandRecorder;
import dev.mali.graphics.core.profiler.ProfilerImpl;

/**
 * Interprets the recorded command list into GLES calls (docs/command-submission.md).
 * The list is a flat preallocated op array owned by the renderer — zero GC.
 */
final class GlesCommandExecutor {

    private GlesCommandExecutor() {}

    // ---- op codes (mirrored by GlesRendererImpl / GlesCommandBuffer recording) ----








    static final int OP_VEC4 = 8;



    static void execute(GraphicsCommandRecorder.Op[] ops, int count, ProfilerImpl profiler) {
        GlesPipeline currentPipeline = null;
        for (int i = 0; i < count; i++) {
            GraphicsCommandRecorder.Op op = ops[i];
            switch (op.code) {
                case GraphicsCommandRecorder.OP_VIEWPORT:
                    GLES20.glViewport(op.i0, op.i1, op.i2, op.i3);
                    break;
                case GraphicsCommandRecorder.OP_SCISSOR:
                    GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                    GLES20.glScissor(op.i0, op.i1, op.i2, op.i3);
                    break;
                case GraphicsCommandRecorder.OP_CLEAR_SCISSOR:
                    GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
                    break;
                case GraphicsCommandRecorder.OP_CLEAR_COLOR:
                    GLES20.glClearColor(op.f0, op.f1, op.f2, op.f3);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    break;
                case GraphicsCommandRecorder.OP_BLEND:
                    if (op.i0 == GraphicsTypes.BlendMode.OFF.ordinal()) {
                        GLES20.glDisable(GLES20.GL_BLEND);
                    } else {
                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFuncSeparate(op.i1, GLES20.GL_ONE_MINUS_SRC_ALPHA,
                                GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    }
                    break;
                case GraphicsCommandRecorder.OP_PIPELINE:
                    currentPipeline = (GlesPipeline) op.a;
                    if (currentPipeline != null && currentPipeline.isValid()) {
                        currentPipeline.use();
                        if (profiler != null) profiler.onPipelineBind();
                    }
                    break;
                case GraphicsCommandRecorder.OP_TEXTURE:
                    dev.mali.graphics.api.GraphicsTexture tex = (dev.mali.graphics.api.GraphicsTexture) op.a;
                    if (tex instanceof GlesTexture) {
                        ((GlesTexture) tex).bind(op.i0);
                    } else {
                        GlesTexture.unbind(op.i0);
                    }
                    if (currentPipeline != null && currentPipeline.isValid()) {
                        GLES20.glUniform1i(currentPipeline.uniformLocation("uUseTex"), tex != null ? 1 : 0);
                        GLES20.glUniform1i(currentPipeline.uniformLocation("uTex"), 0);
                    }
                    if (profiler != null) profiler.onTextureBind();
                    break;
                case GraphicsCommandRecorder.OP_MATRIX:
                    if (currentPipeline != null && currentPipeline.isValid()) {
                        int loc = currentPipeline.uniformLocation((String) op.a);
                        float[] m = (float[]) op.b;
                        GLES20.glUniformMatrix4fv(loc, 1, false, m, op.i0);
                    }
                    break;
                case GraphicsCommandRecorder.OP_VEC4:
                    if (currentPipeline != null && currentPipeline.isValid()) {
                        int loc = currentPipeline.uniformLocation((String) op.a);
                        GLES20.glUniform4f(loc, op.f0, op.f1, op.f2, op.f3);
                    }
                    break;
                case GraphicsCommandRecorder.OP_FLOAT:
                    if (currentPipeline != null && currentPipeline.isValid()) {
                        int loc = currentPipeline.uniformLocation((String) op.a);
                        GLES20.glUniform1f(loc, op.f0);
                    }
                    break;
                case GraphicsCommandRecorder.OP_DRAW:
                    GlesBuffer vbo = (GlesBuffer) op.a;
                    GlesBuffer ibo = (GlesBuffer) op.b;
                    if (vbo != null && ibo != null && currentPipeline != null && currentPipeline.isValid()) {
                        vbo.bind();
                        final int stride = 32; // 8 floats: pos2 uv2 rgba4
                        int aPos = currentPipeline.attribLocation("aPos");
                        int aUV = currentPipeline.attribLocation("aUV");
                        int aColor = currentPipeline.attribLocation("aColor");
                        GLES20.glEnableVertexAttribArray(aPos);
                        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, stride, 0);
                        GLES20.glEnableVertexAttribArray(aUV);
                        GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, stride, 8);
                        GLES20.glEnableVertexAttribArray(aColor);
                        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, 16);
                        ibo.bind();
                        GLES20.glDrawElements(GLES20.GL_TRIANGLES, op.i1 * 6, GLES20.GL_UNSIGNED_SHORT, 0);
                        GLES20.glDisableVertexAttribArray(aPos);
                        GLES20.glDisableVertexAttribArray(aUV);
                        GLES20.glDisableVertexAttribArray(aColor);
                        ibo.unbind();
                        vbo.unbind();
                        if (profiler != null) profiler.onDrawCall(op.i1 * 4);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
