# Graphics Architecture — Mali Graphics Runtime

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## Module map (code lives in `graphics/src/main/java/dev/mali/graphics/`)

```
api/      backend-neutral handles + entry (Graphics.java)         — pure Java
core/     math, CommandList, SpriteBatcher, TextureManager,
          FrameScheduler, Profiler, ResourceCache,
          MaliDetector, CompatDatabase                            — pure Java (unit-testable)
gles/     EGL14 + GLES2/3 backend (first validated backend)      — Android
android/  GraphicsView (SurfaceView+render thread), fallback UI,
          device probe                                            — Android
vulkan/   (Stage 5) same ops, different interpreter               — reserved
mali/     family parsing glue → CompatDB                          — pure Java
```

## Threading & frames

1 UI thread · 1 render thread (all GL) · N loader threads (decode only).
Frame: `Choreographer → beginFrame → drain uploads → record+execute commands → present → stats`.
Scheduler adapts to 60/90/120/144 Hz by reading `Display.getRefreshRate()` and never forcing modes (§19 `CONFIRMED`).

## Renderer decisions

- Sprite batching: one dynamic VBO pool, texture-sorted insertion, 2048 quads/batch cap, orphan+subdata upload. `CONFIRMED` technique, `LIKELY` perf on TBDR
- Text: glyph atlas generated at runtime via android.graphics (no bundled font files), R-channel atlas. `CONFIRMED`
- Textures: TextureManager with LRU cache + async decode queue + shelf atlas packer. `CONFIRMED`
- Clipping: glScissor per clip op (cheap on Mali tilers vs rect-shader discard — `LIKELY`).
- Blending: premultiplied OFF; standard SRC_ALPHA/ONE_MINUS_SRC_ALPHA for UI. `CONFIRMED`

## Fallback ladder (§24)

```
AUTO → GLES3 → GLES2 → native Android views
```
Any init/first-frame failure escalates automatically; the launcher must stay usable (GraphicsView exposes `onRuntimeFailed` for the host to swap views). `CONFIRMED`

## What we deliberately do NOT build (review gate Q4)

- No windowing/compositing (Android owns it), no kernel driver, no GL translation of Minecraft, no shader cross-compiler, no display-mode forcing.
