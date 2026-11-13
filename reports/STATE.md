# TASK lmc84-upscale-vMax — experimental maximum-quality build

## State
task: lmc84-upscale-vMax
status: in_progress
last_verified: 2026-11-13 15:40 — .51-vMax installed (versionCode 64764924); photo-upscale orientation bug fixed (StarlightTiff TIFF-IFD0 fallback, v7 pre-initialised to clear VerifyError). Upscaled orientation-6 DNG -> 2968x3968 portrait, EXIF Orientation=1, visually upright vs source; log shows photo done in 26s. SHA-256 cda6344ae47b87fa76c3629e1b7046cba547d1ec84911b79ae94a78c8a2176e8.
next_step: User to confirm upscale orientation in gallery; optional JPEG-only path regression and long-run thermal soak remain open.

## Log
- 2026-11-12 12:2x confirmed device online and v2.0 installed.
- 2026-11-12 12:4x implemented vMax `StarlightSR` (Lanczos3/RS/colour/unsharp/q100/EXIF/40MP), `StarlightSRDefault`, `StarlightVideoSR`, receiver, notifier; added photo-quality and video settings.
- 2026-11-12 13:1x fixed `StarlightSR.applyColorMatrix()` AIOOBE; max photo produced 5476x7302 26.5MB in ~18.8s.
- 2026-11-12 14:1x default photo produced 4243x5657 11.9MB in ~8.7s; max macro 26.9MB in ~11s.
- 2026-11-12 14:2x video SR produced 3840x2160 24fps H.264 + AAC, 15.7MB (earlier build).
- 2026-11-12 14:5x fixed preview opener id from `preview` to `thumbnail_button`; tap opened Photos.
- 2026-11-12 15:0x added `VIDEO_SR --es path`; video output 19,854,916 bytes, 4K, audio copied.
- 2026-11-12 15:1x copied APK/XML/source/screenshots to results; pushed APK; created camera fix folder and registered it.
- 2026-11-12 16:1x user reported video failures; replaced red ring with `StarlightProcessingBar`; integrated `StarlightGL` into photo/video paths; highp shaders.
- 2026-11-12 16:3x video FBO path regressed to 0/3,232-byte outputs and ANR. Wrote PROBLEM_STATEMENT.md and read Claude/DeepSeek guidance.
- 2026-11-12 16:5x implemented `StarlightVideoGL` and `StarlightVideoTranscoder`; MediaStore pending sink, audio passthrough, PTS monotonicity, full-duration, HDR skip.
- 2026-11-12 17:0x .25/.26 video verified 3840x2160, 34 frames, AAC 67, 1.466656 s; preview opened Photos.
- 2026-11-12 17:10 implemented hardware codec selection, allow-frame-drop=0, encoder ladder, storage/thermal prechecks, `StarlightLog`, auto photo original deletion, preview accessibility.
- 2026-11-12 17:18 .27 video: qcom HEVC decoder + qcom AVC encoder, 34/34/34 frames, 5,351,177 bytes.
- 2026-11-12 17:22 .28 selfie/macro auto jobs each left one upscaled item; added memory cap.
- 2026-11-12 17:27 .29 auto photo left only `Upscaled_Photo_20261112_172658.jpg`.
- 2026-11-12 17:29 found startup ANR from synchronous `ApplicationExitInfo` dump; moved to background in .30.
- 2026-11-12 17:31 .30 cold launch 2295 ms; video `(5)` 5,351,177 bytes, 34 frames, AAC 67.
- 2026-11-12 17:33 copied .30 APK/sources; updated manifests/README/REPORT.
- 2026-11-12 17:48 added tiled `StarlightGL` readback (`TILE=1024`, fixed FBO, Canvas composite), `StarlightPolicy`, potato mode, capability probe, thermal pacing, FGS, settings cleanup, `:sr` video service, 6dp rainbow bar.
- 2026-11-12 17:54 .31 tiled 24 MP: `Upscaled_Night_20261112_175411.jpg`, 5657x4243, original removed; no seams.
- 2026-11-12 17:57 .31 max 40 MP: `Upscaled_Night_20261112_175645.jpg`, 7302x5476, original removed; no seams.
- 2026-11-12 18:26 .32 potato photo: `Upscaled_Night_20261112_182619.jpg`, 3464x4618 (16.0 MP), original removed.
- 2026-11-12 18:28 .32 potato video: 1280x720 -> 1920x1080, 34 frames, 1.467 s, `is_pending=0`.
- 2026-11-12 18:32 .32 normal video: 1920x1080 -> 3840x2160, 34 frames, 1.467 s, 5,351,177 bytes.
- 2026-11-12 18:34 hold-to-process: `Person_20261112_175956_Starlight.jpg` -> `Upscaled_Person_20261112_175956.jpg`; original kept; `deleteOriginal=false`.
- 2026-11-12 18:35 .32 cold launch 1874 ms; no new ANR; preview opener still opens Photos.
- 2026-11-12 18:40 .33 `:sr` video service produced `Upscaled_Video_20261112_183000_NormalTest (1).mp4`, 4K.
- 2026-11-12 18:46 .34 `:sr` video produced `(2)` 4K, 34 frames; `ps` showed `com.google.android.GoogleCameraLMCR17:sr`.
- 2026-11-12 18:50 .34 normal auto photo: `Upscaled_Night_20261112_184906.jpg`, 4243x5657 (24.0 MP), original removed.
- 2026-11-12 18:55 copied .34 APK/sources/screenshots; pushed APK; device hash matches `19fbbd63...`.

