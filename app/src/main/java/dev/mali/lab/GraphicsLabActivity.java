package dev.mali.lab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.android.AndroidDeviceInfo;
import dev.mali.graphics.android.GraphicsView;
import dev.mali.graphics.android.NativeFallbackView;
import dev.mali.graphics.core.profiler.ProfilerImpl;
import dev.mali.graphics.core.compat.CompatDatabase;
import dev.mali.graphics.core.mali.MaliFamily;
import dev.mali.graphics.gles.GlesRendererImpl;
import dev.mali.lab.scenes.LauncherListScene;
import dev.mali.lab.scenes.QuadScene;
import dev.mali.lab.scenes.Scene;

/**
 * Graphics Lab runner (mission §21, §32): 15 s per test, live stats overlay,
 * JSON report export into files/mali-results/.
 */
public final class GraphicsLabActivity extends Activity implements GraphicsView.Callback {

    private static final String EXTRA_TEST = "test";
    private static final long TEST_DURATION_MS = 15_000;

    private GraphicsView view;
    private TextView overlay;
    private Scene scene;
    private long testStartMs = -1;
    private boolean exported = false;

    public static void start(Context ctx, String testName) {
        Intent i = new Intent(ctx, GraphicsLabActivity.class);
        i.putExtra(EXTRA_TEST, testName);
        ctx.startActivity(i);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String testName = getIntent().getStringExtra(EXTRA_TEST);
        scene = sceneFor(testName);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        view = new GraphicsView(this, this);
        root.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        overlay = new TextView(this);
        overlay.setTextColor(0xFF10B981);
        overlay.setTextSize(13);
        overlay.setBackgroundColor(0xE609090B);
        overlay.setPadding(dp(8), dp(4), dp(8), dp(4));
        root.addView(overlay, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    static Scene sceneFor(String name) {
        if (name == null) return QuadScene.single();
        switch (name) {
            case "Test 02 — 100 Quads": return QuadScene.hundred();
            case "Test 03 — 500 Quads": return QuadScene.fiveHundred();
            case "Test 04 — 1000 Quads": return QuadScene.thousand();
            case "Test 05 — Textures": return QuadScene.textures();
            case "Test 06 — Alpha Blending": return QuadScene.alphaBlend();
            case "Test 07 — Sprite Batching": return QuadScene.batching();
            case "Test 08 — Text Rendering": return LauncherListScene.text();
            case "Test 09 — Scrolling Launcher": return LauncherListScene.scrolling();
            case "Test 10 — Animated Launcher": return LauncherListScene.animated();
            case "Test 11 — Texture Cache Stress": return LauncherListScene.cacheStress();
            case "Test 12 — Shader Stress": return QuadScene.shaderStress();
            default: return QuadScene.single();
        }
    }

    @Override public void onRuntimeReady(final GraphicsView.RenderHandle handle) {
        final GlesRendererImpl renderer = handle.gles();
        runOnUiThread(() -> Toast.makeText(this, "Runtime ready: "
                + renderer.capabilities().renderer() + " (GLES "
                + renderer.capabilities().glMajorVersion() + "." + renderer.capabilities().glMinorVersion()
                + ")", Toast.LENGTH_SHORT).show());
        scene.init(renderer, view.getWidth(), view.getHeight());
        testStartMs = System.currentTimeMillis();
    }

    @Override public void onDrawFrame(GraphicsView.RenderHandle handle, GraphicsFrame frame) {
        final GlesRendererImpl renderer = handle.gles();
        final long now = System.currentTimeMillis();
        scene.draw(renderer, frame, now);
        if (now - testStartMs > TEST_DURATION_MS && !exported) {
            exported = true;
            exportReport(renderer);
            finish();
        }
    }

    @Override public void onRuntimeFailed(Throwable cause) {
        NativeFallbackView.installFallback(this, null,
                cause == null ? "unknown" : cause.getMessage());
    }

    private void exportReport(GlesRendererImpl renderer) {
        try {
            ProfilerImpl.Snapshot s = renderer.profilerImpl().takeSnapshot();
            AndroidDeviceInfo info = AndroidDeviceInfo.probe(this);
            String glRenderer = renderer.capabilities().renderer();
            MaliFamily family = MaliFamily.fromRenderer(glRenderer);

            JSONObjectHelper json = new JSONObjectHelper()
                    .put("test", scene.name())
                    .put("gpu", glRenderer)
                    .put("architecture", family.name())
                    .put("gles", renderer.capabilities().glMajorVersion() + "."
                            + renderer.capabilities().glMinorVersion())
                    .put("driver", renderer.capabilities().version())
                    .put("device", info.manufacturer + " " + info.model)
                    .put("android", info.androidVersion)
                    .put("avgFps", round(s.avgFps))
                    .put("avgFrameMs", round(s.avgFrameMs))
                    .put("avgCpuMs", round(s.avgCpuMs))
                    .put("avgPresentMs", round(s.avgPresentMs))
                    .put("droppedFrames", s.droppedFrames)
                    .put("drawCalls", s.drawCalls)
                    .put("vertices", s.vertices)
                    .put("textureUploadBytes", s.textureUploadBytes)
                    .put("shaderCompileMs", round(s.shaderCompileMs))
                    .put("timestamp", System.currentTimeMillis())
                    .put("tested", true);

            File dir = new File(getFilesDir(), "mali-results");
            dir.mkdirs();
            File out = new File(dir, "report-" + System.currentTimeMillis() + ".json");
            FileOutputStream fos = new FileOutputStream(out);
            CompatDatabase.writeReport(fos, json.toString());
            fos.close();
            runOnUiThread(() -> Toast.makeText(this,
                    "Report saved: " + out.getName(), Toast.LENGTH_LONG).show());
        } catch (Throwable t) {
            runOnUiThread(() -> Toast.makeText(this,
                    "Export failed: " + t.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Minimal JSON writer to keep the app dependency-free (org.json is in Android). */
    private static final class JSONObjectHelper {
        private final StringBuilder sb = new StringBuilder("{");
        private boolean first = true;

        JSONObjectHelper put(String k, Object v) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(k).append("\":");
            if (v instanceof String) {
                sb.append('"').append(((String) v).replace("\"", "'")).append('"');
            } else {
                sb.append(String.format(Locale.US, "%s", v));
            }
            return this;
        }

        @Override public String toString() { return sb.append('}').toString(); }
    }
}
