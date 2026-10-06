package dev.mali.lab;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import dev.mali.graphics.api.GraphicsFrame;
import dev.mali.graphics.android.FontAtlasFactory;
import dev.mali.graphics.android.GraphicsView;
import dev.mali.graphics.android.NativeFallbackView;
import dev.mali.graphics.gles.GlesRendererImpl;
import dev.mali.graphics.gles.GlesTexture;

/**
 * Demo launcher UI rendered THROUGH the runtime (mission §14, §34):
 * account header, version cards with PLAY buttons, scrolling list,
 * animated background — same primitives exported to launchers.
 * Hard failure of the runtime swaps to NativeFallbackView (§24, never crash).
 */
public final class LauncherDemoActivity extends Activity implements GraphicsView.Callback {

    private GraphicsView view;
    private volatile GlesRendererImpl renderer;
    private volatile boolean ready = false;
    private float scrollY = 0;
    private float touchY = -1f;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        view = new GraphicsView(this, this);
        root.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        Toast.makeText(this, "LauncherDemo — swipe vertically to scroll", Toast.LENGTH_LONG).show();
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                touchY = event.getY();
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                if (touchY >= 0) {
                    scrollY -= (event.getY() - touchY);
                    touchY = event.getY();
                    if (scrollY < 0) scrollY = 0;
                }
                return true;
            case android.view.MotionEvent.ACTION_UP:
                touchY = -1;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override public void onRuntimeReady(GraphicsView.RenderHandle handle) {
        renderer = handle.gles();
        // glyph atlas for all text (§16/§17: no bundled font, generated once)
        FontAtlasFactory.Result font = FontAtlasFactory.create(
                renderer, null, 48f, 512);
        FontAtlasFactory.installTextRenderer(renderer, font.atlas, font.texture);
        screenW = Math.max(1, view.getWidth());
        screenH = Math.max(1, view.getHeight());
        ready = true;
    }

    @Override public void onDrawFrame(GraphicsView.RenderHandle handle, GraphicsFrame frame) {
        final GlesRendererImpl r = handle.gles();
        if (!ready) return;
        long time = System.currentTimeMillis();

        r.clearColor(0.035f, 0.035f, 0.043f, 1f);

        // animated ambient background (§14)
        drawAmbient(r, time);

        // header
        r.drawText("Minecraft Launcher — Mali Runtime", 24, 30, 34, 1f, 1f, 1f, 1f);
        r.drawText("Account: Player_One", 24, 74, 24, 0.63f, 0.66f, 0.7f, 1f);

        // scrolling version cards
        final float cardH = 150f, gap = 18f;
        final String[] versions = {
                "Minecraft 1.21.1", "Minecraft 1.20.6", "Minecraft 1.19.4",
                "Minecraft 1.16.5", "Minecraft 1.12.2", "Minecraft 1.8.9", "Minecraft 1.0"
        };
        float contentH = versions.length * (cardH + gap);
        float sw = getScreenW(r);
        float sh = getScreenH(r);
        if (scrollY > contentH - sh + 120) scrollY = contentH - sh + 120;
        if (scrollY < 0) scrollY = 0;

        for (int i = 0; i < versions.length; i++) {
            float y = i * (cardH + gap) - scrollY + 110;
            if (y + cardH < 90 || y > sh) continue;
            // card
            r.pushClip(16, (int) Math.max(0, y), (int) (sw - 32), (int) cardH);
            r.drawQuad(null, 16, y, sw - 32, cardH, 0, 0, 1, 1,
                    0.13f, 0.14f, 0.15f, 0.97f, 0, 0, 0);
            // icon
            r.drawQuad(null, 32, y + 24, 56, 56, 0, 0, 1, 1,
                    0.06f, 0.73f, 0.49f, 1f, 0, 0, 0);
            // text
            r.drawText(versions[i], 104, y + 42, 32, 1f, 1f, 1f, 1f);
            r.drawText("Last Play: 2026-10-0" + (1 + i % 9) + "   Play Time: " + (2 + i % 8) + "h",
                    104, y + 84, 22, 0.63f, 0.66f, 0.7f, 1f);
            // PLAY button
            float bx = sw - 150, by = y + cardH / 2f - 22;
            r.drawQuad(null, bx, by, 110, 44, 0, 0, 1, 1,
                    0.06f, 0.73f, 0.49f, 1f, 0, 0, 0);
            r.drawText("PLAY", bx + 28, by + 12, 24, 0.04f, 0.04f, 0.05f, 1f);
            r.popClip();
        }

        // footer fps
        r.drawText(String.format("FPS %.1f | %d draws", frame.fps(), frame.drawCalls), 24, sh - 30, 22,
                0.06f, 0.91f, 0.55f, 1f);
    }

    private void drawAmbient(GlesRendererImpl r, long time) {
        float t = time / 1000f;
        for (int i = 0; i < 6; i++) {
            float x = (float) (Math.sin(t * 0.3 + i * 1.7) * 0.5 + 0.5) * getScreenW(r) - 120;
            float y = (float) (Math.cos(t * 0.23 + i * 2.3) * 0.5 + 0.5) * getScreenH(r) - 120;
            r.drawQuad(null, x, y, 240, 240, 0, 0, 1, 1,
                    0.06f, 0.29f, 0.20f, 0.10f, 0, 0, 0);
        }
    }

    private float screenW = -1, screenH = -1;

    private float getScreenW(GlesRendererImpl r) {
        if (screenW < 0) screenW = Math.max(1, view.getWidth());
        return screenW;
    }

    private float getScreenH(GlesRendererImpl r) {
        if (screenH < 0) screenH = Math.max(1, view.getHeight());
        return screenH;
    }

    @Override public void onRuntimeFailed(Throwable cause) {
        // §24: native fallback — the launcher stays usable
        NativeFallbackView.installFallback(this, null,
                cause == null ? "runtime init failed" : cause.getMessage());
    }
}
