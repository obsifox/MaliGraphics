package dev.mali.lab.scenes;

import java.util.ArrayList;
import java.util.List;

import dev.mali.graphics.api.GraphicsFrame;

import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.gles.GlesRendererImpl;
import dev.mali.graphics.gles.GlesTexture;

/**
 * Tests 08/09/10: text rendering, scrolling launcher list, animated launcher.
 * Reuses the exact card layout of the demo launcher (mission §14) so numbers
 * transfer to the real UI. Tests 11/12 metrics also flow through here.
 */
public final class LauncherListScene implements Scene {
    private final String label;
    private final boolean animated;
    private final boolean textHeavy;
    private final boolean cacheStress;

    private GlesTexture cardTex, iconTex;
    private int w = 1, h = 1;
    private float scroll = 0;
    private final List<String> versions = new ArrayList<>();

    public LauncherListScene(String label, boolean animated, boolean textHeavy, boolean cacheStress) {
        this.label = label;
        this.animated = animated;
        this.textHeavy = textHeavy;
        this.cacheStress = cacheStress;
    }

    public static LauncherListScene text() { return new LauncherListScene("Test 08 — Text Rendering", false, true, false); }
    public static LauncherListScene scrolling() { return new LauncherListScene("Test 09 — Scrolling Launcher", false, true, false); }
    public static LauncherListScene animated() { return new LauncherListScene("Test 10 — Animated Launcher", true, true, false); }
    public static LauncherListScene cacheStress() { return new LauncherListScene("Test 11 — Texture Cache Stress", true, true, true); }

    @Override public String name() { return label; }

    @Override public void init(GlesRendererImpl renderer, int width, int height) {
        w = width; h = height;
        cardTex = (GlesTexture) renderer.device().createTexture(32, 32, GraphicsTypes.TextureFormat.RGBA8,
                GraphicsTypes.FilterMode.LINEAR, GraphicsTypes.FilterMode.LINEAR,
                GraphicsTypes.WrapMode.CLAMP_TO_EDGE, "card");
        cardTex.uploadPixels(TestTextures.gradient(32), 32, 32, GraphicsTypes.TextureFormat.RGBA8);
        iconTex = (GlesTexture) renderer.device().createTexture(32, 32, GraphicsTypes.TextureFormat.RGBA8,
                GraphicsTypes.FilterMode.LINEAR, GraphicsTypes.FilterMode.LINEAR,
                GraphicsTypes.WrapMode.CLAMP_TO_EDGE, "icon");
        iconTex.uploadPixels(TestTextures.checker(32, 4, 255), 32, 32, GraphicsTypes.TextureFormat.RGBA8);
        versions.clear();
        for (int i = 1; i <= 30; i++) versions.add("Minecraft 1." + i + ".x — release " + i);
    }

    @Override public void resize(int width, int height) { w = width; h = height; }

    @Override public void draw(GlesRendererImpl renderer, GraphicsFrame frame, long timeMs) {
        renderer.clearColor(0.035f, 0.035f, 0.043f, 1f);
        scroll = (animated || cacheStress)
                ? (float) ((timeMs / 20) % (versions.size() * 140))
                : (float) ((timeMs / 300) % (versions.size() * 140));

        float cardW = w - 32f;
        float cardH = 120f;
        int visible = (int) (h / (cardH + 16)) + 2;
        int first = (int) (scroll / (cardH + 16));

        for (int i = first; i < Math.min(versions.size(), first + visible); i++) {
            float y = i * (cardH + 16) - scroll + 8;
            if (y + cardH < -cardH || y > h + cardH) continue;
            renderer.drawQuad(cardTex, 16, y, cardW, cardH, 0, 0, 1, 1,
                    0.13f, 0.14f, 0.15f, 0.95f, 0, 0, 0);
            renderer.drawQuad(iconTex, 32, y + 20, 44, 44, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0);
            if (textHeavy) {
                renderer.drawText(versions.get(i), 92, y + 34, 34, 1f, 1f, 1f, 1f);
                renderer.drawText("Last Play: 2026-10-0" + (1 + i % 9) + "   Play Time: " + (2 + i % 8) + "h",
                        92, y + 72, 24, 0.7f, 0.72f, 0.75f, 1f);
                renderer.drawText("PLAY →", cardW - 130, y + cardH / 2f - 8, 36,
                        0.06f, 0.73f, 0.49f, 1f);
            }
        }
        if (cacheStress) {
            // churn cache: rotate through many ids to exercise LRU + hit-rate (Test 11 gate)
            for (int k = 0; k < 8; k++) {
                long dummy = (timeMs / 16 + k) % 64;
                renderer.drawQuad(dummy == k ? iconTex : cardTex, 8 + k * 30, h - 40, 24, 24,
                        0, 0, 1, 1, 1, 1, 1, 0.8f, 0, 0, 0);
            }
        }
    }

    @Override public String statsLine() {
        return label + (cacheStress ? " | LRU churn" : "");
    }
}
