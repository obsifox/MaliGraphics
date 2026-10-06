# Android Graphics Architecture (how Java reaches the GPU)

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## The official path (the ONLY path we use)

```
Java: SurfaceView ──► Surface ──► EGL window surface ──► GLES context ──► GPU
                  └─► BufferQueue ──► SurfaceFlinger ──► Hardware Composer (HWC) ──► display
```

- `SurfaceView.getHolder().getSurface()` gives a producer side of a BufferQueue. `CONFIRMED`
- EGL14.eglCreateWindowSurface(display, config, surface, ...) binds it as the render target. `CONFIRMED`
- eglSwapBuffers enqueues the frame; SurfaceFlinger (with HWC) composites. `CONFIRMED`
- Fences: each queued buffer carries a sync fence; presentation and GPU work are pipelined by the system. We never wait on CPU for GPU except via `EGL_SYNC`/`glFinish` in tests. `CONFIRMED`

## Forbidden by project rules (§5 of mission)

- Direct `/dev/mali*` access from Java. `CONFIRMED` (blocked by SELinux + useless from JVM)
- Kernel code in prototype.
- Bypassing BufferQueue (e.g., private display writes).

## Components and who owns what

| Component | Owner | Our runtime does |
|---|---|---|
| BufferQueue | Android | consumes via EGL; sizes triple-buffered `LIKELY` |
| SurfaceFlinger/HWC | Android | nothing (system) |
| AHardwareBuffer | Android (NDK; Java via TextureView/Surface) | not used in Stage 1; reserved for zero-copy textures later (HYPOTHESIS win for video thumbs) |
| TextureView | Android | supported alternative surface ( costs 1 extra composition `LIKELY`) |
| Choreographer | Android | frame pacing source on render thread `CONFIRMED` |

## Lifecycle hazards (each covered by tests)

- Surface destruction on backgrounding → EGL surface invalid → recreate on `surfaceAvailable`. `CONFIRMED`
- Context loss (driver reset) → detect `EGL_CONTEXT_LOST` / `GL_CONTEXT_LOST` → rebuild all GPU resources from caches. `CONFIRMED` mechanism, HYPOTHESIS frequency
- Rotation → surface size change → viewport + scissor + surface-scale update. `CONFIRMED`
