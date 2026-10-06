# Mali GPU Architecture

Status legend: `CONFIRMED` (official docs / kernel / Mesa source), `LIKELY` (multiple independent sources),
`HYPOTHESIS` (needs on-device validation), `UNKNOWN` (no reliable evidence yet).

## Family matrix

| Family | GPUs (examples) | Command model | Shader cores | GLES | Vulkan | Kernel driver | Mesa driver |
|---|---|---|---|---|---|---|---|
| Utgard | Mali-300/400/450 MP | Tile-based, per-tile command lists emitted by userspace DDK | Fragment: FAB (4 pipes); Vertex: GP | ES 2.0 (100), no ES3 | No | Binary DDK (non-upstream) | Lima (reverse-engineered) `CONFIRMED` |
| Midgard | T604/T628/T760/T860/T880/T8xx | Job manager (JS), descriptor-based jobs | Warp-less, VLIW-ish triple-core pipes | ES 3.0/3.1 `CONFIRMED` | 1.0/1.1 on some (T760+, partial) `LIKELY` | kbase (ARM binary; partial mainline) | Panfrost (GL), no Vulkan `CONFIRMED` |
| Bifrost | G31/G51/G52/G71/G72/G76 | Job manager (JS), new ISA | Fixed-functionish+MRT, warp of 4? (Bifrost warp=8?) | ES 3.1/3.2 (G31/G52/G76 = 3.2 `LIKELY`) | 1.0/1.1 (vendor) `LIKELY` | kbase | Panfrost (GL), PanVK maturing (Vulkan) `CONFIRMED` |
| Valhall | G57/G68/G77/G78/G79/G610/G710 | CSF-era? No: Valhall pre-CSF still Job Manager w/ new scheduling; G610+ = CSF | Warp of 16 `CONFIRMED` | ES 3.2 | 1.1–1.3 (G610+: 1.3 `LIKELY`) | kbase (binary), Panthor (upstream, CSF only) | PanVK (Vulkan, Valhall) `CONFIRMED` |
| 5th gen / Immortalis | G715/G925/G615/G720/G927 | CSF (Command Stream Frontend) | Warp of 16, deferred | ES 3.2 | 1.3 | Panthor (upstream) + vendor | PanVK `CONFIRMED` |

## Key facts the runtime must respect

- All Mali families are **tile-based renderers** (TBDR). `CONFIRMED`
  - Implication: avoid mid-pass flushes (`glReadPixels`, changing FBO), batch by state.
- Utgard has **no OpenGL ES 3** — GLES2 path is mandatory for very old devices. `CONFIRMED`
- Midgard/Bifrost/Valhall differ in ISA; shaders must be recompiled per-device, never shipped as machine code. `CONFIRMED`
- Driver version ≠ GPU generation: a G78 with old DDK may expose fewer extensions. Always read `GL_VERSION` + `GL_EXTENSIONS` at runtime. `CONFIRMED`
- `GL_RENDERER` strings: "Mali-G78", "Mali-G52 MC-2", "Mali-T880", "Mali-450 MP". Parsing is the only reliable in-app family detection. `CONFIRMED`

## HYPOTHESIS / UNKNOWN registry

- HYPOTHESIS: Bifrost warp size is 8 (thread group packing) — affects nothing at our Java API level; noted for profiler interpretation.
- UNKNOWN: exact vendor-DVK Vulkan feature toggles per SoC (Kirin/Exynos packaging differences). Needs device DB entries.
- UNKNOWN: whether low-end G31 devices keep ES3.2 default FBO precision under high DPI pressure — measure in Test 05.
