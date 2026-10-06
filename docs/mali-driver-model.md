# Mali Driver Model: kbase / Panfrost / PanVK / Panthor / Lima

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## Stack placement

```
Java app (our runtime)
   └── Android framework (Surface/SurfaceFlinger)
        └── libEGL_mali.so / libvulkan (vendor) or Mesa (Panfrost/PanVK)
             └── kernel: kbase (vendor, /dev/mali0) | panthor (mainline CSF) | panfrost/lima (mainline legacy)
                  └── Mali GPU
```

## Driver inventory

| Driver | Kernel/userspace | Targets | Upstream status | Suitable for our runtime? |
|---|---|---|---|---|
| ARM Binary DDK (kbase + binary ES/Vulkan ICD) | both closed | Utgard→5th gen (per release) | vendor-only | YES (the default on retail phones) `CONFIRMED` |
| kbase (mainline staging) | kernel, open | Midgard→Valhall (JM), CSF via separate path | partially in mainline (drivers/gpu/drm/arm/display? no — kbase not fully upstream) | Not our concern in Java; Android retail ships vendor kbase `CONFIRMED` |
| Panfrost | Mesa GLES3.x | Midgard + Bifrost | upstream Mesa `CONFIRMED` | Only on devices running mainline/Mesa (dev boards, some tablets) — runtime must still work (standard EGL) |
| PanVK | Mesa Vulkan | Midgard(exp)/Bifrost/Valhall | upstream Mesa, conformance growing `CONFIRMED` | Same as Panfrost |
| Panthor | kernel DRM driver | CSF GPUs (G610+, 5th gen) | mainline since 6.9-ish `LIKELY` | Future path with Mesa; not present on retail phones yet |
| Lima | Mesa GLES2 | Utgard | upstream Mesa `CONFIRMED` | Legacy devices running custom ROMs — HYPOTHESIS rare on user fleet |

## Rules derived

1. Our Java runtime never talks to `/dev/mali0` or ioctls — that is below EGL/Vulkan ICD. `CONFIRMED` (Android SELinux + spec §5)
2. Behavior differences appear to us as: supported GLES version, extension list, driver bugs, shader compile time. The Compatibility DB captures these. `CONFIRMED`
3. Same GPU + different driver = different device entry in the DB. `CONFIRMED`
