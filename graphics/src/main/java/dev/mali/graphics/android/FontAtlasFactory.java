package dev.mali.graphics.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import dev.mali.graphics.api.GraphicsTypes;
import dev.mali.graphics.core.renderer.FontAtlas;
import dev.mali.graphics.gles.GlesRendererImpl;
import dev.mali.graphics.gles.GlesTexture;

/**
 * Rasterizes a glyph atlas with android.graphics into an ALPHA8 texture
 * (docs/shader-pipeline.md: text = alpha atlas). Runs once at init on the
 * render thread — no font files shipped with the runtime (CONFIRMED benefit: tiny APK).
 */
public final class FontAtlasFactory {

    private FontAtlasFactory() {}

    /** Result bundle so no static/global state is needed (mission §29). */
    public static final class Result {
        public final FontAtlas atlas;
        public final GlesTexture texture;
        Result(FontAtlas atlas, GlesTexture texture) { this.atlas = atlas; this.texture = texture; }
    }

    public static Result create(GlesRendererImpl renderer, String fontFamily, float refSizePx, int atlasSize) {
        FontAtlas atlas = new FontAtlas(atlasSize, atlasSize, refSizePx, fontFamily);
        String charset = FontAtlas.defaultCharset();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(fontFamily == null ? Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                : Typeface.create(fontFamily, Typeface.NORMAL));
        paint.setTextSize(refSizePx);
        paint.setColor(Color.WHITE);

        Paint.FontMetrics fm = paint.getFontMetrics();
        atlas.ascentPx = fm.ascent;      // negative
        atlas.descentPx = fm.descent;    // positive

        Bitmap bitmap = Bitmap.createBitmap(atlasSize, atlasSize, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);

        int penX = 1, penY = 1, rowHeight = 0;

        float[] widths = new float[charset.length()];
        paint.getTextWidths(charset, widths);

        for (int i = 0; i < charset.length(); i++) {
            char ch = charset.charAt(i);
            float w = Math.max(1f, widths[i]);
            float h = (fm.descent - fm.ascent);
            int wi = (int) Math.ceil(w) + 1;
            int hi = (int) Math.ceil(h) + 1;

            if (penX + wi >= atlasSize) {
                penX = 1;
                penY += rowHeight + 1;
                rowHeight = 0;
            }
            if (penY + hi >= atlasSize) break;   // atlas full — rest fall back to '?'

            FontAtlas.Glyph g = new FontAtlas.Glyph();
            g.ch = ch;
            g.x = penX; g.y = penY;
            g.width = wi; g.height = hi;
            g.xoff = 0;
            g.yoff = fm.ascent;                  // draw from top of line box
            g.xadv = w;
            atlas.put(g);

            canvas.drawText(String.valueOf(ch), penX, penY - fm.ascent, paint);

            penX += wi + 1;
            rowHeight = Math.max(rowHeight, hi);
        }

        // copy ALPHA8 bytes into GPU texture
        int pixels = atlasSize * atlasSize;
        byte[] alpha = new byte[pixels];
        int[] row = new int[atlasSize];
        for (int y = 0; y < atlasSize; y++) {
            bitmap.getPixels(row, 0, atlasSize, 0, y, atlasSize, 1);
            for (int x = 0; x < atlasSize; x++) {
                alpha[y * atlasSize + x] = (byte) Color.alpha(row[x]);
            }
        }
        bitmap.recycle();

        GlesTexture tex = (GlesTexture) renderer.device().createTexture(
                atlasSize, atlasSize, GraphicsTypes.TextureFormat.ALPHA8,
                GraphicsTypes.FilterMode.LINEAR, GraphicsTypes.FilterMode.LINEAR,
                GraphicsTypes.WrapMode.CLAMP_TO_EDGE, "font-atlas");
        tex.uploadPixels(alpha, atlasSize, atlasSize, GraphicsTypes.TextureFormat.ALPHA8);
        return new Result(atlas, tex);
    }

    /** Text drawing through the sprite batcher (alpha-only atlas). */
    public static void installTextRenderer(final GlesRendererImpl renderer, final FontAtlas atlas, final GlesTexture tex) {
        renderer.setFontRenderer(new GlesRendererImpl.FontRenderer() {
            @Override public void draw(GlesRendererImpl r, String text, float x, float y, float sizePx,
                                       float cr, float cg, float cb, float ca) {
                if (tex == null) return;
                final float scale = sizePx / atlas.refSizePx;
                float pen = x;
                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);
                    FontAtlas.Glyph g = atlas.get(ch);
                    if (g == null) g = atlas.get('?');
                    if (g == null) continue;
                    float gx = pen + g.xoff * scale;
                    float gy = y + g.yoff * scale;
                    float gw = g.width * scale;
                    float gh = g.height * scale;
                    float u0 = (float) g.x / atlas.atlasWidth;
                    float v0 = (float) g.y / atlas.atlasHeight;
                    float u1 = (float) (g.x + g.width) / atlas.atlasWidth;
                    float v1 = (float) (g.y + g.height) / atlas.atlasHeight;
                    r.drawQuad(tex, gx, gy, gw, gh, u0, v0, u1, v1, cr, cg, cb, ca, 0, 0, 0);
                    pen += g.xadv * scale;
                }
            }
        });
    }
}
