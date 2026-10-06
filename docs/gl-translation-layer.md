# GL Translation Layer (Java GL → GLES)

Status: **DESIGNED + IMPLEMENTED (compat subset)** — v3.2 (Test 13)
Evidence policy per mission §2: every claim is tagged.

## 1. Why (user decision, 2026-10)

Mobile Java-Minecraft launchers (Pojav ecosystem) run desktop GL through native
shims: GL4ES (GL 2.1 → GLES 2, ptitSeb), LTW (GL 3.2 → GLES 3), Zink (Mesa,
GL 4.x → Vulkan, needs Vulkan GPU). All are C libraries bundled as `.so` +
JNI. The user asked for our own translation layer. Decision: build it **in
Java** — no C/C++, no JNI, mission rule §31 stays intact. This is novel vs the
Pojav ecosystem: CONFIRMED (no known Java-side GL shim shipping in launchers;
gl4es/LTW/Zink are all native).

## 2. Position in the stack

```
desktop-style renderer code (LWJGL2-style static GL calls)
        │
GL11 / GL13 / GL15 / GL20 / GLU      ← Java translation layer (this package)
        │ state machine + GLSL translator + FF emulation
GLES 2.0 (android.opengl.GLES20)
        │
our EglCore / GraphicsView render thread (unchanged, §8)
        │
Mali driver (vendor) / Panfrost / Panthor …
```

The layer does NOT replace the backend-neutral API (§12); it is a second entry
point that reuses the same EGL context and render thread.

## 3. Implemented subset (v3.2)

| Feature | Status | Notes |
|---|---|---|
| Immediate mode glBegin/glEnd | CONFIRMED | QUADS expanded to triangles; STRIP/FAN/LINES/POINTS pass through |
| Two texture units | CONFIRMED | GL13.glMultiTexCoord2f → uv0/uv1 attribs; MC lightmap pattern |
| Matrix stacks | CONFIRMED | MODELVIEW/PROJECTION/TEXTURE, 32-deep, column-major |
| Textures | CONFIRMED | RGBA/RGB/LUMINANCE(_ALPHA), UBYTE + 565/4444/5551, UNPACK_ALIGNMENT |
| Display lists | CONFIRMED | command capture as closures; GL semantics: state applied at CALL time |
| Fog | CONFIRMED | GL_LINEAR only; EXP/EXP2 accepted, ignored (LIKELY adequate: MC 1.8 uses linear) |
| Alpha test | CONFIRMED | GREATER via fragment discard |
| glPush/PopAttrib | LIKELY | light subset (blend/tex/alpha/fog/depth/cull) |
| VBOs (GL15) | CONFIRMED | binds tracked; LWJGL2-style integer offsets |
| Shaders (GL20) | CONFIRMED | sources translated desktop→ES before compile |
| GLSL translator | LIKELY | strips #version, adds missing precision, 130+ in/out → attribute/varying, texture( → texture2D, fragment out → gl_FragColor #define, FF built-in #defines (gl_Vertex→aPos …) |
| Fixed-function emulation program | CONFIRMED | one built-in ES program: color × tex0 × tex1, fog mix, alpha discard |
| Lighting (glLight*) | UNKNOWN→NOT IMPL | accepted as no-op; FF lighting not emulated (MC 1.8+ uses lightmap texture instead of GL lighting — CONFIRMED) |
| glMapBuffer / PBO / FBO | NOT IMPL | rejected with GL_INVALID_OPERATION, not silent |

## 4. Known limits (honesty per §2)

- GL_QUADS value 0x0007 is the desktop enum; GLES has no QUADS — expansion is
  internal, drawArrays sees GL_TRIANGLES. CONFIRMED.
- Display lists store argument copies (arrays defensively copied at record
  time) — memory grows with list size; MC-style lists (terrain chunk) fit.
  HYPOTHESIS: fine for test workloads.
- Per-call overhead: one Java static dispatch + state lookup per GL call vs
  native shim in C. LIKELY slower than gl4es per call; total cost dominated by
  GLES driver calls either way. To be measured on device (Test 13 reports).
- glMultiTexCoord on units ≥ 2 ignored (stream has two uv slots). CONFIRMED
  limitation.
- No client-side vertex arrays (ES2 allows them; we force VBO path for driver
  friendliness on older Mali). LIKELY avoids known Utgard/Midgard quirks.

## 5. Validation plan

- Test 13 renders: spinning 8×8 textured quad grid via **display list**,
  two units (checker × lightmap), linear fog, plus a **GLSL-120-style user
  shader** through the translator, on GLES2-only path. Device run exports the
  standard JSON report → compare FPS vs Test 07 (native batching) to size the
  translation overhead. UNKNOWN until user device run (docs/DEVICE_VALIDATION.md).

## 6. Relation to Pojav ecosystem layers

| | GL4ES | LTW | Zink | Ours |
|---|---|---|---|---|
| Language | C | C | C (Mesa) | **Java** |
| Target | GL2.1→GLES2 | GL3.2→GLES3 | GL4.x→Vulkan | GL2.x-style→GLES2 |
| Runs unmodified MC jar | yes | yes | yes | **no (by design)** |
| Mission-compliant (§31 Java-only) | no | no | no | **yes** |

The layer's purpose is to let future launcher-grade Java renderers (and MC-style
rendering per §minecraft-rendering) be written against the desktop-GL idiom
while executing on GLES — not to boot the vanilla JVM client.
