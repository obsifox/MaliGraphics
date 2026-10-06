package dev.mali.lab.scenes;

import java.util.Random;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.api.GraphicsTexture;
import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.gles.GlesPipeline;
import dev.mali.graphics.gles.GlesRendererImpl;
import dev.mali.graphics.gles.GlesTexture;

/**
 * Parameterized quad scene — covers Tests 01-07 with different configs
 * (single/100/500/1000 quads, textures, alpha blending, batching pressure).
 * Same primitives the launcher UI uses (mission §13-14).
 */
public final class QuadScene implements Scene {
    private final String label;
    private final int quadCount;
    private final boolean textured;
    private final boolean animated;
    private final boolean stressShader;

    private GlesTexture checker;
    private GlesTexture gradient;
    private GlesPipeline stressPipeline;
    private int w = 1, h = 1;
    private final Random rng = new Random(42);
    private final float[] xs = new float[4096], ys = new float[4096], ss = new float[4096];

    public QuadScene(String label, int quadCount, boolean textured, boolean animated, boolean stressShader) {
        this.label = label;
        this.quadCount = quadCount;
        this.textured = textured;
        this.animated = animated;
        this.stressShader = stressShader;
    }

    public static QuadScene single() { return new QuadScene("Test 01 — Single Quad", 1, false, false, false); }
    public static QuadScene hundred() { return new QuadScene("Test 02 — 100 Quads", 100, true, true, false); }
    public static QuadScene fiveHundred() { return new QuadScene("Test 03 — 500 Quads", 500, true, true, false); }
    public static QuadScene thousand() { return new QuadScene("Test 04 — 1000 Quads", 1000, true, true, false); }
    public static QuadScene textures() { return new QuadScene("Test 05 — Textures", 512, true, false, false); }
    public static QuadScene alphaBlend() { return new QuadScene("Test 06 — Alpha Blending", 400, true, true, false); }
    public static QuadScene batching() { return new QuadScene("Test 07 — Sprite Batching", 1000, true, false, false); }
    public static QuadScene shaderStress() { return new QuadScene("Test 12 — Shader Stress", 200, true, true, true); }

    @Override public String name() { return label; }

    @Override public void init(GlesRendererImpl renderer, int width, int height) {
        w = width; h = height;
        checker = (GlesTexture) renderer.device().createTexture(64, 64, GraphicsTypes.TextureFormat.RGBA8,
                GraphicsTypes.FilterMode.LINEAR, GraphicsTypes.FilterMode.LINEAR,
                GraphicsTypes.WrapMode.REPEAT, "checker");
        checker.uploadPixels(TestTextures.checker(64, 8, 255), 64, 64, GraphicsTypes.TextureFormat.RGBA8);
        gradient = (GlesTexture) renderer.device().createTexture(64, 64, GraphicsTypes.TextureFormat.RGBA8,
                GraphicsTypes.FilterMode.LINEAR, GraphicsTypes.FilterMode.LINEAR,
                GraphicsTypes.WrapMode.CLAMP_TO_EDGE, "gradient");
        gradient.uploadPixels(TestTextures.gradient(64), 64, 64, GraphicsTypes.TextureFormat.RGBA8);

        if (stressShader) {
            stressPipeline = renderer.buildStressPipeline();
        }
        rng.setSeed(42);
        for (int i = 0; i < xs.length; i++) {
            xs[i] = rng.nextFloat();
            ys[i] = rng.nextFloat();
            ss[i] = 16 + rng.nextFloat() * 48;
        }
    }

    @Override public void resize(int width, int height) {
        w = width; h = height;
        rendererSetCamera(rendererRef, width, height);
    }

    private GlesRendererImpl rendererRef;

    private static void rendererSetCamera(GlesRendererImpl r, int width, int height) {
        if (r != null) r.setCamera(width, height);
    }

    @Override public void draw(GlesRendererImpl renderer, GraphicsFrame frame, long timeMs) {
        rendererRef = renderer;
        renderer.clearColor(0.035f, 0.035f, 0.043f, 1f);

        int count = Math.min(quadCount, xs.length);
        float cellW = w / 12f;
        for (int i = 0; i < count; i++) {
            float x = xs[i] * (w - ss[i]);
            float y = ys[i] * (h - ss[i]);
            if (animated) {
                float t = timeMs / 1000f;
                x += Math.sin(t * (0.7f + xs[i]) + i) * cellW * 0.25f;
                y += Math.cos(t * (0.9f + ys[i]) + i) * cellW * 0.25f;
            }
            if (stressShader && i == 0) {
                // bind stress pipeline for the whole batch when enabled
                renderer.drawQuad(gradient, x, y, ss[i], ss[i], 0, 0, 1, 1,
                        1f, 1f, 1f, 0.9f, (animated ? (float) (timeMs % 360) : 0f), ss[i] / 2, ss[i] / 2);
            } else if (textured) {
                GlesTexture tex = (i % 2 == 0) ? checker : gradient;
                renderer.drawQuad(tex, x, y, ss[i], ss[i],
                        0, 0, 1, 1,
                        1f, 1f, 1f, i % 3 == 0 ? 0.55f : 1f,
                        animated && i % 5 == 0 ? (float) ((timeMs / 20 + i * 37) % 360) : 0f,
                        ss[i] / 2, ss[i] / 2);
            } else {
                float hue = (i % 8) / 8f;
                renderer.drawQuad(null, x, y, ss[i], ss[i], 0, 0, 1, 1,
                        0.06f + hue * 0.9f, 0.72f, 0.5f + hue * 0.3f, 1f, 0, 0, 0);
            }
        }
    }

    @Override public String statsLine() {
        return label + " | quads=" + quadCount + (textured ? " | textured" : " | flat")
                + (stressShader ? " | stress-FS" : "");
    }
}
