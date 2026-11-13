# Starlight Camera vMax

Experimental maximum-quality build of Starlight Camera for sweetin.

## Current build (.49)

- Gemini two-pass GL: Pass 1 BT.709 chroma bilateral + motion-gated temporal
  accumulation; Pass 2 directional de-blur + dual-band frequency separation +
  IGN dithering. RGB carries color, alpha carries BT.709 luma.
- 1:1 cadence (one encoded frame per source frame); the old 2x interpolation
  path is disabled, so there is no interpolated-frame ghosting.
- Green tint fixed: no double saturation/contrast; luma-only gain; encoder and
  muxer force BT.709/full/SDR.
- `LMC8.4`/`LMC` branding removed from localized strings and `/LMC8.4/` paths
  now use `/StarlightCamera/`. Package name remains
  `com.google.android.GoogleCameraLMCR17`.
- Sliders are now full-range: Off is truly zero (no temporal/chroma/de-blur/
  detail/S-curve), Maximum is strong. Denoise -> `0.90*temporal` + chroma mix;
  Sharpen -> `1.25*de-blur`; Detail -> `1.35*fine + 0.60*clarity`.
- Latest device test (.49): 30/30/30 frames, 30fps, HEVC/AAC, BT.709.
  Max vs Off: SSIM `0.9601`, PSNR `31.73 dB`; Laplacian variance
  Off/Default/Max `510 / 869 / 1543`. Bytes: Off `6,060,319`,
  Default `6,010,398`, Max `6,227,918`.

APK SHA-256:
`f81eaf7a31c283d4872a26a243e09dbbc3f136933ec4c79162bcea00594ebce5`
(138,880,279 bytes, versionCode 64764922, versionName
`8.4.300.414775575.49-vMax`).

## What it adds over v2.0

- **Photo upscale range**: `default` keeps the v2.0/Tensei pipeline (24 MP,
  JPEG q98); `max` uses GPU tiled readback with a Lanczos3 fallback, the exact
  Tensei colour matrix, edge-gated unsharp, EXIF copy, JPEG q98 and a 40 MP cap.
  Tiles are 1024 px; 24 MP and 40 MP outputs have no seams.
- **Potato device mode**: caps photos at 16 MP/2x, video at 1080p30, lowers
  sharpening and bitrate, and disables auto video. Manual hold still works.
- **Video SR**: same-resolution hardware HEVC output with AAC re-encoded and
  clipped to the video end. The Gemini two-pass GL runs at source resolution;
  a capability probe picks the hardware encoder.
- **Video tier**: source resolution by default; legacy tier values remain for
  compatibility.
- **Thermal pacing**: moderate/severe throttling, critical abort, power-save
  pacing, and storage rechecks every 10 s.
- **Foreground service**: photo/video jobs stay alive under a `dataSync` FGS.
- **Separate `:sr` process**: video SR runs in
  `com.google.android.GoogleCameraLMCR17:sr` so its heap cannot starve the
  camera.
- **Live progress**: animated rainbow bar (6 dp) and silent ongoing
  notification.
- **Photo auto-upscale cleanup**: with auto-upscale ON, the source JPEG is
  deleted only after the upscaled file exists. Hold-to-process keeps the
  original. A “Delete original after upscale” switch controls auto behavior.
- **Preview opener fix**: tapping the preview thumbnail opens the selected
  gallery/opener; supports performClick/contentDescription.
- **Diagnostics**: rolling `starlight/starlight.log` plus background
  `ApplicationExitInfo` dump.
- **Debug receiver** for repeatable tests.

## Artifacts

| Artifact | Path |
|---|---|
| APK | `agents/results/lmc84-upscale-vMax/artifacts/Starlight-Camera-vMax.49.apk` |
| Device copy | `/sdcard/Download/Starlight-Camera-vMax.apk` |
| Config | `agents/results/lmc84-upscale-vMax/artifacts/Starlight_Master.xml` |
| Source | `agents/results/lmc84-upscale-vMax/src/` |
| Patches | `agents/results/lmc84-upscale-vMax/patches/` |
| Screenshots | `agents/results/lmc84-upscale-vMax/screenshots/` |

APK SHA-256:
`f81eaf7a31c283d4872a26a243e09dbbc3f136933ec4c79162bcea00594ebce5`
(138,880,279 bytes, versionCode 64764922, versionName
`8.4.300.414775575.49-vMax`).

## Install

```bash
adb install -r agents/results/lmc84-upscale-vMax/artifacts/Starlight-Camera-vMax.apk
```

The APK is signed with the same `starlight.jks` key as v2.0, so `-r` works.

## Test

```bash
# Video SR; receiver routes to the :sr process
adb shell am broadcast -n com.google.android.GoogleCameraLMCR17/com.starlight.camera.StarlightVideoReceiver \
  -a com.starlight.camera.VIDEO_SR

# Photo max mode
adb shell am broadcast -n com.google.android.GoogleCameraLMCR17/com.starlight.camera.StarlightVideoReceiver \
  -a com.starlight.camera.DEBUG_MAX
```

Outputs land in `/sdcard/DCIM/Camera/` as `Upscaled_Photo_*.jpg` or
`Upscaled_Video_*.mp4`.

## Verified on device

- Photo 24 MP: 5657x4243, no seams; source removed.
- Photo 40 MP: 7302x5476, no seams; source removed.
- Potato photo: 3464x4618 (16.0 MP), 4.2 MB; source removed.
- Hold-to-process: original + `Upscaled_Person_...jpg` both kept.
- Video normal: 1920x1080 -> 3840x2160, 34 frames, 1.467 s.
- Video potato: 1280x720 -> 1920x1080, 34 frames, 1.467 s.
- `:sr`: `ps` showed `com.google.android.GoogleCameraLMCR17:sr`; 4K output
  published with `is_pending=0`.
- Cold launch after force-stop: `Status: ok`, `COLD`, `TotalTime: 1874` ms;
  no new app ANR.
- Preview tap: `com.google.android.apps.photos/.pager.HostPhotoPagerActivity`.

## Limits

- Media3 migration is not implemented; no `androidx.media3` dependency exists
  in the APK. The pipeline still uses `MediaExtractor`/`MediaCodec`.
- Photo jobs still run in the app process; only video SR is routed to `:sr`.
- No real ML super-resolution model, NDK/Vulkan path, or 4:4:4 JPEG encoder.
- HDR/10-bit video is skipped; long-run thermal soak remains untested.
- Android logs a `ForegroundServiceTypeLoggerModule` “no types” warning despite
  the manifest `dataSync` type; the service starts and completes normally.

## Rollback

Uninstall vMax and install the v2.0 APK:

```bash
adb uninstall com.google.android.GoogleCameraLMCR17
adb install agents/results/lmc84-upscale-v2/artifacts/Starlight-Camera-v2.0.apk
```
