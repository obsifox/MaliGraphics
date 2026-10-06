package dev.mali.graphics.api;

/**
 * Profiler interface (mission §20). Implementations feed FrameScheduler stats
 * and the benchmark report writer. All methods must be cheap (no I/O in frame loop).
 */
public interface GraphicsProfiler {
    void onFrameBegin(GraphicsFrame frame);
    void onDrawCall(int vertices);
    void onTextureBind();
    void onPipelineBind();
    void onTextureUpload(long bytes, long timeNs);
    void onShaderCompile(String key, long timeMs);
    void onFrameEnd(GraphicsFrame frame);

    /** Snapshot for the current benchmark window (also resets counters). */
    Snapshot takeSnapshot();

    final class Snapshot {
        public int frames;
        public double avgFps;
        public double avgFrameMs;
        public double avgCpuMs;
        public double avgPresentMs;
        public long droppedFrames;
        public long drawCalls;
        public long vertices;
        public long textureBinds;
        public long pipelineBinds;
        public long textureUploadBytes;
        public double textureUploadMs;
        public long shaderCompiles;
        public double shaderCompileMs;
        public double cacheHitRate; // 0..1 (from ResourceCache, set by runtime)
        public int liveResources;
    }
}
