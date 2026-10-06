# Mali V3 — Java Android Graphics Runtime

A reusable, measurable, Mali-aware 2D graphics runtime for Android **Minecraft launchers**,
written in pure Java over Android's official graphics stack (EGL + OpenGL ES, Vulkan-ready).
Mission window: Minecraft 1.0 → 1.26 · Hardware: ARM Mali (Utgard → 5th gen).

```
Minecraft Launcher (host)
   └── Java Graphics SDK  (graphics/api)
        └── Mali Graphics Runtime (graphics/core + gles + android)
             ├── GLES backend (Stage 1 — shipped)
             └── Vulkan backend (Stage 5 — documented path)
                  └── Android Graphics Stack → Mali Driver → Mali GPU
```

## Modules

| Path | What |
|---|---|
| `graphics/api/` | Backend-neutral handles: Device, Surface, Context, Buffer, Texture, Sampler, Shader, Pipeline, CommandBuffer, Queue, Fence, Frame, Renderer, Capabilities, Profiler |
| `graphics/core/` | Math, pooled command recorder, SpriteBatcher, FontAtlas data, TextureManager (LRU), ResourceCache, FrameScheduler, Profiler, MaliFamily parser, CompatDatabase — **pure Java, unit tested** |
| `graphics/gles/` | EGL14 context, GLES2/3 handles, capability snapshot, command executor, batching renderer |
| `graphics/android/` | `GraphicsView` (SurfaceView + render thread + Choreographer + lifecycle), glyph atlas factory, native fallback UI, device probe |
| `app/` | **Graphics Lab** (Tests 01–12 + JSON report export) and **Launcher Demo** (launcher UI through the runtime + fallback drill) |
| `docs/` | Architecture KB with `CONFIRMED/LIKELY/HYPOTHESIS/UNKNOWN` tags (mission §25), review gate (§26), limitations, Vulkan path, perf report, device protocol |

## Build & run

```bash
# Android Studio: open this folder, then Run 'app' on a Mali device.
# or CLI:
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- `MainActivity` → device card + entries for all 12 Lab tests + Launcher Demo.
- Each Lab test runs 15 s and exports `files/mali-results/report-<ts>.json` (pull per `docs/DEVICE_VALIDATION.md`).
- Pure-core JVM check (no Android needed): `javac + java graphics/src/test/.../SelfCheck.java` — see `docs/PERFORMANCE_REPORT.md`.

## Fallback ladder (never crash the launcher — mission §24)

```
AUTO → GLES3 → GLES2 → Native Android UI
```
`GraphicsView.Callback.onRuntimeFailed` hands the host a ready-made native screen.

## Rules honored from the mission (§28-29)

- Java + Android SDK only; zero C/C++ in the prototype; no `/dev/mali*`, no kernel code.
- No per-frame allocation (pooled commands + staging), no UI-thread GL, no hard-coded GPU caps.
- Runtime capabilities always queried live; the CompatDB grows from real Lab reports.

## License

Apache-2.0
