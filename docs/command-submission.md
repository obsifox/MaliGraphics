# Command Submission Model

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## Flow (backend-neutral)

```
App / Scenes
   │  GraphicsRenderer.draw*()   (immediate-mode recorder, pooled objects)
   ▼
GraphicsCommandBuffer (recorded list, reset each frame)
   │  backend interprets
   ▼
GLES:  state ops → gl* calls           Vulkan: recorded to VkCommandBuffer (Stage 5)
   ▼
Queue submit → present (eglSwapBuffers / vkQueuePresentKHR)
```

## Recorded op set (v1)

`VIEWPORT, SCISSOR(clip), CLEAR, BLEND(mode), BIND_PIPELINE, BIND_TEXTURE(unit,handle),
BIND_VERTEX_DATA(batchRange), PUSH_CONSTANTS(=uniform block: mvp,tint,time), DRAW_QUADS(range)`

- Ops are pre-allocated objects recycled via `CommandPool` — no GC in frame loop. `CONFIRMED`
- Record order = submission order; no reordering in Stage 1 (Mali TBDR prefers state stability over micro-reorder — `LIKELY`).
- Batcher merges consecutive ops sharing (pipeline, texture, blend) into one DRAW — reduces Mali tiler state switches `LIKELY` (verify Test 07 vs Test 03).

## GLES mapping

| Op | GLES call |
|---|---|
| BIND_PIPELINE | glUseProgram |
| BIND_TEXTURE | glActiveTexture + glBindTexture |
| BLEND | glEnable(GL_BLEND)+glBlendFuncSeparate |
| DRAW_QUADS | glDrawElements(GL_TRIANGLES, 6n, UNSIGNED_SHORT/INT, 0) |
| PUSH_CONSTANTS | glUniform* (per-draw MVP block; low-end-safe `LIKELY`) |

## Vulkan mapping (reserved interfaces, Stage 5)

One GraphicsPipeline = one VkPipeline; BIND_TEXTURE = descriptor bind; PUSH_CONSTANTS = vkCmdPushConstants (≤128 B limit respected `CONFIRMED` Mali minimum 128).
No duplicate engine: same CommandBuffer ops, different interpreter. `CONFIRMED` architecture
