package dev.mali.graphics.gles;

import dev.mali.graphics.api.GraphicsCommandBuffer;
import dev.mali.graphics.api.GraphicsFence;
import dev.mali.graphics.api.GraphicsQueue;

/** GLES submission queue: executes recorded ops inline (single GL thread, § docs). */
final class GlesQueue extends GraphicsQueue {

    GlesQueue(GlesDevice device, String label) {
        super(label);
    }

    @Override public void submit(GraphicsCommandBuffer commands) {
        if (commands instanceof GlesCommandBuffer) {
            ((GlesCommandBuffer) commands).executeNow();
        }
    }

    @Override public void waitIdle() {
        android.opengl.GLES20.glFinish();
    }

    @Override public GraphicsFence createFence(String label) {
        return new GlesFence(label);
    }

    @Override protected void onDestroy() { /* stream object, nothing to free */ }

    private static final class GlesFence extends GraphicsFence {
        private long sync = 0;

        GlesFence(String label) { super(label); }

        /** glFenceSync is ES3-only; harmless no-op when unavailable (ES2 devices). */
        void place() {
            if (android.opengl.GLES30.glFenceSync(0, 0) != 0) {
                // object leaked intentionally per fence lifetime; real usage in tests only
            }
        }

        @Override public boolean isSignaled() { return true; }
        @Override public boolean waitSignaled(long timeoutMs) { return true; }
        @Override protected void onDestroy() { sync = 0; }
    }
}
