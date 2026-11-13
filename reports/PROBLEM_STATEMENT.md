**PROBLEM & SUGGESTION STATEMENT — Starlight Camera vMax (LMC 8.4 R17 port)**

**1. Context**

I am working on a patched Google Camera / LMC 8.4 R17 APK for the Xiaomi Redmi Note 10 Pro Max (`sweetin`, SM7150 / Snapdragon 732G, Adreno 618, Android 13-based ColorOS 17 port). Package: `com.google.android.GoogleCameraLMCR17`.

The goal is a “Starlight Camera vMax” build with:

- Photo upscaling: default 2x / 24 MP / JPEG 98, and experimental max 2x / 40 MP / JPEG 100.
- Automatic video upscaling: 1080p → 4K, 24 fps, H.264, audio preserved, output visible in the gallery.
- Preview thumbnail tap opens the user’s configured media app (ColorOS Gallery or Google Photos).
- A small animated rainbow processing bar at the bottom, below the mode labels, instead of the previous large red ring or red dot.
- Maximum GPU use, minimal CPU use, and acceptable behavior on a mid-range/low-end device.

The app is built by decompiling with apktool, compiling added Java sources separately, inserting them as `classes4.dex`, rebuilding, aligning, and signing. The current installed test build is versionCode `64764897`, versionName `8.4.300.414775575.24-vMax`, SHA-256 `f0638f78023d4047df552ae9cb7ae4ad8f3d798533b311a6673c5064739f89f2`.

**2. Current implementation**

**Photo path**

- `StarlightSR` performs 2x upscaling, color matrix, and edge-gated unsharp.
- A new `StarlightGL` class creates an offscreen EGL/GLES2 context, renders a bicubic/bilinear resize plus color/sharpen shader into an FBO, reads back with `glReadPixels`, and returns a Bitmap.
- RenderScript bicubic and CPU Lanczos remain as fallbacks.
- A recent selfie upscale succeeded, so the offscreen GL path can work at least for photos, but performance and memory are not yet characterized.

**Video path**

Three approaches have been tried:

1. `MediaMetadataRetriever.getFrameAtTime()` → Bitmap → RenderScript/CPU upscale → `MediaCodec` input `Image` → manual RGBA-to-YUV `fillYuv()` → `MediaMuxer`.
2. Same, but with the offscreen `StarlightGL` FBO shader and `glReadPixels` before `fillYuv()`.
3. Newest, currently installed but unverified: `MediaCodec.createInputSurface()` + `StarlightGL.renderToSurface()` using an EGL window surface, so frames render directly into the encoder surface with no `glReadPixels` and no `fillYuv()`. Buffer mode remains as fallback.

The video output is written next to the source as `Upscaled_<name>.mp4`, then `MediaScannerConnection.scanFile()` is called.

**Observed failures**

- Before the GL changes, the buffer-mode video path produced valid outputs (for example 26.9 MB and 18.5 MB files, MediaStore-visible, 3840×2160).
- After adding the GL FBO path, video tests produced 0-byte or 3,232-byte files.
- One test log showed:
  `StarlightVideoSR: first 1920x1080 out 3840x2160 frames=91 fps=24`
  even though the source clip was reported by `ffprobe` as 1.47 s / 34 frames. That frame-count mismatch is unexplained.
- The camera app then hit an ANR: “Input dispatching timed out… Waited 5000ms for MotionEvent”, and Android killed the process. ANR traces are inaccessible because the device is a production build with no root.
- The latest surface-render build was installed, but the verification command was aborted before results were collected.
- The rainbow bar has not yet been visually verified on-device.

**3. Questions**

**A. Video pipeline architecture and correctness**

1. Is the correct long-term design to abandon `MediaMetadataRetriever.getFrameAtTime()` entirely and build a proper `MediaExtractor` → `MediaCodec` decoder → `SurfaceTexture`/external OES texture → GL shader → `MediaCodec` encoder input surface pipeline? If so, what is the most robust synchronous implementation for a single background thread on Android 13, including decoder output format handling, `SurfaceTexture.OnFrameAvailableListener`, `updateTexImage()`, and preserving source presentation timestamps?