## 2026-11-12 19:45 — new user requirements
- Video SR is now **quality enhancement at source resolution**, not 2x/4K upscaling. Must keep all frames and PTS, use GPU shader for denoise/sharpen/colour, no resolution change.
- Photo default must beat Tensei; max must be clearly better. Add JPG/JPEG/HEIC/PNG output setting.
- Preview opener must use native intent/startActivity choke point, not global touch interceptor.
- New circular Starlight app icon.
- Re-read Claude + DeepSeek suggestions and implement remaining.
- SSH root is down; only adb `dd89fda1` non-root shell. No reboot, no git commit.
- Current .34 has 1080p30 2.9s -> 4K 87/87 frames, but user reports lag/drops on longer clips and wants same-resolution enhancement.


## 2026-11-13 08:45 — icon + video quality rework
- Launcher icon replaced with 108dp safe-zone Starlight star/ring assets for mdpi..xxxhdpi; manifest now also sets `roundIcon`.
- Android 12 system splash now shows the new icon correctly (verified screenshot).
- In-app settings header LMC logo replaced: `res/drawable/signature.xml` now uses `@drawable/starlight_signature` (512px circular Starlight badge); `signaturedown.xml` changed from LMC 4-color bar to gold rule.
- Window-background splash `cover.xml` now uses `starlight_splash.png` (icon + STARLIGHT wordmark) at 160x188dp instead of clipped 54dp LMC logo.
- Video path reworked: same source resolution, output frame rate doubled to 60fps for <=1080p (motion-adaptive interpolated frames via FBO blend shader), HEVC encoder preferred, target bitrate ~100 Mbps / source*1.2, `outFps` used in encoder + PTS. Expected 8.3s 1080p source -> ~100 MB, not 3.3 MB.
- `StarlightVideoGL` now renders enhanced current frame to FBO, blends previous+current for intermediate PTS, encodes current at original PTS; two FBO textures/program added.
- Fixed .36 failure: `enc = createCodec(outMime, true)` (was hard-coded AVC while MediaFormat was HEVC).
- .37 built/signed/pushed. Device package uninstalled by user; install attempts via adb timed out, so manual install requested.
- No git commit; no reboot.


## 2026-11-13 11:10 — Gemini two-pass, color fix, branding removal (.44–.48)
- Read Gemini Argon `1.txt`, `2.txt`, `3.txt`; implemented Pass 1 (BT.709 chroma bilateral + motion-gated temporal accumulation, luma packed in alpha) and Pass 2 (directional de-blur + dual-band frequency separation + IGN dithering) in `StarlightVideoGL`.
- Replaced the old fused merge/CAS/interpolation path. Output is now 1:1 in time (one encoded frame per source frame) so interpolation ghosting cannot occur. `StarlightVideoTranscoder` FPS doubling and expected-frame doubling patched to 1x.
- Fixed green tint: removed double saturation/contrast from the GL path; Pass 2 applies luma-only gain. Encoder/muxer now force `color-standard=BT709`, `color-range=full`, `color-transfer=SDR_VIDEO`; output probes `bt709/bt709/bt709`.
- Wired the Detail slider into Pass 2 (`videoDetail`); Denoise controls temporal strength; Sharpen controls de-blur.
- Removed `LMC8.4`/`LMC` from all `values*/strings.xml` and `/LMC8.4/` paths in smali (`/StarlightCamera/` now); APK label remains `Starlight Camera`. Package name intentionally unchanged.
- Updated `artifacts/Starlight_Master.xml` with Gemini-safe values: qjpg 98, LMC internal upscale off, sharpmini 3.0, front sabre 0.35, FPS_30, lens-5 opmodes, reduced frame counts, media opener on.
- .47 had a `VerifyError` from reusing p5 (float sat) as boolean; fixed by moving sat to v0 before the closed check. .48 verified on device.
- .48 APK saved to `agents/results/.../artifacts/` and `roms/coloros/fixes/starlight-camera-vmax/files/`; XML saved to both; APK pushed to `/sdcard/Download/Starlight-Camera-vMax.apk`.
- No reboot, no git commit.

