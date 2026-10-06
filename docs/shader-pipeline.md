# Shader & Pipeline System

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## GLSL strategy

| Backend | Version | Syntax |
|---|---|---|
| GLES2 (Utgard fallback) | GLSL ES 100 | attribute/varying/gl_FragColor, texture2D |
| GLES3 (Midgard→5th gen) | GLSL ES 300 | in/out, texture() |
| Vulkan (Stage 5) | GLSL 450 + SPIR-V | offline compile (glslang) — NOT runtime GLSL |

- Runtime selects `#version 300 es` + `precision highp float` when ES3 available, else ES1.00. `CONFIRMED`
- Shaders compiled per-device on first use; **never** assume identical compile results across Mali generations (`CONFIRMED` mission rule). Compile errors and compile TIMES are recorded into the CompatDB. `CONFIRMED`
- No runtime SPIR-V on GLES; no GLSL on Vulkan — GLSL sources are single-source, parameterized by header. `LIKELY` workable for our shader subset

## Built-in pipelines (Stage 1)

1. `sprite` — pos(2) uv(2) rgba(4); uniform mvp, tint, texture — covers 90% of launcher UI `CONFIRMED`
2. `text` — same vertex layout + alpha-only glyph atlas (r channel) `CONFIRMED`
3. `post-fx` (Test 12 shader stress) — time-based distortion, temp regs pressure `CONFIRMED`

## PipelineCache

- Key: (backend, glslVersion, sourceHash, defines).
- Cache stores: program handle + link time + driver version.
- On context loss: rebuild from cache; on compile failure: fall back to `sprite` + report. `CONFIRMED`

## Mali-specific notes

- Utgard: `mediump` samplerless unit math quirks — force `highp` for positions. `LIKELY`
- Midgard: long chains of dependent ALU are slower than Bifrost — keep fragment shaders shallow; our UI shaders qualify. `LIKELY`
- Bifrost/Valhall: idle-in-tiler stalls from glReadPixels — banned in frame loop; Test suite uses fences outside frames only. `CONFIRMED`
- Compile times measured in Test 12; watchdog > 500 ms/engine-shader triggers warning into DB. `CONFIRMED` testable
