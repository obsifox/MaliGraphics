# Synchronization

Legend: `CONFIRMED` / `LIKELY` / `HYPOTHESIS` / `UNKNOWN`

## Primitives available on Android

| Primitive | API | Where we use it |
|---|---|---|
| eglSwapBuffers implicit fence | EGL | every present; free pipelining `CONFIRMED` |
| Fence FD (sync_file) | sync/NDK; Android 14: `GLES31.glImportSyncFile`? — NOT in public Java SDK | NOT used in Stage 1 (would need NDK; spec forbids C unless unavoidable) `CONFIRMED` |
| EGL sync (eglCreateSyncKHR / EGL_SYNC_NATIVE_FENCE_ANDROID) | EGL ext | async texture upload completion on ES3 devices where present — `HYPOTHESIS` benefit; Stage 2 flag |
| glFenceSync (ES3) | GLES31 | CPU-side readback guards in benchmarks only `CONFIRMED` |
| Choreographer vsync | Java framework | frame pacing `CONFIRMED` |

## Runtime threading model

```
UI thread            : lifecycle events, surface create/destroy, input
Render thread        : all EGL + GLES + command execution (single GL thread)
Loader thread(s)     : decode textures (Bitmap→staging), never touches GL
```

Cross-thread contracts `CONFIRMED` by design:
1. GPU handles are created/destroyed ONLY on render thread.
2. Loader threads produce CPU staging + a `TextureUploadRequest`; render thread drains the queue at frame start (bounded: ≤ 2 uploads/frame → stable pacing `LIKELY`).
3. Surface destroy is double-buffered through a message: render thread owns teardown.

## Locks

- `uploadQueue`: single monitor; render thread `drain(limit)`, loader thread `offer(req)`.
- No lock is ever held while issuing GL calls except the upload queue drain snapshot (copied under lock, executed outside). `CONFIRMED`
- Deadlock rule: UI thread never blocks on render thread except `awaitFirstFrame(timeout=2s)` with fallback escalation. `CONFIRMED`

## Frame contract

`beginFrame → [drain uploads → execute command list] → present → endFrame`
A fence check (cheap) decides if the previous staging slot is safe for reuse; default assumption = 3-slot rotation makes the check unnecessary on all Mali families (`LIKELY`, verified by Test 11).
