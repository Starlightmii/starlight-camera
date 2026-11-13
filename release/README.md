# 📷 Starlight Camera vMax

A heavily modified **LMC 8.4 Google Camera (GCam)** port for the
**Xiaomi Redmi Note 10 Pro Max (`sweetin` / `sweet`)** — rebranded
**Starlight Camera**, with a ColorOS 17 glass UI, on-device photo/video
super-resolution, smart content-aware naming, and a tuned Canon EOS R10 DSLR
library.

Current build: **.51-vMax** · `versionCode 64764924` ·
`8.4.300.414775575.51-vMax` · package `com.google.android.GoogleCameraLMCR17`.

---

## ✨ Highlights

- 🏷️ **Starlight branding** — splash, settings, footer and file naming; no
  LMC/Hasli art in the UI.
- 🧠 **Smart auto-naming** — `Person_ / Selfie_ / Laptop_ / Macro_ / Wide_ /
  Night_ / 108MP_ / B&W_ / Video_ / Timelapse_ …_Starlight`.
- ⬆️ **On-device photo upscale** — GPU tiled readback + Lanczos3 fallback,
  Tensei colour matrix, edge-gated unsharp, JPEG q98, 24 MP default / 40 MP
  max. **.51 fixes DNG orientation**: TIFF IFD0 orientation is read directly
  when `ExifInterface` returns 0, so portrait DNGs no longer come out rotated.
- 🎬 **Video super-resolution** — hardware MediaCodec decode/encode, Gemini
  two-pass GL (temporal + chroma bilateral, then directional de-blur +
  dual-band + IGN), BT.709/full/SDR forced, AAC passthrough.
- 🎛️ **Full-range sliders** — Denoise / Detail / Sharpen; *Off* is truly zero.
- 🥔 **Potato mode** — 16 MP photos, 1080p30 video, lower bitrate/sharpen.
- 🪟 **Glass UI** — ColorOS 17-style liquid glass, live CPU/GPU/MEM/BAT capsule.
- 🎞️ **Video** — 4K30 HEVC, 1080p60, time-lapse, slow motion.
- 📊 **Diagnostics** — rolling `starlight/starlight.log` + exit-reason dump.

---

## 📦 What's included

| File | What it is |
|---|---|
| `apk/Starlight-Camera-vMax.51.apk` | The camera — install this |
| `config/Starlight_Master.xml` | Master config (load in LMC Configs) |
| `libs/Starlight_EOS_R10_DSLR.so` | Starlight-branded Canon EOS R10 DSLR lib (active) |
| `libs/Canon_EOS_R10_DSLR🕊️.so` | Original Canon EOS R10 DSLR lib (reference / fallback) |
| `src/` | Java sources for all Starlight classes |
| `smali/` | Patched smali for the .51 orientation fix |
| `patches/` | v2 → vMax diffs |
| `reports/` | STATE, REPORT, TENSEI_ANALYSIS, PROBLEM_STATEMENT |
| `suggestions/` | LLM design reviews (DeepSeek, Gemini, Claude) |
| `screenshots/` | Device screenshots and sample outputs |
| `fix/` | Starlight Ports fix folder (manifest + README + payload) |
| `CHECKSUMS.sha256` | SHA-256 for every release asset |

---

## 📥 Install

1. Uninstall any existing build (the signer is the same, so `-r` also works):
   ```bash
   adb uninstall com.google.android.GoogleCameraLMCR17
   ```
2. Install:
   ```bash
   adb install apk/Starlight-Camera-vMax.51.apk
   ```
3. Push the config (optional but recommended):
   ```bash
   adb push config/Starlight_Master.xml /sdcard/StarlightCamera/Starlight_Master.xml
   adb push config/Starlight_Master.xml /sdcard/LMC8.4/Starlight_Master.xml
   ```
   Load it in the app: double-tap the black area near the shutter →
   **IMPORT** → pick `Starlight_Master.xml`. The app restarts.