2. Why would a 1.47 s / 34-frame source produce `frames=91` in my code? Is this likely a metadata duration mismatch, a rotation issue, a `getFrameAtTime()` behavior, or a bug in `frameCount = min(720, durationUs * fps / 1_000_000)`? What is the correct way to derive output frame count and duration from the actual decoded stream rather than metadata?

3. Why did the GL FBO + `glReadPixels` + `fillYuv()` path produce 3,232-byte files while the earlier RenderScript/CPU path produced valid files? What are the most likely failure points: `GLUtils.texImage2D`/`texSubImage2D`, FBO completeness, `glReadPixels` format/stride, `Bitmap.copyPixelsFromBuffer`, `MediaCodec.getInputImage()` returning null, YUV plane strides, or encoder input-buffer starvation?

4. Is rendering directly to `MediaCodec.createInputSurface()` the right fix for both CPU load and ANR risk? Please review the lifecycle: `configure()` with `COLOR_FormatSurface`, `createInputSurface()`, `start()`, `eglCreateWindowSurface()`, `eglMakeCurrent()`, `eglPresentationTimeANDROID()`, `eglSwapBuffers()`, `signalEndOfInputStream()`, then drain to EOS. What ordering or EGL configuration mistakes commonly break this on Adreno?

5. Should the EGL config include `EGL_RECORDABLE_ANDROID` when the window surface belongs to a `MediaCodec` encoder? Are there additional requirements such as `EGL_WINDOW_BIT`, RGB888, or avoiding pbuffer-only configs? I have already changed the config to request both `EGL_PBUFFER_BIT` and `EGL_WINDOW_BIT`.

6. How should audio be handled? Current code copies the source audio track into the muxer after video draining. Is that safe when video is rendered through an input surface, and how should I guarantee A/V sync and correct duration when the video encoder’s PTS starts at zero but audio PTS may not?

**B. GPU, CPU, and performance**

7. The user reports “CPU maxed, GPU barely used” during processing. RenderScript is deprecated on Android 12+ and may be CPU-only on this device. Is the GLES2 shader path the right replacement, or should I use GLES3 with PBO async readback for photos and a pure surface path for video? What is the best GPU strategy for Adreno 618?

8. What is the likely cause of the camera ANR during video processing: GPU saturation from the 4K shader, CPU decode via `MediaMetadataRetriever`, 33 MB `glReadPixels` per frame, `fillYuv()` allocating a 33 MB int array per frame, main-thread overlay updates, or something else? Would moving video SR into a separate `:sr` process or foreground service materially reduce camera ANR risk?

9. What performance targets are realistic for this device? For 1080p → 4K at 24 fps with H.264, is 4K output feasible in real time or near-real time, or should video output be capped at 1440p / 1080p? What bitrate, codec (H.264 vs HEVC), and frame-rate policy would you recommend to balance quality, thermals, storage, and encoder stability?

10. Should video processing use `Process.THREAD_PRIORITY_BACKGROUND`, a single-thread executor, or a foreground service with a user-visible notification? How do I keep the camera UI responsive while still showing the rainbow progress bar?

**C. Upscaling algorithm and image quality**

11. For a mid-range mobile device, is a GLES bicubic/Catmull-Rom shader plus edge-gated unsharp the right quality/performance tradeoff, or should I use Lanczos in a shader, a separable two-pass approach, or a lightweight learned model? Are ESRGAN/Real-ESRGAN/NCNN-style models realistic here without NPU access, or would they be too slow and too large?

12. Is my shader’s bicubic sampling formula correct for arbitrary scale factors and non-power-of-two textures? It computes four cubic weights, samples four texels, and blends with `mix()`. What edge cases should I test: very small source, 90°/270° rotation, odd dimensions, and 4K readback?

13. The shader currently applies saturation `mix(luma, rgb, 1.15)`, contrast `rgb * 1.15 - 19.125/255`, and edge-gated unsharp with strength 0.35 for video and 0.45 for photos. Is this appropriate for video in BT.709/sRGB, or will it cause clipping, halos, or color shifts? Should video use a milder matrix or no sharpening at all?

**D. Video save path, MediaStore, and gallery visibility**

14. What is the most reliable Android 13 save path for a camera app that writes its own MP4: direct file in `DCIM/Camera` plus `MediaScannerConnection.scanFile()`, or `MediaStore.Video.Media.insert()` with `RELATIVE_PATH` and an output stream? The app currently writes directly and scans; it worked once, but I am concerned about ownership, scoped storage, duplicate entries, and gallery refresh.

