# Starlight Camera vMax — tiled photo, hardware video, potato mode

## Result

Built, signed, installed, and device-tested `Starlight-Camera-vMax.apk` on
sweetin (`dd89fda1`). This round adds tiled 24/40 MP photo readback, potato
mode, capability-probed video tiers, thermal-aware pacing, a foreground
service, and a separate `:sr` process for video SR.

**Status: device-tested.** The APK is installed and
`/sdcard/Download/Starlight-Camera-vMax.apk` matches the repo artifact.

## 2026-11-13 — .48 Gemini two-pass update

**Status: device-tested.** Installed `Starlight-Camera-vMax.48.apk`
(versionCode `64764921`, versionName `8.4.300.414775575.48-vMax`).

- Pass 1: BT.709 chroma bilateral + motion-gated temporal accumulation; RGB in
  color, BT.709 luma packed in alpha. Pass 2: directional edge de-blur,
  dual-band frequency separation, luma-only gain, S-curve, IGN dithering.
- Output is one frame per source frame (1x cadence). This removes the
  interpolation ghosting seen in .43; the previous 2x path is no longer used.
- Green tint fixed by removing double saturation/contrast and forcing encoder
  and muxer color metadata to BT.709/full/SDR. Latest test: 30/30/30 frames,
  30fps, HEVC/AAC, 6,035,031 bytes. Y/U/V means: output `100.26/111.25/126.36`
  vs source `100.87/111.92/126.88`.
- Branding: `LMC8.4`/`LMC` removed from all localized string resources and
  `/LMC8.4/` storage paths now use `/StarlightCamera/`. APK label is
  `Starlight Camera`; package name is unchanged.
- `Starlight_Master.xml` updated with Gemini-safe values (qjpg 98, LMC internal
  upscale off, sharpmini 3.0, front Sabre 0.35, FPS_30, lens-5 opmodes,
  reduced HDR/ZSL frame counts, media opener on).
- APK SHA-256: `c04414b557c1b0824b2005486fdcd7c89417bc784e325f860fd0b7515cac02fb`.
- Pushed to `/sdcard/Download/Starlight-Camera-vMax.apk`; saved under
  `agents/results/lmc84-upscale-vMax/artifacts/` and
  `roms/coloros/fixes/starlight-camera-vmax/files/`.
- No reboot, no git commit.

## 2026-11-13 — .49 slider range fix

**Status: device-tested.** Installed `Starlight-Camera-vMax.49.apk`
(versionCode `64764922`, versionName `8.4.300.414775575.49-vMax`).

- Root cause: sliders were wired, but the mapping was too subtle. Off still
  applied temporal `0.15`, de-blur `0.228`, fine `0.225`, clarity `0.09`, and a
  fixed S-curve, so low vs max looked almost identical.
- New mapping: `uTemporalStr = 0.90 * denoise` (chroma bilateral strength also
  follows it), `uDeBlurStr = 1.25 * sharpen`,
  `uFineDetailStr = 1.35 * detail`, `uClarityStr = 0.60 * detail`, and the
  S-curve is scaled by detail. Off is now truly zero.
- Device test on the same 1080p30 HEVC clip: 30/30/30 frames, HEVC/AAC,
  BT.709/full. Max vs Off: SSIM `0.9601`, PSNR `31.73 dB`; Laplacian variance
  Off/Default/Max `510 / 869 / 1543`. Bytes: Off `6,060,319`, Default
  `6,010,398`, Max `6,227,918`. Sliders left at balanced defaults
  (0.5 / 0.5 / 0.25).
- APK SHA-256: `f81eaf7a31c283d4872a26a243e09dbbc3f136933ec4c79162bcea00594ebce5`.
- Pushed to `/sdcard/Download/Starlight-Camera-vMax.apk`; saved under
  `agents/results/lmc84-upscale-vMax/artifacts/` and
  `roms/coloros/fixes/starlight-camera-vmax/files/`.
- No reboot, no git commit.

## Artifacts

