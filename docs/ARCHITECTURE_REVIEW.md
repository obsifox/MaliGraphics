# MALI GRAPHICS ARCHITECTURE REVIEW (Gate §26)

Answers 1–15. Evidence tags inline.

1. **What exactly is the Mali stack?** ARM GPUs from Utgard→5th gen, driven by vendor kbase + binary userspace DDK on retail Android; Mesa (Lima/Panfrost/PanVK) + Panthor on mainline. Tile-based renderers throughout. `CONFIRMED`
2. **What does Android provide?** Surface/BufferQueue/SurfaceFlinger/HWC, EGL/GLES/Vulkan loaders, Choreographer pacing, AHardwareBuffer. `CONFIRMED`
3. **What must our runtime provide?** Backend-neutral Java API, command model, sprite/text/texture services, batching, resource cache, frame scheduler, profiler, capability detection, CompatDB, fallback. `CONFIRMED`
4. **What must it NOT provide?** Compositing, kernel access, display forcing, Minecraft internals, shader cross-compilation for Vulkan (offline instead). `CONFIRMED`
5. **Kernel parts?** kbase/panthor/panfrost/lima — none of ours. `CONFIRMED`
6. **Userspace parts?** Driver ICDs (theirs); our runtime sits above EGL/Vulkan. `CONFIRMED`
7. **Java parts?** Everything in `api/ core/ android/ mali/`. `CONFIRMED`
8. **GLES parts?** `gles/` — EGL context, VBO/VAO, GLSL ES compile, state mapping. `CONFIRMED`
9. **Vulkan parts?** `vulkan/` Stage 5: same op interpretation; push-constant ≤128B; renderpasses per frame. `CONFIRMED` design / not yet code
10. **Mali-specific?** Family detection (GL_RENDERER parsing), CompatDB limitations/knownIssues, tiler-friendly rules (no mid-pass readback, stable state). `CONFIRMED`
11. **Universal?** Command model, batching, caching, scheduling — identical across GPUs. `CONFIRMED`
12. **How do generations differ?** GLES max version (2.0 vs 3.x), Vulkan (none→1.3), warp/ISA (compile-time only), perf envelopes. See mali-architecture.md. `CONFIRMED`
13. **Minecraft 1.0–1.26?** Launcher workload now; in-game later via interfaces; matrix in minecraft-rendering.md; 1.22+ tagged UNKNOWN/HYPOTHESIS. `CONFIRMED`
14. **Minimum viable implementation?** Stage 1-2: GraphicsView + GLES3/2 backend + sprite/text batching + texture cache + fallback + GraphicsLab. SHIPPED IN THIS REPO. `CONFIRMED`
15. **What can be postponed?** Vulkan backend (Stage 5), multi-window/atlas streaming, AHardwareBuffer zero-copy, in-game MC integration (Stage 4+). `CONFIRMED`

**Decision: proceed to implementation** — Gate passed for Stages 1-3.