15. Should I write to a temporary file such as `.starlight_tmp.mp4`, finalize the muxer, then atomically rename and scan? How do I ensure failed or killed jobs do not leave 0-byte/3,232-byte files visible in the gallery? What cleanup policy do you recommend for stale temp files?

16. For long videos, current code caps processing at `DEFAULT_MAX_FRAMES = 720` (30 s at 24 fps). Is a 30-second cap acceptable, or should the app process the full clip with progress/cancel? If full-length, how should I handle storage, thermals, and background execution limits?

**E. Preview media opener and UI behavior**

17. The current preview fix installs a touch interceptor on the camera view hierarchy and re-applies it periodically so a tap opens the configured media app (`ACTION_VIEW`, ColorOS Gallery `ViewGallery`). This works, but it risks stealing taps from the shutter, mode switcher, or preview gestures. Is there a safer, more native hook: replace the thumbnail click listener, use the existing preview `View`’s `onClick`, or launch the media app from the camera’s own review activity? How would you design the behavior so it is predictable and accessible?

18. Should the media opener use the latest `MediaStore` item filtered to `BUCKET_DISPLAY_NAME = "Camera"`, or the exact file just captured? What fallback should appear if the configured app is missing or cannot handle the MIME type? Should the app offer a chooser instead of silently falling back?

19. The rainbow bar is a custom `View` added to the camera `FrameLayout` with `Gravity.BOTTOM`, height 4 dp, left/right margin 24 dp, bottom margin 38 dp, and an animated `LinearGradient` with `postInvalidateOnAnimation()`. Is this the right place and animation strategy? How do I ensure it sits below the mode labels on different aspect ratios, does not block touches, and remains visible during background processing when the camera UI is not in the foreground?

**F. Robustness, testing, and low-end policy**

20. Without root or ANR trace access, what is the best way to diagnose the ANR and process death? Are `dumpsys activity lastanr`, DropBoxManager, `adb bugreport`, `dumpsys media.codec`, `dumpsys SurfaceFlinger`, and GPU busy counters sufficient? What specific logs should I capture for EGL/MediaCodec/Adreno failures?

21. What fallback policy should the app expose on low-end devices: automatic downgrade to 1440p, 1080p, lower fps, H.264 instead of HEVC, disabling sharpening, or disabling video SR entirely when thermals/storage are bad? Should this be automatic, user-selectable, or both?

**4. My proposed direction**

- Replace the video path with a fully hardware-oriented pipeline: `MediaExtractor` + `MediaCodec` decoder → `SurfaceTexture` → GLES shader → `MediaCodec` encoder input surface. Preserve source PTS; do not use `MediaMetadataRetriever` per frame.
- Keep the GLES surface path for video, and use GLES FBO/PBO readback only for still photos. Treat RenderScript as a legacy fallback, not the primary path.
- Write video to a temporary file, finalize, rename atomically, then insert/scan through MediaStore. Delete failed/temp outputs aggressively.
- Cap video output adaptively: 4K only for short clips and good thermal state; otherwise 1440p or 1080p. Use 24 fps, H.264 for compatibility, and a bitrate in the 40–60 Mbps range unless testing proves 100 Mbps is stable.
- Move video SR into a separate process or foreground service so camera UI/ANR behavior is isolated. Keep the rainbow bar as a lightweight progress indicator driven by a binder/notification/state file.
- Replace the global preview touch interceptor with a thumbnail-only click hook if possible; otherwise constrain the hit area and preserve shutter/mode gestures.
- Add a test matrix: 1–2 s 1080p clip, rotated clip, no-audio clip, 30 s clip, low-storage case, and a 4K photo. Verify with `ffprobe` (duration, frame count, audio stream), MediaStore row, gallery visibility, and ANR/process-death logs.

**5. What I need from you**

Please answer the numbered questions with concrete Android API-level guidance, identify any incorrect assumptions, and prioritize the fixes. I am especially unsure about the correct decoder-to-encoder surface pipeline, EGL configuration for `MediaCodec` input surfaces, why the GL FBO path regressed from valid MP4s to 3,232-byte files, and how to prevent camera ANRs while still using maximum GPU.
