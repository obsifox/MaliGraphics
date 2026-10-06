package dev.mali.lab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.mali.graphics.android.AndroidDeviceInfo;
import dev.mali.graphics.android.GraphicsView;

/**
 * Main menu (mission §21): choose a GraphicsLab test or the LauncherDemo.
 * Shows the auto-detected device card (§22) before entering any scene.
 */
public final class MainActivity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(20));
        root.setBackgroundColor(0xFF09090B);

        TextView title = new TextView(this);
        title.setText("Mali Graphics Lab — v3");
        title.setTextColor(0xFF10B981);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        AndroidDeviceInfo info = AndroidDeviceInfo.probe(this);
        TextView device = new TextView(this);
        device.setText(info.summary());
        device.setTextColor(0xFFA1A1AA);
        device.setTextSize(13);
        device.setGravity(Gravity.CENTER);
        device.setPadding(0, dp(8), 0, dp(16));
        root.addView(device);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        addEntry(list, "▶  Launcher Demo (runtime UI + fallback)",
                v -> startActivity(new Intent(this, LauncherDemoActivity.class)));

        String[] tests = {
                "Test 01 — Single Quad", "Test 02 — 100 Quads", "Test 03 — 500 Quads",
                "Test 04 — 1000 Quads", "Test 05 — Textures", "Test 06 — Alpha Blending",
                "Test 07 — Sprite Batching", "Test 08 — Text Rendering",
                "Test 09 — Scrolling Launcher", "Test 10 — Animated Launcher",
                "Test 11 — Texture Cache Stress", "Test 12 — Shader Stress"
        };
        for (final String t : tests) {
            addEntry(list, t, v -> GraphicsLabActivity.start(this, t));
        }

        TextView note = new TextView(this);
        note.setText("Results export → files/mali-results (§32). Docs: docs/ in repo.");
        note.setTextColor(0xFF52525B);
        note.setTextSize(12);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        setContentView(root);
    }

    private void addEntry(LinearLayout list, String label, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(0xFF09090B);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        list.addView(b, lp);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
