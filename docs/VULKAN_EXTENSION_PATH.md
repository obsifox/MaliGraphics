# Vulkan Extension Path (Stage 5 blueprint)

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`
Mission §12: Vulkan is designed-in from day one, not the first implementation.

## Principles

1. **No engine duplication.** The same `GraphicsCommandBuffer` op set is interpreted by
   `vulkan.VulkanCommandBuffer` into VkCmd* calls. Scenes never change. `CONFIRMED` design
2. GLSL single-source for UI shaders → offline `glslangValidator` to SPIR-V, shipped in assets.
   Runtime GLSL compilation is NOT used on Vulkan. `CONFIRMED`
3. Push constants capped at 128 bytes (Mali guaranteed minimum). `CONFIRMED`
4. One `VkRenderPass` per frame for UI; load/clear per FrameScheduler state. `LIKELY`

## Mapping table (op → Vulkan)

| GraphicsCommandBuffer op | Vulkan call (Stage 5) |
|---|---|
| bindPipeline | vkCmdBindPipeline |
| bindTexture | descriptor set bind (VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER) |
| setUniformMatrix/Vec4/Float | vkCmdPushConstants (MVP block ≤ 64 B) |
| bindVertexBuffers | vkCmdBindVertexBuffers + Index |
| drawQuads | vkCmdDrawIndexed(6n) |
| setScissor | vkCmdSetScissor (dynamic state) |
| present | vkQueuePresentKHR + WSI swapchain |

## Additional Vulkan-only runtime objects

- `VulkanDevice`: instance/physical selection filtered by Mali families (Valhall+, Vulkan 1.1+ `LIKELY`).
- `VulkanSwapchain`: per GraphicsSurface; recreated on resize/loss (same lifecycle as EGL).
- `VulkanDescriptorPool`: growing ring, 256 sets initial. `HYPOTHESIS` sizing.

## Device gating

`GraphicsTypes.Backend.AUTO` stays GLES until the CompatDB marks a device
`vulkanValidated: true` after a Lab Vulkan smoke test. No silent switching.

## What is intentionally NOT scheduled

Geometry shaders, transform feedback, timeline semaphores (UI workload does not need them) — revisit only with evidence (§36 rule).
