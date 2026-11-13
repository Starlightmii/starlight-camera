package com.starlight.camera;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Experimental video super-resolution.
 *
 * Proof of concept: sample the latest video at 10 fps (first 6 seconds),
 * upscale each frame with the StarlightSR bitmap pipeline (RenderScript
 * bicubic + color matrix + edge-gated unsharp), and re-encode to H.264 at
 * 80 Mbps. Audio is not muxed in this PoC. The output is
 * Upscaled_<name>.mp4 next to the source.
 */
public final class StarlightVideoSR {
    public static final String TAG = "StarlightVideoSR";
    public static final String ACTION = "com.starlight.camera.VIDEO_SR";
    private static final int DEFAULT_FPS = 24;
    private static final int DEFAULT_MAX_FRAMES = 0;
    private static final long MAX_PIXELS = 8300000L; // 4K-ish ceiling
    private static final int BITRATE = 100000000;
    private static volatile boolean sProcessing = false;
    private static volatile int sProgress = 0;

    public static boolean isProcessing() {
        return sProcessing;
    }

    public static int progress() {
        return sProgress;
    }

    static void setProcessing(boolean value) {
        sProcessing = value;
    }

    static void setProgress(int value) {
        sProgress = Math.max(0, Math.min(100, value));
    }

    private StarlightVideoSR() {
    }