## 2026-11-13 12:02 — .49 slider sensitivity fix
- Read `StarlightPolicy.floatPref` (`:165`): it parses the stored string, clamps 0..1, returns default on failure. Settings use `ListPreference` values `0.0/0.25/0.5/0.75/1.0`; wiring is correct.
- UI confirmed all three sliders were already at `Maximum`; set them to `Off` via taps and processed the same clip -> Off still had visible processing.
- Root cause was the mapping, not missing wiring: Off applied temporal 0.15, de-blur 0.228, fine 0.225, clarity 0.09 and a fixed S-curve.
- Patched `StarlightVideoGL.java` and `StarlightVideoGL.smali`: `uTemporalStr=0.90*denoise`, chroma bilateral follows it, `uDeBlurStr=1.25*sharpen`, `uFineDetailStr=1.35*detail`, `uClarityStr=0.60*detail`, S-curve scaled by detail. Removed the constant offsets.
- Rebuilt with `apktool b tree` (versionCode 64764922), zipaligned, signed with `starlight.jks`, installed with `adb install -r`.
- Device test: Off 6,060,319 bytes; Max 6,227,918 bytes; both 30 frames, BT.709. Max vs Off SSIM 0.9601/PSNR 31.73 dB; Laplacian variance 510 -> 1543. Saved `out49_off.mp4`, `out49_max.mp4`, frame crops.
- Updated fix manifest, root manifest, README, REPORT, artifacts and `/sdcard/Download/Starlight-Camera-vMax.apk`.

## 2026-11-13 12:10 — clean install requested
- User requested full uninstall of `com.google.android.GoogleCameraLMCR17`, removal of `/sdcard/LMC8.4/` and all config dirs, fresh `.49` install, fresh `Starlight_Master.xml`, launch verification.
- Device paths observed: `/sdcard/LMC8.4` (files, libs, patchedlibs, Starlight_backup), `/sdcard/StarlightCamera` (files, logcat, AWB CSS), no `/sdcard/DCIM/LMC*`.
- Correct config folder from smali `xxx.saveSharedPreferences`: `/sdcard/StarlightCamera/`; target `/sdcard/StarlightCamera/Starlight_Master.xml`.

## 2026-11-13 12:41 — APK copied for manual install
- Copied `/tmp/recover37/newbuild/Starlight-Camera-vMax.49.apk` to `/home/starlight/Downloads/Starlight-Camera-vMax.49.apk`; SHA-256 `f81eaf7a31c283d4872a26a243e09dbbc3f136933ec4c79162bcea00594ebce5`, 138,880,279 bytes. No rebuild.

## 2026-11-13 12:46 — PC copies + device config/libs
- Copied `.49` APK, `Starlight_Master.xml`, original Canon lib, and Starlight-branded lib to `/home/starlight/Downloads/`.
- Pushed config to `/sdcard/LMC8.4/Starlight_Master.xml` and `/sdcard/StarlightCamera/Starlight_Master.xml`; libs to both `libs/` dirs.
- Pushed APK to `/sdcard/Download/Starlight-Camera-vMax.49/` for manual install.
- App is NOT installed: repeated `adb install`/`pm install` attempts were aborted/timed out; package absent.

## 2026-11-13 15:40 — .50/.51 photo-upscale orientation fix
- Root cause: `triggerLatestUpscale` upscales the DNG; `android.media.ExifInterface` returns 0 for that DNG (verified on-device via `app_process`), so `StarlightSR.orient()` skipped rotation and output stayed landscape.
- Added `com.starlight.camera.StarlightTiff` (Java -> javac -> d8 -> tiny APK -> baksmali) parsing TIFF header/IFD0 tag 0x0112; verified DNG 141727 -> 6, DNG 151249 -> 1, JPEG -> 0.
- Patched `StarlightSR.orient()` and `StarlightSRDefault.orient()`: save path in v7, fallback to `StarlightTiff.orientation` when ExifInterface result <= 0.
- .50 VerifyError: `v7` Undefined at catch merge; fixed in .51 by `const/4 v7, 0x0` before the try block.
- .51 built (versionCode 64764924, versionName 8.4.300.414775575.51-vMax), zipaligned, signed, `adb install -r` Success.
- Device test: long-press upscale button at physical (97,1717) (display forced portrait; `settings put system user_rotation 0`). Log: `photo start DSC_20261113_160500_lmc_8.4.dng` 15:39:28 -> `photo done` 15:39:54. Output `Upscaled_20261113_160500_lmc_8.4.jpg` 5,499,069 bytes, 2968x3968, Orientation 1; side-by-side with source JPEG upright and sharper (`artifacts/orient_fix_compare.png`, `artifacts/upscaled_160500_fixed.jpg`).
- Cleanup pending: remove test DNG copy, pushed JPEG and upscaled outputs; restore `accelerometer_rotation`.
- JPEG regression checked: upscaling `DSC_20261113_170000_lmc_8.4.jpg` (3000x4000, orient 1) produced 4242x5656 Orientation 1 — no rotation, fallback inert.
- Cleaned `/sdcard/Pictures/Raw/` test copies and outputs (`160500` DNG/upscales, `170000` JPEG/upscale); `accelerometer_rotation` restored to 1. No reboot, no git commit.
