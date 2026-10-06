# Compatibility Matrix

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`
This file is the human-readable view; machine DB: `graphics/src/main/assets/mali/compat-db.json`.

## Mali family × runtime capability

| Family | GLES versions | Backend chosen | Known issues to encode | Evidence |
|---|---|---|---|---|
| Utgard (400/450) | ES2.0 | GLES2 path | no ES3, mediump precision issues, tiny tex units (2-4?) | ARM docs `CONFIRMED` |
| Midgard (T6xx-T8xx) | ES3.0/3.1 | GLES3 | glReadPixels stall; T760+ Vulkan 1.0 partial | ARM + Panfrost `CONFIRMED` |
| Bifrost (G31/51/52/71/72/76) | ES3.1/3.2 | GLES3 | G31 low tex budget; driver variance high | Panfrost `CONFIRMED`, per-device `UNKNOWN` |
| Valhall (G57/68/77/78/79, G610/710) | ES3.2 | GLES3 (+future Vulkan 1.1-1.3) | none blocking; CSF from G610 | ARM `CONFIRMED` |
| 5th gen (G615/G715/G720/G925/Immortalis) | ES3.2 | GLES3 (+Vulkan 1.3) | none known | ARM `CONFIRMED` |

## Minecraft range × rendering need (condensed from minecraft-rendering.md)

| MC range | API need | Our answer |
|---|---|---|
| 1.0–1.12 | FF/GL2 via translation | launcher-only Stage 1-4; future: GLES2 path present |
| 1.13–1.21.x | core GL3.2 (translators) | launcher-only; ES3.2 devices preferred |
| 1.22–1.26 | UNKNOWN | HYPOTHESIS: same trajectory |

## Device DB growth protocol (§23, §32)

1. GraphicsLab run writes `DeviceInfo + Capabilities + per-Test metrics` JSON to `filesDir/mali-results/`.
2. `tested:false` until a real device run is exported and attached.
3. Fields: gpu, architecture, driver, gles, vulkan, backend, limitations[], knownIssues[], tested.