| Artifact | Path | SHA-256 |
|---|---|---|
| APK (.48) | `agents/results/lmc84-upscale-vMax/artifacts/Starlight-Camera-vMax.48.apk` | `c04414b557c1b0824b2005486fdcd7c89417bc784e325f860fd0b7515cac02fb` |
| Previous APK (.34) | `agents/results/lmc84-upscale-vMax/artifacts/Starlight-Camera-vMax.34.apk` | `19fbbd63...` |
| Device APK | `/sdcard/Download/Starlight-Camera-vMax.apk` | `c04414b5...` |
| Source | `agents/results/lmc84-upscale-vMax/src/` | — |

APK: 138,364,010 bytes, versionCode `64764907`, versionName
`8.4.300.414775575.34-vMax`, minSdk 29, targetSdk 29.
Signer: `CN=Starlight Ports, OU=Ports, O=Starlight, C=IN`,
cert SHA-256 `c4765c669fd97b6dcfd32d1cb600dd3174d7009138797ba9f1a6e47600a39186`.
`classes4.dex` SHA-256
`d0c6bcf72faed2d5f7832d38be1c82aea9752ff635bb66998a76c6068005e6d2`.

## Code changes

### Tiled photo GL (`StarlightGL.java`)

- 1024 px tiles, fixed FBO, `Canvas` composite into the final bitmap.
- Removed the old full-frame contrast stretch; output alpha forced to 1.0.
- 24 MP output is 5657x4243; 40 MP output is 7302x5476. No tile seams in the
  captured images.

### Potato mode (`StarlightPolicy.java`)

- Settings key `pref_starlight_potato_key`, under Settings → Starlight Camera.
- Photo cap 16 MP, 2x, JPEG 95, sharpen 0.10.
- Video cap 1080p, 30 fps, bitrate ≤10 Mbps, sharpen 0.08.
- Disables auto video; manual hold still works.

### Video planner (`StarlightVideoTranscoder.java`, `StarlightCapabilities.java`)

- Capability probe selects a hardware encoder and the highest supported output
  edge (3840/2560/1920/1280).
- Non-integer target sizing: 720p potato → 1920x1080; 1080p normal → 3840x2160.
- Thermal pacing: MODERATE 40 ms, SEVERE 150 ms, CRITICAL abort; power-save
  25 ms. Storage rechecked every 10 s.
- PTS handling remains one-in/one-out; `KEY_ALLOW_FRAME_DROP=0`; encoder ladder
  still strips unsupported keys.

### Foreground service and `:sr`

- `StarlightJobService` keeps photo/video jobs alive with a `dataSync` FGS.
- `StarlightSrService` runs video SR in `android:process=":sr"`; the debug
  `VIDEO_SR` receiver now routes there. Device `ps` shows
  `com.google.android.GoogleCameraLMCR17:sr` during the job.
- Manifest adds `FOREGROUND_SERVICE_DATA_SYNC` and `POST_NOTIFICATIONS`.

### Settings

- Added Potato device mode.
- Added Video upscale tier: Auto / 1080p / 1440p / 4K / Off.
- Added Delete original after upscale (default on; hold always keeps it).
- Renamed photo range to “Default — 2x, 24 MP, JPEG 98” and
  “Experimental max — 2x, 40 MP, JPEG 98”.
- Updated hardware-acceleration summary to the GPU tile path.
- No “Upscale strength” entry exists in the current preference tree.

### UI

- Rainbow bar increased to 6 dp, 20 dp side margins, 36 dp bottom margin,
  stronger glow, and detached-window animation stop.

## Verified on device

### Tiled photo

```text
17:54 .31 24 MP -> Upscaled_Night_20261112_175411.jpg, 5657x4243
17:57 .31 40 MP -> Upscaled_Night_20261112_175645.jpg, 7302x5476
18:26 .32 potato -> Upscaled_Night_20261112_182619.jpg, 3464x4618 (16.0 MP)
18:50 .34 normal -> Upscaled_Night_20261112_184906.jpg, 4243x5657 (24.0 MP)
```

Each auto job removed the source JPEG; only the upscaled file remained.
Hold-to-process:

```text
18:34 photo start Person_20261112_175956_Starlight.jpg scale=2 deleteOriginal=false
18:34 photo done Person_20261112_175956_Starlight.jpg
```

Both `Person_...jpg` and `Upscaled_Person_...jpg` remained.

### Video

```text
potato: 1280x720 -> 1920x1080, 34 frames, 1.467 s, is_pending=0
normal: 1920x1080 -> 3840x2160, 34 frames, 1.467 s, 5,351,177 bytes
:sr:   1920x1080 -> 3840x2160, 34 frames, 1.467 s
```

`ps` during the `:sr` run showed the separate
`com.google.android.GoogleCameraLMCR17:sr` process.

### Launch / stability

```text
am force-stop; am start -W CameraLauncher
Status: ok
LaunchState: COLD
TotalTime: 1874 ms
```

No new app ANR. Preview thumbnail still opens
`com.google.android.apps.photos/.pager.HostPhotoPagerActivity`.

## Build

```text
javac -source 1.8 -target 1.8 -bootclasspath android.jar   -> 0
d8 --min-api 29 --lib android.jar                           -> 0
apktool b tree/live                                         -> 0
zip replace classes4.dex                                    -> 0
zipalign -f -p 4                                            -> 0
apksigner sign (starlight.jks)                              -> 0
aapt2 badging -> 64764907 / 8.4.300.414775575.34-vMax
```

## Limits

- Media3 migration is not implemented. The APK has no `androidx.media3`
  classes or dependency; the pipeline still uses `MediaExtractor`/`MediaCodec`.
- Photo jobs still run in the app process; only video SR is routed to `:sr`.
- No ML super-resolution model, NDK/Vulkan path, or 4:4:4 JPEG encoder.
- Long-run thermal soak and 10-bit/HDR video remain untested.
- Android logs a `ForegroundServiceTypeLoggerModule` “no types” warning despite
  the manifest `dataSync` type; the service starts and completes normally.

## Rollback

```bash
adb uninstall com.google.android.GoogleCameraLMCR17
adb install agents/results/lmc84-upscale-v2/artifacts/Starlight-Camera-v2.0.apk
```

## 2026-11-13 — Photo upscale orientation fix (.51-vMax)

**Symptom.** Upscaling a portrait photo produced a landscape output (3968x2968)
with the scene rotated 90 degrees.

**Cause.** The long-press upscale path feeds the DNG to `StarlightSR`. That DNG
is 2000x1500 with TIFF IFD0 orientation 6, but `android.media.ExifInterface`
returns `0` for it (verified on-device with an `app_process` harness:
DNG -> 0, JPEG -> 1/6 as expected). `orient()` therefore skipped rotation.

**Fix.** Added `com.starlight.camera.StarlightTiff`, a minimal TIFF reader that
parses the header and IFD0 tag `0x0112`, and used it as a fallback in
`StarlightSR.orient()` and `StarlightSRDefault.orient()` whenever the
ExifInterface result is `<= 0`. `.50` hit a `VerifyError` because `v7` was
undefined on the catch merge; `.51` initialises `v7` before the try block.

**Verification (.51, versionCode 64764924, SHA-256
`cda6344ae47b87fa76c3629e1b7046cba547d1ec84911b79ae94a78c8a2176e8`).**

| Job | Input | Output | Result |
|-----|-------|--------|--------|
| DNG (orientation 6) | `DSC_...160500.dng` 2000x1500 | `Upscaled_...160500.jpg` 2968x3968, Orientation 1 | upright, matches source (`artifacts/orient_fix_compare.png`) |
| JPEG (orientation 1) | `DSC_...170000.jpg` 3000x4000 | `Upscaled_...170000.jpg` 4242x5656, Orientation 1 | unchanged, no regression |

Job log: `photo start ...160500...dng` 15:39:28 -> `photo done` 15:39:54.

Test copies and outputs were removed from `/sdcard/Pictures/Raw/`; screen
rotation setting restored to auto. No reboot, no git commit.
