# Real Device Validation Protocol (§32)

Run on any ARM Mali phone/tablet (Android 5.0+).

## Steps

1. Build: `./gradlew :app:assembleDebug` (Android Studio: Run ▶).
2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. Launch **Mali Graphics Lab**.
   - Home screen shows: device card (SoC/GPU family/driver/GLES/Vulkan/RAM/refresh) — auto-detected.
4. Run each test 01–12 (auto-advances 15 s, shows live FPS/CPU-ms/draw-calls/tris).
5. Tap **Export report** → writes `mali-results/report-<ts>.json` to app files dir.
6. Pull: `adb shell run-as dev.mali.lab cat files/mali-results/<file>.json > report.json`
7. Attach the JSON to `compat-db.json` PR (set `"tested": true` for your device).

## Pass criteria per milestone

| Metric | Gate |
|---|---|
| Test 01 single quad | ≥ display refresh (no dropped frames in 15 s window) |
| Test 04 1000 quads | ≥ 60 FPS on Midgard+ ; ≥ 30 FPS on Utgard/Low-Bifrost (HYPOTHESIS gate, tune after first runs) |
| Test 07 batching | draw calls ≤ 12 for 1000 same-texture quads |
| Test 11 cache stress | hit-rate ≥ 92%, zero per-frame allocations |
| Fallback drill | force-fail flag in Lab → native UI shows, no crash |

## Known-pitfall capture

If a test regresses vs a previous report, Lab prints the delta on the result card — paste it into the device's DB entry under `knownIssues`.
