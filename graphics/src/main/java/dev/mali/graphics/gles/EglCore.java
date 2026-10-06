package dev.mali.graphics.gles;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;

/**
 * EGL14 wrapper (docs/android-graphics.md): one display, one config, one context,
 * N window surfaces. ES3 requested with ES2 fallback (Utgard, mission §11).
 * All methods must be called from the render thread.
 */
final class EglCore {
    private EGLDisplay display = EGL14.EGL_NO_DISPLAY;
    private EGLConfig config;
    private EGLContext context = EGL14.EGL_NO_CONTEXT;
    public int glMajor = 2, glMinor = 0;

    void init() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (display == EGL14.EGL_NO_DISPLAY) throw new IllegalStateException("eglGetDisplay failed");
        int[] major = new int[1], minor = new int[1];
        if (!EGL14.eglInitialize(display, major, 0, minor, 0)) {
            throw new IllegalStateException("eglInitialize failed: 0x"
                    + Integer.toHexString(EGL14.eglGetError()));
        }
        glMajor = major[0]; glMinor = minor[0];

        config = chooseConfig();
        if (config == null) throw new IllegalStateException("no EGL config");

        // try ES3, fall back to ES2 (Utgard)
        int[] attrs3 = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE};
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attrs3, 0);
        if (context == EGL14.EGL_NO_CONTEXT) {
            int[] attrs2 = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attrs2, 0);
            glMajor = 2; glMinor = 0;
        }
        if (context == EGL14.EGL_NO_CONTEXT) {
            throw new IllegalStateException("eglCreateContext failed: 0x"
                    + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    private EGLConfig chooseConfig() {
        int[] attrs = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,          // 2D UI: no depth buffer → saves bandwidth (LIKELY)
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_RENDERABLE_TYPE, 0x44,  // ES3 bit | ES2 bit
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num = new int[1];
        if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, num, 0) || num[0] == 0) {
            // strict ES3 renderable failed at config level → retry ES2-only
            int[] attrs2 = attrs.clone();
            attrs2[12] = 0x4; // EGL_OPENGL_ES2_BIT only
            if (!EGL14.eglChooseConfig(display, attrs2, 0, configs, 0, 1, num, 0) || num[0] == 0) {
                return null;
            }
        }
        return configs[0];
    }

    EGLSurface createWindowSurface(Surface surface) {
        int[] attrs = {EGL14.EGL_NONE};
        EGLSurface s = EGL14.eglCreateWindowSurface(display, config, surface, attrs, 0);
        if (s == null || s == EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("eglCreateWindowSurface failed: 0x"
                    + Integer.toHexString(EGL14.eglGetError()));
        }
        return s;
    }

    void makeCurrent(EGLSurface surface) {
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            throw new IllegalStateException("eglMakeCurrent failed: 0x"
                    + Integer.toHexString(EGL14.eglGetError()));
        }
    }

    void makeNotCurrent() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
    }

    boolean swapBuffers(EGLSurface surface) {
        return EGL14.eglSwapBuffers(display, surface);
    }

    void destroySurface(EGLSurface surface) {
        if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, surface);
        }
    }

    /** true when the driver reported a context loss (android-graphics.md lifecycle). */
    boolean isContextLost() {
        int err = EGL14.eglGetError();
        return err == EGL_CONTEXT_LOST_EGL;
    }

    void release() {
        if (context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context);
            context = EGL14.EGL_NO_CONTEXT;
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display);
            display = EGL14.EGL_NO_DISPLAY;
        }
    }

    // EGL_CONTEXT_LOST is 0x300E — keep a literal to avoid pulling EGL11+ symbols
    private static final int EGL_CONTEXT_LOST_EGL = 0x300E;
}
