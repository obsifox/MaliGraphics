package dev.mali.graphics.gles;

import android.opengl.EGLSurface;
import android.view.Surface;

import dev.mali.graphics.api.GraphicsSurface;

/** EGL window surface wrapper. */
public final class GlesSurface extends GraphicsSurface {
    private final GlesDevice device;
    private final Surface androidSurface;
    private EGLSurface eglSurface;
    private int width, height;

    GlesSurface(GlesDevice device, Surface surface, int width, int height, String label) {
        super(label);
        this.device = device;
        this.androidSurface = surface;
        this.width = width;
        this.height = height;
        this.eglSurface = device.egl.createWindowSurface(surface);
    }

    EGLSurface eglSurface() { return eglSurface; }

    public void makeCurrent() {
        device.ensureEgl();
        device.egl.makeCurrent(eglSurface);
    }

    boolean swap() {
        return device.egl.swapBuffers(eglSurface);
    }

    @Override public Surface surface() { return androidSurface; }
    @Override public int width() { return width; }
    @Override public int height() { return height; }

    @Override public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        // EGL window surfaces follow the BufferQueue automatically; viewport is set per frame. CONFIRMED
    }

    @Override protected void onDestroy() {
        device.egl.makeNotCurrent();
        device.egl.destroySurface(eglSurface);
        eglSurface = null;
    }
}
