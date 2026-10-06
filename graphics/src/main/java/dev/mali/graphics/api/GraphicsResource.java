package dev.mali.graphics.api;

/** Base for all GPU resource handles (mission §15: reference tracking + explicit destruction). */
public abstract class GraphicsResource {
    private final String label;
    private int refCount = 1;          // runtime holds one implicit reference
    private boolean destroyed = false;

    protected GraphicsResource(String label) {
        this.label = label;
    }

    public final String label() { return label; }
    public final boolean isDestroyed() { return destroyed; }
    public final int refCount() { return refCount; }

    public final void addRef() {
        if (destroyed) throw new IllegalStateException("addRef on destroyed resource: " + label);
        refCount++;
    }

    /** Drops one reference; destroys when it reaches zero. */
    public final void release() {
        if (destroyed) return;
        if (--refCount <= 0) {
            destroyed = true;
            onDestroy();
        }
    }

    /** Forces destruction regardless of refcount (cache eviction). */
    public final void destroy() {
        if (destroyed) return;
        destroyed = true;
        onDestroy();
    }

    /** Backend teardown — called exactly once, on the render thread. */
    protected abstract void onDestroy();
}
