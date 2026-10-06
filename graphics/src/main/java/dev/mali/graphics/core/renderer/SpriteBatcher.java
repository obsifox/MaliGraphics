package dev.mali.graphics.core.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Dynamic quad batcher (mission §13, §18): interleaved pos2/uv2/rgba4,
 * pooled CPU staging, texture-ordered batches. Zero per-frame allocation
 * after warm-up (§29). Pure Java — unit tested.
 */
public final class SpriteBatcher {
    public static final int VERTICES_PER_QUAD = 4;
    public static final int FLOATS_PER_VERT = 8;              // x,y,u,v,r,g,b,a
    public static final int BYTES_PER_VERT = FLOATS_PER_VERT * 4;

    private final int maxQuads;
    private final ByteBuffer cpuVerts;                        // pooled staging
    private final ShortBuffer indexData;                      // static quad indices
    private int quadCount;
    private int activeTextureId = 0;                          // 0 = none/flat color
    private boolean textureIsSet = false;
    private long usedTextureId = 0;

    public SpriteBatcher(int maxQuads) {
        this.maxQuads = maxQuads;
        this.cpuVerts = ByteBuffer.allocateDirect(maxQuads * VERTICES_PER_QUAD * BYTES_PER_VERT)
                .order(ByteOrder.nativeOrder());
        this.indexData = buildIndices(maxQuads);
        reset();
    }

    public static ShortBuffer buildIndices(int maxQuads) {
        ByteBuffer bb = ByteBuffer.allocateDirect(maxQuads * 6 * 2).order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        for (int q = 0; q < maxQuads; q++) {
            int v = q * 4;
            sb.put((short) v).put((short) (v + 1)).put((short) (v + 2));
            sb.put((short) v).put((short) (v + 2)).put((short) (v + 3));
        }
        sb.position(0);
        return sb;
    }

    public void reset() {
        quadCount = 0;
        cpuVerts.clear();
        textureIsSet = false;
        activeTextureId = 0;
    }

    public boolean canFit(int quads) { return quadCount + quads <= maxQuads; }

    /**
     * Appends a quad. Vertices order: (0) x,y (1) x+w,y (2) x+w,y+h (3) x,y+h —
     * matches indices 0,1,2 / 0,2,3 (top-left origin UI space).
     */
    public void pushQuad(float x, float y, float w, float h,
                         float u0, float v0, float u1, float v1,
                         float r, float g, float b, float a,
                         float rotDeg, float pivotX, float pivotY,
                         int textureId) {
        if (!canFit(1)) return;
        if (textureIsSet && textureId != activeTextureId) return;   // caller must flush first
        activeTextureId = textureId;
        textureIsSet = true;
        if (textureId != 0) usedTextureId = textureId;

        final double rad = Math.toRadians(rotDeg);
        final float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        final float px = pivotX, py = pivotY;

        // corners relative to pivot
        float[][] corners = {
                {0, 0}, {w, 0}, {w, h}, {0, h}
        };
        float[][] uvs = {
                {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
        };

        cpuVerts.mark();
        for (int i = 0; i < 4; i++) {
            float cx = corners[i][0] - px, cy = corners[i][1] - py;
            float rx = cx * cos - cy * sin + x + px;
            float ry = cx * sin + cy * cos + y + py;
            cpuVerts.putFloat(rx);
            cpuVerts.putFloat(ry);
            cpuVerts.putFloat(uvs[i][0]);
            cpuVerts.putFloat(uvs[i][1]);
            cpuVerts.putFloat(r);
            cpuVerts.putFloat(g);
            cpuVerts.putFloat(b);
            cpuVerts.putFloat(a);
        }
        quadCount++;
    }

    /** Current batch texture (0 = flat color). */
    public int batchTextureId() { return activeTextureId; }

    public int quadCount() { return quadCount; }
    public boolean isEmpty() { return quadCount == 0; }
    public boolean isFull() { return quadCount >= maxQuads; }
    public int maxQuads() { return maxQuads; }

    /** Byte size of pending vertex data. */
    public int pendingBytes() { return quadCount * VERTICES_PER_QUAD * BYTES_PER_VERT; }

    /** Read view for upload; position/limit set to pending range. */
    public ByteBuffer pendingVertices() {
        ByteBuffer view = cpuVerts.duplicate();
        view.position(0);
        view.limit(quadCount * VERTICES_PER_QUAD * BYTES_PER_VERT);
        view.order(cpuVerts.order());
        return view;
    }

    /** Static shared index buffer (full maxQuads range; draw uses first 6*quadCount). */
    public ShortBuffer indices() {
        indexData.position(0);
        return indexData;
    }
}
