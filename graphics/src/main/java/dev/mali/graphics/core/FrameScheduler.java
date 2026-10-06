package dev.mali.graphics.core;

import dev.mali.graphics.api.GraphicsFrame;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Frame scheduler + pacing policy (mission §19, §20).
 * Pure Java — unit tested. The Android view feeds it vsync timestamps via Choreographer;
 * the scheduler only measures/advises (never forces display modes).
 */
public final class FrameScheduler {
    private static final int WINDOW = 120;                 // rolling frames kept
    private final Deque<GraphicsFrame> history = new ArrayDeque<>(WINDOW);
    private final long[] cpuSamples = new long[WINDOW];
    private final long[] presentSamples = new long[WINDOW];
    private int sampleIdx;
    private int frameIndex;
    private double refreshHz = 60.0;
    private long droppedFrames;
    private long totalFrames;

    public void setDetectedRefreshHz(double hz) {
        if (hz >= 30 && hz <= 240) this.refreshHz = hz;
    }

    public double detectedRefreshHz() { return refreshHz; }

    public GraphicsFrame begin(long nowNs) {
        GraphicsFrame f = new GraphicsFrame();
        f.frameIndex = frameIndex++;
        f.beginNs = nowNs;
        f.frameIntervalNs = (long) (1_000_000_000.0 / refreshHz);
        totalFrames++;
        return f;
    }

    public void end(GraphicsFrame f, long nowNs) {
        f.endNs = nowNs;
        long dur = f.endNs - f.beginNs;
        f.missedVsync = f.frameIntervalNs > 0 && dur > f.frameIntervalNs + f.frameIntervalNs / 4;
        if (f.missedVsync) droppedFrames++;
        cpuSamples[sampleIdx] = f.cpuSubmitNs;
        presentSamples[sampleIdx] = f.presentNs;
        sampleIdx = (sampleIdx + 1) % WINDOW;
        history.addLast(f);
        if (history.size() > WINDOW) history.removeFirst();
    }

    public long droppedFrames() { return droppedFrames; }
    public long totalFrames() { return totalFrames; }

    /** Average FPS across the rolling window. */
    public double avgFps() {
        if (history.isEmpty()) return 0;
        long first = history.peekFirst().beginNs;
        long last = history.peekLast().endNs;
        if (last <= first) return 0;
        return (history.size() - 1) * 1_000_000_000.0 / (last - first);
    }

    public double avgFrameMs() {
        if (history.isEmpty()) return 0;
        double sum = 0;
        for (GraphicsFrame f : history) sum += (f.endNs - f.beginNs) / 1e6;
        return sum / history.size();
    }

    public double avgCpuMs() {
        double sum = 0; int n = 0;
        for (int i = 0; i < WINDOW; i++) { if (cpuSamples[i] > 0) { sum += cpuSamples[i] / 1e6; n++; } }
        return n == 0 ? 0 : sum / n;
    }

    public double avgPresentMs() {
        double sum = 0; int n = 0;
        for (int i = 0; i < WINDOW; i++) { if (presentSamples[i] > 0) { sum += presentSamples[i] / 1e6; n++; } }
        return n == 0 ? 0 : sum / n;
    }
}
