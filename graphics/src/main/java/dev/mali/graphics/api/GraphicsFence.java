package dev.mali.graphics.api;

/** GPU completion fence (ES3 glFenceSync when available; else no-op). */
public abstract class GraphicsFence extends GraphicsResource {
    protected GraphicsFence(String label) { super(label); }

    /** true once the GPU passed this point. Never blocks the frame loop. */
    public abstract boolean isSignaled();

    /** Optional bounded wait (ms) — tests/benchmarks only. */
    public abstract boolean waitSignaled(long timeoutMs);
}
