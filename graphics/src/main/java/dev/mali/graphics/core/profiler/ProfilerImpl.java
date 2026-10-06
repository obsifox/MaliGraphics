package dev.mali.graphics.core.profiler;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.api.GraphicsProfiler;
import dev.mali.graphics.core.FrameScheduler;

/** Default profiler: counters + rolling averages, zero allocation after construction (§20, §29). */
public final class ProfilerImpl implements GraphicsProfiler {
    private final FrameScheduler scheduler;
    private long drawCalls, vertices, textureBinds, pipelineBinds;
    private long uploadBytes; private double uploadMs;
    private long shaderCompiles; private double compileMs;

    public ProfilerImpl(FrameScheduler scheduler) { this.scheduler = scheduler; }

    @Override public void onFrameBegin(GraphicsFrame frame) { /* scheduler owns timing */ }
    @Override public void onDrawCall(int vertices) { drawCalls++; this.vertices += vertices; }
    @Override public void onTextureBind() { textureBinds++; }
    @Override public void onPipelineBind() { pipelineBinds++; }
    @Override public void onTextureUpload(long bytes, long timeNs) {
        uploadBytes += bytes; uploadMs += timeNs / 1e6;
    }
    @Override public void onShaderCompile(String key, long timeMs) {
        shaderCompiles++; compileMs += timeMs;
    }
    @Override public void onFrameEnd(GraphicsFrame frame) {
        scheduler.end(frame, frame.endNs);
    }

    @Override public Snapshot takeSnapshot() {
        Snapshot s = new Snapshot();
        s.avgFps = scheduler.avgFps();
        s.avgFrameMs = scheduler.avgFrameMs();
        s.avgCpuMs = scheduler.avgCpuMs();
        s.avgPresentMs = scheduler.avgPresentMs();
        s.droppedFrames = scheduler.droppedFrames();
        s.drawCalls = drawCalls;
        s.vertices = vertices;
        s.textureBinds = textureBinds;
        s.pipelineBinds = pipelineBinds;
        s.textureUploadBytes = uploadBytes;
        s.textureUploadMs = uploadMs;
        s.shaderCompiles = shaderCompiles;
        s.shaderCompileMs = compileMs;
        return s;
    }

    public void resetCounters() {
        drawCalls = vertices = textureBinds = pipelineBinds = 0;
        uploadBytes = 0; uploadMs = 0; shaderCompiles = 0; compileMs = 0;
    }
}
