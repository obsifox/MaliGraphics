# Memory Model

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## Ownership layers

| Memory | Allocated by | Freed by | Notes |
|---|---|---|---|
| Vertex/index CPU staging | runtime (direct ByteBuffers) | GC (keep fixed pools) | allocate once per capacity, orphan-rotate; NEVER per frame `CONFIRMED` rule |
| GPU buffer (VBO/IBO) | GLES driver via glBufferData | glDeleteBuffers + our refcount | driver-owned store `CONFIRMED` |
| Texture device memory | driver | glDeleteTextures | upload via glTexSubImage2D from CPU staging `CONFIRMED` |
| Java heap decode (Bitmap→pixels) | BitmapFactory | GC + Bitmap.recycle() | decode on worker thread, copy into staging, recycle bitmap immediately `CONFIRMED` |
| Native graphics heap | driver/GrAlloc | Android ION/GRALLOC + driver | out of our control; monitor via Debug.getNativeHeapAllocatedSize() `LIKELY` |

## Sizing policy

- Sprite vertex pool: `maxQuadsPerBatch × 4 verts × 32 bytes` (pos2 uv2 rgba4). 2048 quads → 256 KB/vertex-buffer slot, double-buffered → 512 KB. `CONFIRMED` arithmetic
- Texture budget: default 96 MB device estimate from `ActivityManager.getMemoryClass()` (heuristic — `LIKELY`, tune per-device via CompatDB).
- Atlas: 2048² RGBA8 = 16 MB GPU. At most 4 UI atlases in Stage 1. `CONFIRMED` arithmetic

## Rules

1. Zero allocations in the frame loop (verify in Test 12 via Profiler.allocPerFrame == 0). `CONFIRMED` testable
2. CPU staging buffers are pooled with generation counters; a buffer is reused only when its fence-reported frame has completed (2-frame latency safe default `LIKELY`).
3. Bitmaps: decode → `Bitmap.getPixels` into pooled int[] → recycle Bitmap; never hold Bitmap across frames. `CONFIRMED`
4. All handles are reference-counted; explicit `destroy()` decrements; finalizer only logs leaks (never relies on GC timing). `CONFIRMED`
