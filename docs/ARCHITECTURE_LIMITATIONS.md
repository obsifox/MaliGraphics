# Architecture Limitations (honest registry — DoD §35)

## Stage 1-3 limitations (this repository)

1. **Single GL thread.** All GPU work serialized; multi-threaded command recording not supported. Justified: launcher workload is CPU-light. `CONFIRMED`
2. **GLES only.** Vulkan backend is a documented path (VULKAN_EXTENSION_PATH.md), not code. `CONFIRMED`
3. **UI-class shaders only.** No skeletal animation, no PBR, no post-process chains. Minecraft *in-game* rendering is explicitly out of scope for this milestone. `CONFIRMED`
4. **Glyph atlases are CPU-rasterized** via android.graphics at init — not SDF; scaling beyond ~2× reference size loses crispness. `CONFIRMED`
5. **Atlas packer is shelf-based** — ≤ ~78% utilization on wide assets vs ~90% for skyline packers. Acceptable; revisit with evidence. `LIKELY`
6. **No async compute / no DMA from Java.** Texture uploads drain ≤2/frame on the GL thread; large packs (texture-pack browser) rely on LRU + prewarm. `CONFIRMED` API limit
7. **Fence granularity.** Java SDK exposes no sync-fd import; staging reuse uses conservative 3-slot rotation instead of true GPU feedback. Extra ~2× staging memory. `CONFIRMED` (API), `LIKELY` (cost)
8. **Refresh-rate adaptation is measure-only** (§19) — the runtime never requests higher modes; launcher host may do so via its own APIs. `CONFIRMED`

## Knowledge limitations (research ledger)

- Per-SoC vendor DDK behavior deltas (Kirin/Exynos/MediaTek Mali packaging): UNKNOWN until device DB grows.
- Utgard render-accuracy edge cases under our ES2 path: HYPOTHESIS (no device in CI).
- 5th-gen CSF scheduling nuances: not observable from Java; tracked as UNKNOWN.

## Risk register

| Risk | Impact | Mitigation |
|---|---|---|
| Driver context loss mid-launcher | black screen | loss detect → full rebuild → fallback ladder |
| Low-end G31 GPU budget | jank on 120 Hz panels | budget from ActivityManager + CompatDB; degrade: drop shadows/atlas count |
| MC version drift (1.22+) | matrix stale | launcher/runtime decoupled by contract (§33) |

Each entry is re-tested by GraphicsLab on real hardware before being downgraded from HYPOTHESIS/UNKNOWN to CONFIRMED (§36: never hide uncertainty).
