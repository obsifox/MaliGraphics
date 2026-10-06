# Minecraft Rendering Requirements (1.0 → 1.26)

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

> Scope rule (§33): the runtime serves **launcher UI** in Stage 1-4 and *future* in-game rendering via clean interfaces. It does NOT rewrite Minecraft.

## Java Edition matrix

| MC range | LWJGL/GL requirement | What it means on Mali/Android | Renderer concern for our runtime |
|---|---|---|---|
| 1.0 – 1.12.2 | compat GL 1.3–2.1, fixed function + ARB shaders | Needs GL translation layer (gl4es-style) on top of ES2/ES3 — launcher itself does NOT need FF | texture-heavy UI, simple 2D `CONFIRMED` |
| 1.13 – 1.16.5 | GL 3.2 core-ish | gl4es 1.1.5+/Zink on GLES3.2 devices `LIKELY` | same launcher concerns |
| 1.17 – 1.19.4 | GL 3.2 core strictly | core-profile translation (Zink/Mesa) `LIKELY` | launcher unaffected |
| 1.20 – 1.21.x | GL 3.2 core, Java 17→21 runtime | same as above; bigger textures/atlas 2048+ `CONFIRMED` | launcher icon/thumb sizes grow |
| 1.22 – 1.26 | UNKNOWN (future) | assume same core-GL trajectory `HYPOTHESIS` | keep launcher decoupled (§ rule) |

## Bedrock / Android specifics

- Bedrock (RenderDragon) ships native GLES2/GLES3/Vulkan paths — NOT embeddable in launchers; irrelevant to our runtime except as inspiration. `CONFIRMED`
- MC-related launchers on Android (Pojav/FCL family) run the JRE + translation layers; a launcher UI using our runtime sits beside them, sharing the process only if the launcher chooses. `CONFIRMED` by ecosystem observation

## Runtime concerns per workload (from §6)

| Workload | Texture needs | Buffer needs | Android concern |
|---|---|---|---|
| Launcher UI | icons, skins, backgrounds (≤2048² atlases) | static quads + dynamic batches | 60-144 Hz pacing, low draw calls |
| Texture-pack browser | hundreds of 128²–512² thumbs | streaming upload queue | decode off-thread, LRU eviction `CONFIRMED` |
| Mod manager icons | small, many | same | cache hit-rate target ≥ 92% (Test 11) `CONFIRMED` metric |
| Shader-pack preview | animated per-frame uniforms | dynamic | compile time budget — Test 12 |
| In-game render (future) | MC-generated atlas | large dynamic VB | explicitly OUT of Stage 1 scope `CONFIRMED` |

## Decoupling contract

`dev.mali.graphics.*` contains ZERO Minecraft symbols. Launchers adapt via their own modules; compatibility layers (e.g., version→asset mapping) belong to the launcher, not the runtime. `CONFIRMED` (enforced by review gate Q13)