4. Copy the libs (optional):
   ```bash
   adb push libs/Starlight_EOS_R10_DSLR.so /sdcard/StarlightCamera/libs/
   adb push libs/Canon_EOS_R10_DSLR🕊️.so   /sdcard/StarlightCamera/libs/
   ```
   Select the lib in **Settings → Libs** (`libs_key = -1`,
   `pref_libs_fromdir_key = Starlight_EOS_R10_DSLR.so`).
5. Open the camera, grant permissions, and set the three sliders to taste.

### Upscale a photo

Long-press the **scale/upscale chip** at the bottom-left (portrait). It
processes the newest photo; output lands in
`/sdcard/Pictures/Raw/Upscaled_<stem>.jpg`.

### Upscale a video

Set the video tier, record, then long-press the same chip in video mode — or:
```bash
adb shell am broadcast -n com.google.android.GoogleCameraLMCR17/com.starlight.camera.StarlightVideoReceiver \
  -a com.starlight.camera.VIDEO_SR --es path /sdcard/DCIM/Camera/<file>.mp4
```

---

## ✅ Verified on device (sweetin, Android 17 / ColorOS 17)

- **Photo orientation (.51)**: orientation-6 DNG 2000x1500 → `Upscaled_…jpg`
  **2968x3968**, EXIF Orientation 1, upright and sharper than source.
- **JPEG regression**: 3000x4000 orientation-1 JPEG → 4242x5656, Orientation 1.
- **Photo 24 MP**: 5657x4243, no seams; source removed when auto-upscale is on.
- **Photo 40 MP**: 7302x5476, no seams.
- **Potato photo**: 3464x4618 (16 MP), 4.2 MB.
- **Video SR**: 4K HEVC 30 fps, BT.709 space/transfer/primaries, AAC 48 kHz
  stereo; 30/30/30 frames decoded/rendered/encoded.
- **Slider A/B**: Max vs Off SSIM 0.9601, PSNR 31.73 dB; Laplacian variance
  510 / 869 / 1543 (Off / Default / Max).
- **Preview**: thumbnail opens
  `com.google.android.apps.photos/.pager.HostPhotoPagerActivity`.

---

## ⚠️ Notes & limits

- Tested only on **sweetin** with **ColorOS 17 / Android 17**. Not a
  Magisk/KernelSU module — it is a normal APK.
- No ML super-resolution model: upscaling is GPU Lanczos3 + sharpening, not
  generative AI.
- Video is 8-bit SDR; HDR/10-bit sources are skipped.
- The package name intentionally remains
  `com.google.android.GoogleCameraLMCR17` for config/lib compatibility.
- Photo jobs run in the app process; only video SR uses the `:sr` process.
- Long thermal soaks and 10-bit video are untested.

---

## 🙏 Credits

- **Google** — Google Camera / Camera2 / HDR+ pipeline (original work).
- **Hasli (LMC)** — [LMC 8.4](https://www.celsoazevedo.com/files/android/google-camera/dev-hasli/)
  GCam base, config system, lib patcher. This project would not exist without
  LMC.
- **Prashant** — `Canon_EOS_R10_DSLR` lib base used by the Starlight lib.
- **Tensei (@maxmaxim59)** — colour-matrix and pipeline reference work that the
  default photo path and v2.0 baseline are modelled on.
- **GCam community** — modding tools, config format and testing notes.
- **DeepSeek, Gemini, Claude** — design reviews, shader/pipeline suggestions
  and debugging notes collected in `suggestions/`.
- **Starlightmii** — Starlight branding, upscale pipeline, video SR, UI,
  config, testing and packaging.

LMC 8.4 is the work of its authors; this is an unofficial, independent mod.
Please support the original GCam/LMC developers.

---

## 🔐 Checksums

See `CHECKSUMS.sha256`. APK:
```
cda6344ae47b87fa76c3629e1b7046cba547d1ec84911b79ae94a78c8a2176e8  Starlight-Camera-vMax.51.apk
```
