package dev.mali.graphics.api;

/**
 * One presentation frame's timing record (§19). Produced by FrameScheduler.
 * All values are nanoseconds unless stated.
 */
public final class GraphicsFrame {
    public int frameIndex;
    public long beginNs;
    public long endNs;
    public long cpuSubmitNs;   // time spent recording+executing commands
    public long presentNs;     // eglSwapBuffers duration
    public long frameIntervalNs; // vsync interval detected at frame start
    public int drawCalls;
    public int vertices;
    public int textureBinds;
    public boolean missedVsync; // frame exceeded its interval

    public double fps() {
        long dur = endNs - beginNs;
        return dur > 0 ? 1_000_000_000.0 / dur : 0.0;
    }
}
