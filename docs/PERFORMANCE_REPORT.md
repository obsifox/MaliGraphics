# Performance Report — template + current lab results

Legend: `CONFIRMED` (measured in sandbox) / `PENDING` (needs real Mali device per §32)

## Sandbox self-check (JVM, CONFIRMED)

Ran `SelfCheck` on the pure-Java core (math, batcher, cache, scheduler, Mali parser):

```
[SelfCheck] Mat4 ortho+trs ... OK
[SelfCheck] SpriteBatcher 2048-quad capacity ... OK
[SelfCheck] SpriteBatcher texture-switch guard ... OK
[SelfCheck] ResourceCache LRU eviction + hit-rate ... OK
[SelfCheck] FrameScheduler window stats ... OK
[SelfCheck] MaliFamily parser (G78/G52/T860/450/G610/G715) ... OK
```

## Real-device report (PENDING — fill from GraphicsLab export)

| Test | Target | Result | Device / driver |
|---|---|---|---|
| 01 single quad | refresh-capped | PENDING | — |
| 02 100 quads | refresh-capped | PENDING | — |
| 03 500 quads | ≥60 FPS | PENDING | — |
| 04 1000 quads | ≥60 Midgard+, ≥30 low | PENDING | — |
| 05 textures | ≥60 with 64 textures | PENDING | — |
| 06 alpha blend | no overdraw collapse | PENDING | — |
| 07 sprite batching | ≤12 draw calls / 1000 quads | PENDING | — |
| 08 text | 500 glyphs @ 60 FPS | PENDING | — |
| 09 scrolling | stable frame time | PENDING | — |
| 10 animated | 120 Hz capable | PENDING | — |
| 11 cache stress | hit ≥92%, 0 allocs/frame | PENDING | — |
| 12 shader stress | compile < 500 ms | PENDING | — |

Export format: `files/mali-results/report-<timestamp>.json` (DeviceValidation.md step 6).
