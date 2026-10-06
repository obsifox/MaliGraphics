package dev.mali.graphics;

import android.view.Surface;
import dev.mali.graphics.api.GraphicsDevice;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.gles.GlesDevice;

/**
 * Entry point (mission §9). Auto-selects backend from runtime capabilities (§24).
 *
 * <pre>{@code
 *   GraphicsDevice device = Graphics.createDevice(GraphicsTypes.Backend.AUTO);
 *   GraphicsSurface surface = device.createSurface(holder, "main");
 *   GraphicsContext ctx = device.createContext(surface);
 *   GraphicsRenderer renderer = ... // host wires ctx + batcher via GlesRendererImpl helper
 * }</pre>
 */
public final class Graphics {
    private Graphics() {}

    public static GraphicsDevice createDevice(GraphicsTypes.Backend preferred) {
        GraphicsTypes.Backend choice = (preferred == GraphicsTypes.Backend.AUTO)
                ? GraphicsTypes.Backend.GLES     // Stage 1 validated backend (mission §11)
                : preferred;
        switch (choice) {
            case GLES:
            case ANDROID_NATIVE:      // native fallback does not need a device; reuse GLES for lab overlays
                return new GlesDevice();
            case VULKAN:
                throw new UnsupportedOperationException(
                        "Vulkan backend is Stage 5 (mission §12, §27). Use AUTO/GLES.");
            default:
                return new GlesDevice();
        }
    }

    /** Convenience when the caller already holds an android.view.Surface. */
    public static GraphicsDevice createDeviceForSurface(Surface ignoredSurface) {
        return createDevice(GraphicsTypes.Backend.AUTO);
    }
}
