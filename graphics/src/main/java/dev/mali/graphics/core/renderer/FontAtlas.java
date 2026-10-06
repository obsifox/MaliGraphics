package dev.mali.graphics.core.renderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Glyph atlas data for the text renderer (mission §13). Pure data — the atlas
 * bitmap itself is produced on Android by android/FontAtlasFactory (decode-free path).
 */
public final class FontAtlas {
    public static final class Glyph {
        public char ch;
        public int x, y, width, height;   // atlas rect (pixels)
        public float xoff, yoff, xadv;    // per-glyph offsets in px at refSize
    }

    public final int atlasWidth;
    public final int atlasHeight;
    public final float refSizePx;         // size the atlas was rasterized at
    public final String fontFamily;
    private final Map<Character, Glyph> glyphs = new HashMap<>(128);
    public float ascentPx, descentPx;

    public FontAtlas(int atlasWidth, int atlasHeight, float refSizePx, String fontFamily) {
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.refSizePx = refSizePx;
        this.fontFamily = fontFamily;
    }

    public void put(Glyph g) { glyphs.put(g.ch, g); }
    public Glyph get(char c) { return glyphs.get(c); }
    public boolean hasGlyph(char c) { return glyphs.containsKey(c); }
    public int glyphCount() { return glyphs.size(); }

    /** ASCII + Latin-1 + common punctuation coverage for launcher UI. */
    public static String defaultCharset() {
        StringBuilder sb = new StringBuilder(256);
        for (char c = 32; c < 127; c++) sb.append(c);            // ASCII
        sb.append("©®°±×÷«»…‹›€£¥¢¹²³¼½¾");
        return sb.toString();
    }
}
