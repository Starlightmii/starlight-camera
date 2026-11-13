# Tensei upscaler — reverse-engineering analysis

Task: port the "Tensei" upscaler into Starlight Camera (LMC 8.4 R17,
`com.google.android.GoogleCameraLMCR17`), fix auto-upscale, and ship a signed
APK.

## Bottom line

Tensei is **not an AI/ML upscaler**. It contains no model file, no TFLite /
ONNX / NCNN / MNN runtime, and no native inference library. Its entire "enhance"
path is Android graphics:

1. Decode the source bitmap.
2. Cap the output at 24,000,000 pixels.
3. Draw the source into a larger bitmap with `Canvas.drawBitmap()` and
   `Paint.FILTER_BITMAP_FLAG` (bilinear filtering, despite the "AI" marketing).
4. Apply a `ColorMatrix`: saturation 1.15, then contrast 1.15 with
   `translate = (-0.5 * 1.15 + 0.5) * 255 = -19.125`.
5. Run `RenderScript ScriptIntrinsicConvolve3x3` with the cross kernel
   `[0, -1, 0, -1, 5, -1, 0, -1, 0]`.
6. Compress JPEG at quality 100 and save through MediaStore.
7. Retry up to 3 times with a halved pixel cap on OOM.

The "Tensei" result therefore comes from a mild 5-tap sharpen, exact
saturation/contrast, and q100 JPEG — not from a neural network.

## Evidence

| Item | Finding |
|---|---|
| APK | `/tmp/opencode/tensei_work/tensei-upscale.apk` |
| Package | `com.tensei.upscale`, versionCode 1, versionName 1.0 |
| SDK | minSdk 24, targetSdk 36 |
| Native libs | only `libandroidx.graphics.path.so` for 4 ABIs; no ML runtime |
| Model assets | none; only `DebugProbesKt.bin` (Kotlin debug probes) |
| Decompiled source | `/tmp/opencode/tensei_port/tensei_src/sources/com/tensei/upscale/ImageProcessor.java` |
| Bytecode proof | `/tmp/opencode/tensei_work/enhance.asm` |
| Previous LMC source | `/tmp/opencode/tensei_work/prov_src/sources/com/starlight/camera/StarlightProvider.java` |

`ImageProcessor.kt` was compiled into `classes4.dex`. JADX hits an overflow on
the large `invokeSuspend` method, so `enhance.asm` is the authoritative
instruction dump for the pipeline constants.

## Why the previous build looked worse

The v32/v35 Starlight path used a stronger full 3x3 kernel
(`-0.22 / 1.88`) and q96 JPEG. That kernel sharpens more aggressively, which
produces visible halos and amplifies noise. Tensei's cross kernel only touches
the four direct neighbours with `-1` and leaves diagonals alone, and q100 avoids
extra JPEG ringing. The color matrix is also applied after resampling in both,
but the exact 1.15/1.15 values matter.

## v2 Starlight pipeline

Implemented in `StarlightSR.java`:

- **Same color matrix** as Tensei: saturation 1.15, contrast 1.15, translate
  `-19.125`.
- **Multi-step 2x resampling** for 3x/4x instead of one large step. This keeps
  more real detail than a single Canvas scale.
- **Edge-gated luma unsharp**: amount 0.35, threshold 3. Flat areas are left
  alone; only edges get a luma boost applied equally to R/G/B, so no colour
  fringes.
- **Banded processing**: 96-row bands with one-row overlap, so a 24 MP result
  does not need a second full-size copy.
- **EXIF orientation baked into pixels** before resampling.
- **JPEG q98** from the provider (`UPSCALE_JPEG_QUALITY = 98`); `StarlightSR`
  defaults to 98.
- **Atomic output**: `Upscaled_<stem>.jpg.tmp` then rename; MediaScanner scan.
- **24 MP cap**, OOM retry halves the cap up to 3 attempts.
- Skips files already named `Upscaled_*` or containing `_Upscaled`.

## What was deliberately not done

- No TFLite/NCNN model was added. The donor APK has no model, and adding one
  would require a runtime, a model licence, a large APK increase, and device
  testing that is currently impossible (device unreachable).
- No native `libtensorflowlite` or `libncnn` was bundled.
- The old unused `bicubicUpscale` method remains in `StarlightProvider` for
  source compatibility but is no longer called.

## Limitations

- Canvas filtering is bilinear, not bicubic. Calling it "bicubic" is inaccurate;
  Tensei itself uses the same filter flag.
- 12 MP source + 24 MP cap means the effective 2x factor is about 1.41x
  (`4242x5656`), not a true 2x. The UI says "2x-4x" and the provider logs the
  real output size.
- 108 MP (9000x12000) sources exceed the 24 MP cap and are skipped by design.
- Output JPEG does not copy EXIF metadata; orientation is baked into pixels.

## vMax application (2026-11-12)

The vMax default photo path is the v2/Tensei pipeline unchanged. The opt-in
max path keeps the same colour matrix and edge-gated unsharp but replaces the
bicubic resample with Lanczos3 (RenderScript bicubic fallback), raises the cap
to 40 MP, and writes JPEG q100. The video path is new and is not part of the
Tensei app; it uses RenderScript/bicubic scaling plus the same colour stage.
No model, TFLite, ONNX, NCNN, MNN, Vulkan, or NDK path was added.