    public static void processLatest(final Context context) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                File latest = latestVideo(app);
                if (latest == null) {
                    toast(app, "Video SR: no video found");
                    return;
                }
                toast(app, "Video SR: processing " + latest.getName());
                boolean ok = process(app, latest, 2, DEFAULT_FPS, DEFAULT_MAX_FRAMES);
                toast(app, ok ? "Video SR saved" : "Video SR failed (see logcat)");
            }
        }, "starlight-video-sr").start();
    }

    public static File latestVideo(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"_data", "_display_name"},
                    null, null, "date_added DESC");
            while (cursor != null && cursor.moveToNext()) {
                String path = cursor.getString(0);
                String name = cursor.getString(1);
                if (path == null || name == null) {
                    continue;
                }
                if (name.startsWith("Upscaled_") || name.contains("_Upscaled")) {
                    continue;
                }
                File file = new File(path);
                if (file.exists() && file.length() > 0 && canUpscale(file)) {
                    return file;
                }
            }
        } catch (Throwable th) {
            Log.w(TAG, "latestVideo: " + th);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean canUpscale(File file) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(file.getAbsolutePath());
            int w = Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int h = Integer.parseInt(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            return w > 0 && h > 0 && w < 3840 && h < 3840 && ((long) w) * h < MAX_PIXELS;
        } catch (Throwable th) {
            return false;
        } finally {
            try {
                r.release();
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean process(Context context, File src, int scale, int fps, int maxFrames) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        } catch (Throwable ignored) {
        }
        Log.i(TAG, "start transcode src=" + src.getName() + " scale=" + scale
                + " fps=" + fps + " maxFrames=" + maxFrames);
        StarlightJobService.start(context, "Upscaling video");
        try {
            return StarlightVideoTranscoder.process(context, src, scale, fps, maxFrames);
        } finally {
            StarlightJobService.stop(context);
        }
    }

    private static int autoScale(int srcW, int srcH, int requested) {
        int scale = Math.max(1, Math.min(4, requested));
        while (scale > 1) {
            long w = (long) srcW * scale;
            long h = (long) srcH * scale;
            if (w * h <= MAX_PIXELS && w <= 3840 && h <= 3840) {
                break;
            }
            scale--;
        }
        return scale;
    }

    private static int findAudioTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            String mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    private static void copyAudio(MuxState state) {
        if (state.audioExtractor == null || state.audioTrack < 0 || !state.started) {
            return;
        }
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (true) {
                int size = state.audioExtractor.readSampleData(buffer, 0);
                if (size < 0) {
                    break;
                }
                info.offset = 0;
                info.size = size;
                info.presentationTimeUs = state.audioExtractor.getSampleTime();
                info.flags = state.audioExtractor.getSampleFlags();
                state.muxer.writeSampleData(state.audioTrack, buffer, info);
                state.audioExtractor.advance();
            }
        } catch (Throwable th) {
            Log.w(TAG, "audio copy: " + th);
        }
    }

    private static boolean queueFrame(MediaCodec codec, Bitmap bitmap, long ptsUs) {
        int index = -1;
        for (int attempt = 0; attempt < 60 && index < 0; attempt++) {
            index = codec.dequeueInputBuffer(20000L);
            if (index < 0) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (index < 0) {
            return false;
        }
        Image image = codec.getInputImage(index);
        if (image == null) {
            Log.w(TAG, "no input image");
            codec.queueInputBuffer(index, 0, 0, ptsUs, 0);
            return false;
        }
        fillYuv(image, bitmap);
        codec.queueInputBuffer(index, 0, image.getWidth() * image.getHeight() * 3 / 2, ptsUs, 0);
        return true;
    }

    private static void queueEos(MediaCodec codec, long ptsUs) {
        try {
            int index = codec.dequeueInputBuffer(50000L);
            if (index >= 0) {
                codec.queueInputBuffer(index, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
        } catch (Throwable th) {
            Log.w(TAG, "eos: " + th);
        }
    }

    private static void drain(MediaCodec codec, MediaMuxer muxer, MuxState state, boolean eos) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int guard = 0;
        while (guard++ < 1024) {
            int index = codec.dequeueOutputBuffer(info, eos ? 10000L : 0L);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!state.started) {
                    state.track = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    state.started = true;
                }
            } else if (index >= 0) {
                ByteBuffer buffer = codec.getOutputBuffer(index);
                if (buffer != null && info.size > 0 && state.started) {
                    buffer.position(info.offset);
                    buffer.limit(info.offset + info.size);
                    muxer.writeSampleData(state.track, buffer, info);
                }
                codec.releaseOutputBuffer(index, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!eos) {
                    break;
                }
            }
        }
    }

    private static void fillYuv(Image image, Bitmap bitmap) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = new int[width * height];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();
        int yRow = planes[0].getRowStride();
        int yPix = planes[0].getPixelStride();
        int uRow = planes[1].getRowStride();
        int uPix = planes[1].getPixelStride();
        int vRow = planes[2].getRowStride();
        int vPix = planes[2].getPixelStride();

        for (int y = 0; y < height; y++) {
            int base = y * width;
            for (int x = 0; x < width; x++) {
                int p = argb[base + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                int yv = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                yBuf.put(y * yRow + x * yPix, (byte) (yv < 0 ? 0 : (yv > 255 ? 255 : yv)));
            }
        }
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width / 2; x++) {
                int p0 = argb[(y * 2) * width + x * 2];
                int p1 = argb[(y * 2) * width + x * 2 + 1];
                int p2 = argb[(y * 2 + 1) * width + x * 2];
                int p3 = argb[(y * 2 + 1) * width + x * 2 + 1];
                int r = (((p0 >> 16) & 0xFF) + ((p1 >> 16) & 0xFF) + ((p2 >> 16) & 0xFF) + ((p3 >> 16) & 0xFF)) >> 2;
                int g = (((p0 >> 8) & 0xFF) + ((p1 >> 8) & 0xFF) + ((p2 >> 8) & 0xFF) + ((p3 >> 8) & 0xFF)) >> 2;
                int b = ((p0 & 0xFF) + (p1 & 0xFF) + (p2 & 0xFF) + (p3 & 0xFF)) >> 2;
                int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                uBuf.put(y * uRow + x * uPix, (byte) (u < 0 ? 0 : (u > 255 ? 255 : u)));
                vBuf.put(y * vRow + x * vPix, (byte) (v < 0 ? 0 : (v > 255 ? 255 : v)));
            }
        }
    }

    private static Bitmap rotate(Bitmap bitmap, int degrees) {
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Throwable th) {
            Log.w(TAG, "rotate: " + th);
            return bitmap;
        }
    }

    private static int even(int v) {
        return v & ~1;
    }

    private static File nextOutput(File src) {
        File parent = src.getParentFile();
        String name = src.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replace("_Starlight", "").replace("_Upscaled", "");
        File dst = new File(parent, "Upscaled_" + name + ".mp4");
        int n = 1;
        while (dst.exists()) {
            dst = new File(parent, "Upscaled_" + name + "_" + n + ".mp4");
            n++;
        }
        return dst;
    }

    private static void toast(final Context context, final String text) {
        Log.i(TAG, text);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static final class MuxState {
        int track = -1;
        int audioTrack = -1;
        boolean started = false;
        MediaExtractor audioExtractor;
        MediaMuxer muxer;
    }
}
