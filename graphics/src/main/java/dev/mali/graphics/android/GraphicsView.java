package dev.mali.graphics.android;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.mali.graphics.api.GraphicsContext;
import dev.mali.graphics.api.GraphicsDevice;
import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.api.GraphicsSurface;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.Graphics;

/**
 * Host view for the runtime (mission §5, §8): SurfaceView + dedicated render
 * thread + Choreographer pacing + full lifecycle (pause/resume/rotate/loss)
 * + escalation hook to the native fallback (§24).
 *
 * Host implements {@link Callback} and does its drawing inside
 * {@link Callback#onDrawFrame} via the renderer it created in {@link Callback#onRuntimeReady}.
 */
public final class GraphicsView extends SurfaceView implements SurfaceHolder.Callback {

    public interface Callback {
        /** Called on the render thread once the renderer is usable. Build your UI-draw closure here. */
        void onRuntimeReady(RenderHandle handle);
        /** Called on the render thread each frame before drawing. */
        void onDrawFrame(RenderHandle handle, GraphicsFrame frame);
        /** Called on the UI thread when the runtime failed hard — host must fall back (§24). */
        void onRuntimeFailed(Throwable cause);
    }

    /** Renderer facade handed to the host. */
    public interface RenderHandle {
        dev.mali.graphics.gles.GlesRendererImpl gles();
        void requestStatsOverlayUpdate(String line);
    }

    private final Callback hostCallback;
    private final AtomicBoolean runtimeFailed = new AtomicBoolean(false);
    private volatile RenderThread renderThread;
    private volatile boolean surfaceValid = false;
    private int pendingWidth, pendingHeight;

    public GraphicsView(Activity activity, Callback callback) {
        super(activity);
        this.hostCallback = callback;
        getHolder().addCallback(this);
        setFocusable(false);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        surfaceValid = true;
        if (renderThread == null || !renderThread.isAlive()) {
            renderThread = new RenderThread("mali-render");
            renderThread.start();
        }
        renderThread.postInit();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        pendingWidth = width;
        pendingHeight = height;
        if (renderThread != null) renderThread.postResize(width, height);
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceValid = false;
        if (renderThread != null) renderThread.postSurfaceLost();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (renderThread != null) renderThread.shutdown();
    }

    /** Marks the runtime as failed from anywhere (tests, watchdog). */
    public void failRuntime(Throwable t) {
        if (runtimeFailed.compareAndSet(false, true)) {
            hostCallback.onRuntimeFailed(t);
        }
    }

    public boolean isFailed() { return runtimeFailed.get(); }

    // ------------------------------------------------------------------ //

    private final class RenderThread extends HandlerThread implements Choreographer.FrameCallback, RenderHandle {
        private volatile boolean running = true;
        private volatile boolean initialized = false;
        private volatile boolean resizeRequested = false;

        private GraphicsDevice device;
        private GraphicsSurface surface;
        private GraphicsContext context;
        private dev.mali.graphics.gles.GlesRendererImpl renderer;
        private double refreshHz = 60.0;

        private Handler handler;

        RenderThread(String name) { super(name); }

        void postInit() {
            Handler h = handler;
            if (h != null) h.post(() -> { /* kick: init happens on first frame callback */ });
        }

        void shutdown() {
            running = false;
            try { Choreographer.getInstance().removeFrameCallback(this); } catch (Throwable ignored) {}
            quitSafely();
        }
        void postResize(int w, int h) { resizeRequested = true; pendingWidth = w; pendingHeight = h; }
        void postSurfaceLost() { surfaceValid = false; }

        @Override public dev.mali.graphics.gles.GlesRendererImpl gles() { return renderer; }

        @Override public void requestStatsOverlayUpdate(String line) { /* handled by lab activity */ }

        @Override protected void onLooperPrepared() {
            // HandlerThread guarantees a prepared Looper on THIS thread, so
            // Choreographer.getInstance() binds to the render thread (mission §8).
            handler = new Handler(getLooper());
            try {
                Choreographer.getInstance().postFrameCallback(this);
            } catch (Throwable t) {
                fail(t);
            }
        }

        private void fail(Throwable t) {
            running = false;
            runtimeFailed.set(true);
            try { Choreographer.getInstance().removeFrameCallback(this); } catch (Throwable ignored) {}
            quitSafely();
            ((Activity) getContext()).runOnUiThread(() -> hostCallback.onRuntimeFailed(t));
        }

        /** Tears down EGL/GL objects when the Android Surface goes away (pause/home/rotate). */
        private void teardown() {
            initialized = false;
            try { if (renderer != null) renderer.destroy(); } catch (Throwable ignored) {}
            try { if (context != null) context.destroy(); } catch (Throwable ignored) {}
            try { if (surface != null) surface.destroy(); } catch (Throwable ignored) {}
            try { if (device != null) device.destroy(); } catch (Throwable ignored) {}
            renderer = null; context = null; surface = null; device = null;
        }

        private void initializeIfNeeded() {
            if (initialized || !surfaceValid) return;
            try {
                Surface s = getHolder().getSurface();
                device = Graphics.createDevice(GraphicsTypes.Backend.AUTO);
                surface = device.createSurface(s, Math.max(1, getWidth()), Math.max(1, getHeight()), "main");
                context = device.createContext(surface);
                renderer = new dev.mali.graphics.gles.GlesRendererImpl(
                        (dev.mali.graphics.gles.GlesDevice) device,
                        (dev.mali.graphics.gles.GlesContext) context, "main-renderer");
                try {
                    Object disp = getDisplay();
                    if (disp instanceof android.view.Display) {
                        refreshHz = ((android.view.Display) disp).getRefreshRate();
                        renderer.scheduler().setDetectedRefreshHz(refreshHz);
                    }
                } catch (Throwable ignored) {}
                renderer.setCamera(getWidth(), getHeight());
                initialized = true;
                hostCallback.onRuntimeReady(this);
            } catch (Throwable t) {
                fail(t);
            }
        }

        @Override public void doFrame(long frameTimeNanos) {
            if (!running) return;
            try {
                if (initialized && !surfaceValid) teardown(); // surface lost -> rebuild on next create
                initializeIfNeeded();
                if (initialized && surfaceValid && renderer != null) {
                    if (resizeRequested) {
                        resizeRequested = false;
                        surface.resize(pendingWidth, pendingHeight);
                        renderer.setCamera(pendingWidth, pendingHeight);
                    }
                    GraphicsFrame frame = renderer.beginFrame();
                    hostCallback.onDrawFrame(this, frame);
                    renderer.endFrameAndPresent();
                }
                if (running) Choreographer.getInstance().postFrameCallback(this);
            } catch (Throwable t) {
                if (initialized && !surfaceValid) {
                    // In-flight frame raced with surface destruction — recoverable.
                    try { teardown(); } catch (Throwable ignored) {}
                    if (running) Choreographer.getInstance().postFrameCallback(this);
                } else {
                    fail(t);
                }
            }
        }
    }
}
