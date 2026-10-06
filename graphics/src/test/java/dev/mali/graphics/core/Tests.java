package dev.mali.graphics.core;

import dev.mali.graphics.core.renderer.SpriteBatcher;
import dev.mali.graphics.core.math.Mat4;

/** Plain-assert tests runnable both via JUnit and SelfCheck (no dependencies). */
public final class Tests {
    private Tests() {}

    public static void runAll() {
        mat4Tests();
        batcherTests();
        cacheTests();
        schedulerTests();
        maliParserTests();
        System.out.println("[Tests] ALL CORE TESTS PASSED");
    }

    // ---------- math ----------
    static void mat4Tests() {
        check(Mat4.identity().m[15] == 1f && Mat4.identity().m[0] == 1f, "identity");
        Mat4 o = Mat4.orthoPixels(1000, 500);
        check(o.m[0] > 0 && o.m[5] < 0, "orthoPixels: x right, y down");
        check(Math.abs(o.m[0] - 2f / 1000) < 1e-6, "orthoPixels scale x");
        Mat4 t = Mat4.trs(100, 200, 2, 2, 0, 0, 0);
        check(Math.abs(t.m[12] - 100) < 1e-5 && Math.abs(t.m[13] - 200) < 1e-5, "trs translate");
        Mat4 r = Mat4.trs(0, 0, 1, 1, 90, 0, 0);
        check(Math.abs(r.m[0]) < 1e-5 && Math.abs(Math.abs(r.m[1]) - 1) < 1e-5, "trs rotate 90");
        // multiply: identity * identity = identity
        Mat4 mm = Mat4.multiply(Mat4.identity(), Mat4.identity());
        check(Math.abs(mm.m[15] - 1) < 1e-6 && Math.abs(mm.m[5] - 1) < 1e-6, "multiply identity");
    }

    // ---------- batcher ----------
    static void batcherTests() {
        SpriteBatcher b = new SpriteBatcher(2048);
        check(b.isEmpty(), "batcher starts empty");
        for (int i = 0; i < 2048; i++) {
            b.pushQuad(i, i, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 7);
        }
        check(b.quadCount() == 2048, "batcher capacity 2048");
        check(b.pendingBytes() == 2048 * 4 * 32, "batcher pending bytes (32B/vert)");
        b.pushQuad(0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 7);
        check(b.quadCount() == 2048, "batcher never exceeds maxQuads");
        b.reset();
        check(b.isEmpty(), "batcher reset");

        // texture-switch guard: pushing different texture into non-empty batch is a no-op
        SpriteBatcher b2 = new SpriteBatcher(64);
        b2.pushQuad(0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1);
        b2.pushQuad(0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 2);
        check(b2.quadCount() == 1, "batcher texture-switch guard");
    }

    // ---------- cache ----------
    static void cacheTests() {
        ResourceCache<String, String> cache = new ResourceCache<>(
                k -> "created:" + k,
                v -> { /* release hook */ },
                10);
        String a = cache.get("a");
        check(a.equals("created:a"), "cache factory create");
        String a2 = cache.get("a");
        check(a == a2, "cache hit returns same instance");
        check(cache.hits() == 1 && cache.misses() == 1, "cache hit/miss counters");
        for (int i = 0; i < 30; i++) cache.get("k" + i);
        check(cache.usedBytes() <= 10, "cache LRU respects budget");
        check(cache.evictions() > 0, "cache eviction happened");
        double hr = cache.hitRate();
        check(hr > 0 && hr <= 1.0, "cache hit-rate range");

        // putDirect path
        cache.putDirect("direct", "DIRECT", 3);
        check("DIRECT".equals(cache.get("direct")), "cache putDirect/get");
    }

    // ---------- scheduler ----------
    static void schedulerTests() {
        FrameScheduler s = new FrameScheduler();
        s.setDetectedRefreshHz(120);
        check(s.detectedRefreshHz() == 120.0, "scheduler refresh detect");
        check(new FrameScheduler().detectedRefreshHz() == 60.0, "scheduler default 60Hz");
        long t = 0;
        for (int i = 0; i < 200; i++) {
            dev.mali.graphics.api.GraphicsFrame f = s.begin(t);
            f.cpuSubmitNs = 2_000_000;
            t += 8_333_333;   // ~120fps
            f.endNs = t;
            s.end(f, t);
        }
        check(s.avgFps() > 100, "scheduler avgFps ~120 (got " + s.avgFps() + ")");
        check(s.droppedFrames() == 0, "no dropped frames at 120fps vs 120Hz");
        // slow frames must be flagged
        FrameScheduler s2 = new FrameScheduler();
        long t2 = 0;
        for (int i = 0; i < 50; i++) {
            dev.mali.graphics.api.GraphicsFrame f = s2.begin(t2);
            t2 += 33_000_000; // 30fps vs 60Hz → drop
            f.endNs = t2;
            s2.end(f, t2);
        }
        check(s2.droppedFrames() == 50, "slow frames counted as dropped");
        check(Math.abs(s2.avgFrameMs() - 33.0) < 1.0, "avgFrameMs ~33ms");
    }

    // ---------- mali parser ----------
    static void maliParserTests() {
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-G78") == dev.mali.graphics.core.mali.MaliFamily.VALHALL, "G78=Valhall");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-G78 MP20") == dev.mali.graphics.core.mali.MaliFamily.VALHALL, "G78 MP20=Valhall");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-G52 MC-2") == dev.mali.graphics.core.mali.MaliFamily.BIFROST, "G52=Bifrost");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-T880") == dev.mali.graphics.core.mali.MaliFamily.MIDGARD, "T880=Midgard");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-450 MP") == dev.mali.graphics.core.mali.MaliFamily.UTGARD, "450=Utgard");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-400 MP2") == dev.mali.graphics.core.mali.MaliFamily.UTGARD, "400=Utgard");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-G610 MC6") == dev.mali.graphics.core.mali.MaliFamily.VALHALL, "G610=Valhall CSF");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Mali-G715-Immortalis MC11") == dev.mali.graphics.core.mali.MaliFamily.FIFTH_GEN, "G715=5th gen");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer("Adreno 740") == dev.mali.graphics.core.mali.MaliFamily.UNKNOWN, "non-Mali=UNKNOWN");
        check(dev.mali.graphics.core.mali.MaliFamily.fromRenderer(null) == dev.mali.graphics.core.mali.MaliFamily.UNKNOWN, "null=UNKNOWN");
        check(dev.mali.graphics.core.mali.MaliFamily.UTGARD.maxExpectedGlesMajor() == 2, "Utgard ES2");
        check(dev.mali.graphics.core.mali.MaliFamily.VALHALL.maxExpectedGlesMajor() == 3, "Valhall ES3");
    }

    static void check(boolean cond, String name) {
        if (!cond) throw new AssertionError("TEST FAILED: " + name);
        System.out.println("[ok] " + name);
    }
}
