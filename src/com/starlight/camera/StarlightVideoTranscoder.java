package com.starlight.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Hardware transcode pipeline for Starlight video SR:
 *
 *   MediaExtractor -> MediaCodec decoder -> SurfaceTexture/OES
 *   -> StarlightVideoGL (Catmull-Rom + colour + sharpen)
 *   -> MediaCodec encoder input Surface -> MediaMuxer -> MediaStore
 *
 * The old MediaMetadataRetriever/getFrameAtTime + glReadPixels + fillYuv
 * path is intentionally not used for video anymore.
 */
final class StarlightVideoTranscoder {
    private static final String TAG = "StarlightVideoSR";
    private static final long MAX_PIXELS = 8300000L;
    private static final int MAX_OUT_EDGE = 3840;

    private StarlightVideoTranscoder() {
    }

    static boolean process(Context context, File src, int scale, int fps, int maxFrames) {
        StarlightVideoSR.setProcessing(true);
        StarlightVideoSR.setProgress(0);
        try {
            return run(context, src, scale, fps, maxFrames);
        } catch (Throwable th) {
            Log.e(TAG, "job failed", th);
            StarlightLog.e("video failed " + src.getName(), th);
            return false;
        } finally {
            StarlightVideoSR.setProcessing(false);
            StarlightVideoSR.setProgress(0);
            StarlightProgressNotifier.stop(context);
        }
    }

    private static boolean run(Context context, File src, int scale, int fps, int maxFrames) throws Exception {
        MediaExtractor vx = null;
        MediaExtractor ax = null;
        MediaCodec dec = null;
        MediaCodec enc = null;
        Surface encInput = null;
        StarlightVideoGL gl = null;
        Sink sink = null;
        boolean success = false;
        try {
            vx = new MediaExtractor();
            vx.setDataSource(src.getAbsolutePath());
            int vt = firstTrack(vx, "video/");
            if (vt < 0) {
                throw new IOException("no video track");
            }
            vx.selectTrack(vt);
            MediaFormat vf = vx.getTrackFormat(vt);
            if (isHdr(vf)) {
                Log.w(TAG, "HDR/10-bit source skipped: " + src.getName());
                return false;
            }
            int srcW = vf.getInteger(MediaFormat.KEY_WIDTH);
            int srcH = vf.getInteger(MediaFormat.KEY_HEIGHT);
            int rotation = vf.containsKey(MediaFormat.KEY_ROTATION)
                    ? vf.getInteger(MediaFormat.KEY_ROTATION) : 0;
            long durationUs = vf.containsKey(MediaFormat.KEY_DURATION)
                    ? vf.getLong(MediaFormat.KEY_DURATION) : 0L;
            if ("off".equals(StarlightPolicy.videoTier(context))) {
                StarlightLog.job("video tier off");
                return false;
            }
            int srcFps = vf.containsKey(MediaFormat.KEY_FRAME_RATE)
                    ? vf.getInteger(MediaFormat.KEY_FRAME_RATE) : fps;
            if (srcFps <= 0) {
                srcFps = 30;
            }
            srcFps = StarlightPolicy.videoFps(context, srcFps);
            int requestedEdge = StarlightPolicy.videoMaxEdge(context);
            StarlightCapabilities.Probe caps = StarlightCapabilities.probe(
                    context, MediaFormat.MIMETYPE_VIDEO_AVC, requestedEdge, srcFps);
            int maxEdge = Math.min(requestedEdge, caps.maxEdge);
            int[] target = targetSize(srcW, srcH, scale, maxEdge);
            int outW = target[0];
            int outH = target[1];
            if (outW <= srcW || outH <= srcH) {
                Log.w(TAG, "already at ceiling " + srcW + "x" + srcH);
                return false;
            }
            int bitrate = (int) Math.min(40000000L,
                    Math.max(8000000L, ((long) outW) * outH * srcFps * 17L / 100L));
            if (maxEdge <= 1920) {
                bitrate = Math.min(bitrate, 12000000);
            } else if (maxEdge <= 2560) {
                bitrate = Math.min(bitrate, 20000000);
            }
            if (StarlightPolicy.potato(context)) {
                bitrate = Math.min(bitrate, 10000000);
            }
            checkHeadroom(context, src, bitrate, durationUs);
            Log.i(TAG, "src=" + src.getName() + " " + srcW + "x" + srcH + " rot=" + rotation
                    + " durUs=" + durationUs + " -> " + outW + "x" + outH + " @" + srcFps
                    + " bitrate=" + bitrate);
            StarlightLog.job("video start " + src.getName() + " " + srcW + "x" + srcH
                    + " -> " + outW + "x" + outH + " @" + srcFps);

            MediaFormat ef = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outW, outH);
            ef.setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            ef.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            ef.setInteger(MediaFormat.KEY_FRAME_RATE, srcFps);
            ef.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            try {
                ef.setInteger(MediaFormat.KEY_BITRATE_MODE,
                        android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            } catch (Throwable ignored) {
            }
            try {
                ef.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
            } catch (Throwable ignored) {
            }
            copyColor(vf, ef);
            try {
                ef.setInteger(MediaFormat.KEY_PRIORITY, 1);
            } catch (Throwable ignored) {
            }

            sink = Sink.create(context, src);
            sink.rotation = rotation;

            enc = createCodec(MediaFormat.MIMETYPE_VIDEO_AVC, true);
            configureEncoder(enc, ef);
            encInput = enc.createInputSurface();
            gl = StarlightVideoGL.create(encInput, srcW, srcH, outW, outH);
            if (gl == null) {
                throw new IOException("video GL init failed");
            }
            enc.start();

            dec = createCodec(vf.getString(MediaFormat.KEY_MIME), false);
            try {
                vf.setInteger(MediaFormat.KEY_ROTATION, 0);
                vf.setInteger(MediaFormat.KEY_PRIORITY, 1);
            } catch (Throwable ignored) {
            }
            try {
                // Keep every decoded frame: a non-View SurfaceTexture is a
                // slow consumer and API 29+ may otherwise drop silently.
                vf.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 0);
            } catch (Throwable ignored) {
            }
            try {
                vf.setInteger(MediaFormat.KEY_OPERATING_RATE, 60);
            } catch (Throwable ignored) {
            }
            dec.configure(vf, gl.getInputSurface(), null, 0);
            dec.start();

            ax = new MediaExtractor();
            ax.setDataSource(src.getAbsolutePath());
            int at = firstTrack(ax, "audio/");
            ByteBuffer audioBuffer = null;
            if (at >= 0) {
                ax.selectTrack(at);
                sink.audioFormat = ax.getTrackFormat(at);
                int maxInput = sink.audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)
                        ? sink.audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) : 65536;
                audioBuffer = ByteBuffer.allocateDirect(Math.max(65536, maxInput));
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean decDone = false;
            int decoded = 0;
            int rendered = 0;
            long firstPts = -1L;
            long lastPts = -1L;
            int lastProgress = -1;
            long lastHeadroom = SystemClock.uptimeMillis();

            while (!decDone) {
                if (!inputDone) {
                    int ii = dec.dequeueInputBuffer(10000L);
                    if (ii >= 0) {
                        ByteBuffer ib = dec.getInputBuffer(ii);
                        int n = vx.readSampleData(ib, 0);
                        if (n < 0) {
                            dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            dec.queueInputBuffer(ii, 0, n, vx.getSampleTime(), 0);
                            vx.advance();
                        }
                    }
                }

                drainEncoder(enc, sink, false, info);

                int oi = dec.dequeueOutputBuffer(info, 10000L);
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "decoder out: " + dec.getOutputFormat());
                } else if (oi >= 0) {
                    boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    boolean hasFrame = !eos;
                    long ptsUs = info.presentationTimeUs;
                    dec.releaseOutputBuffer(oi, hasFrame);
                    if (hasFrame) {
                        decoded++;
                        if (!gl.awaitFrame(2500L)) {
                            throw new IOException("decoder frame timeout");
                        }
                        gl.updateTexImage();
                        if (ptsUs <= lastPts) {
                            ptsUs = lastPts + 1L;
                        }
                        drainEncoder(enc, sink, false, info);
                        if (!gl.renderFrame(ptsUs, StarlightPolicy.videoSharpen(context), 1.0f)) {
                            throw new IOException("GL render/swap failed");
                        }
                        rendered++;
                        if (firstPts < 0) {
                            firstPts = ptsUs;
                        }
                        lastPts = ptsUs;
                        drainEncoder(enc, sink, false, info);
                        pumpAudio(ax, sink, ptsUs + 500000L, audioBuffer);
                        if ((rendered % 30) == 0) {
                            int thermal = StarlightPolicy.thermalStatus(context);
                            if (thermal >= PowerManager.THERMAL_STATUS_CRITICAL) {
                                throw new IOException("thermal critical=" + thermal);
                            }
                            if (thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
                                StarlightLog.job("thermal severe pacing");
                                Thread.sleep(150L);
                            } else if (thermal >= PowerManager.THERMAL_STATUS_MODERATE) {
                                Thread.sleep(40L);
                            }
                            if (StarlightPolicy.powerSave(context)) {
                                Thread.sleep(25L);
                            }
                            long now = SystemClock.uptimeMillis();
                            if (now - lastHeadroom > 10000L) {
                                checkHeadroom(context, src, bitrate, durationUs);
                                lastHeadroom = now;
                            }
                        }
                        if (durationUs > 0) {
                            int pct = (int) Math.max(0L, Math.min(99L, ptsUs * 100L / durationUs));
                            if (pct != lastProgress) {
                                lastProgress = pct;
                                StarlightVideoSR.setProgress(pct);
                                StarlightProgressNotifier.update(context, "Starlight Video SR", pct);
                            }
                        }
                    }
                    if (eos) {
                        enc.signalEndOfInputStream();
                        decDone = true;
                    }
                }
            }

            drainEncoder(enc, sink, true, info);
            long frameUs = 1000000L / Math.max(1, srcFps);
            pumpAudio(ax, sink, lastPts + frameUs + 100000L, audioBuffer);
            StarlightVideoSR.setProgress(100);
            StarlightProgressNotifier.update(context, "Starlight Video SR", 100);
            if (rendered == 0 || sink.encoded == 0) {
                throw new IOException("no encoded frames");
            }
            if (sink.encoded < rendered - 2) {
                throw new IOException("encoder lost frames " + sink.encoded + "/" + rendered);
            }
            if (sink.size() < 65536L) {
                throw new IOException("output too small: " + sink.size());
            }
            sink.publish();
            success = true;
            Log.i(TAG, "done decoded=" + decoded + " rendered=" + rendered
                    + " encoded=" + sink.encoded + " firstPts=" + firstPts + " lastPts=" + lastPts
                    + " out=" + sink.describe());
            StarlightLog.job("video done decoded=" + decoded + " rendered=" + rendered
                    + " encoded=" + sink.encoded + " firstPts=" + firstPts
                    + " lastPts=" + lastPts + " out=" + sink.describe());
            return true;
        } finally {
            final MediaCodec decFinal = dec;
            final MediaCodec encFinal = enc;
            final MediaExtractor vxFinal = vx;
            final MediaExtractor axFinal = ax;
            quiet(new Runnable() { public void run() { if (decFinal != null) decFinal.stop(); } });
            quiet(new Runnable() { public void run() { if (decFinal != null) decFinal.release(); } });
            quiet(new Runnable() { public void run() { if (encFinal != null) encFinal.stop(); } });
            quiet(new Runnable() { public void run() { if (encFinal != null) encFinal.release(); } });
            if (gl != null) {
                gl.close();
            }
            if (encInput != null) {
                encInput.release();
            }
            if (sink != null) {
                if (success) {
                    sink.close();
                } else {
                    sink.fail();
                }
            }
            quiet(new Runnable() { public void run() { if (vxFinal != null) vxFinal.release(); } });
            quiet(new Runnable() { public void run() { if (axFinal != null) axFinal.release(); } });
        }
    }

    private static void drainEncoder(MediaCodec enc, Sink sink, boolean untilEos,
                                     MediaCodec.BufferInfo info) throws IOException {
        long deadline = SystemClock.uptimeMillis() + 15000L;
        int guard = 0;
        while (guard++ < 4096) {
            int index = enc.dequeueOutputBuffer(info, untilEos ? 10000L : 0L);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!untilEos) {
                    return;
                }
                if (SystemClock.uptimeMillis() > deadline) {
                    throw new IOException("encoder EOS timeout");
                }
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (sink.started) {
                    throw new IOException("encoder format changed twice");
                }
                sink.start(enc.getOutputFormat());
            } else if (index >= 0) {
                ByteBuffer buffer = enc.getOutputBuffer(index);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.size = 0;
                }
                if (info.size > 0) {
                    if (!sink.started) {
                        throw new IOException("sample before muxer start");
                    }
                    buffer.position(info.offset);
                    buffer.limit(info.offset + info.size);
                    sink.muxer.writeSampleData(sink.vTrack, buffer, info);
                    sink.encoded++;
                    sink.lastPtsUs = info.presentationTimeUs;
                }
                enc.releaseOutputBuffer(index, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    return;
                }
            }
        }
        throw new IOException("encoder drain guard exceeded");
    }

    private static void pumpAudio(MediaExtractor ax, Sink sink, long limitUs, ByteBuffer buffer) {
        if (ax == null || sink == null || sink.audioFormat == null || sink.aTrack < 0 || !sink.started) {
            return;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        try {
            while (true) {
                long time = ax.getSampleTime();
                if (time < 0 || time > limitUs) {
                    return;
                }
                buffer.clear();
                int size = ax.readSampleData(buffer, 0);
                if (size < 0) {
                    return;
                }
                int flags = (ax.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                        ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                info.set(0, size, time, flags);
                sink.muxer.writeSampleData(sink.aTrack, buffer, info);
                ax.advance();
            }
        } catch (Throwable th) {
            Log.w(TAG, "audio pump: " + th);
        }
    }

    private static int firstTrack(MediaExtractor extractor, String prefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            String mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHdr(MediaFormat format) {
        try {
            int standard = format.containsKey(MediaFormat.KEY_COLOR_STANDARD)
                    ? format.getInteger(MediaFormat.KEY_COLOR_STANDARD) : MediaFormat.COLOR_STANDARD_BT709;
            int transfer = format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)
                    ? format.getInteger(MediaFormat.KEY_COLOR_TRANSFER) : MediaFormat.COLOR_TRANSFER_SDR_VIDEO;
            return standard == MediaFormat.COLOR_STANDARD_BT2020
                    || transfer == MediaFormat.COLOR_TRANSFER_HLG
                    || transfer == MediaFormat.COLOR_TRANSFER_ST2084;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void checkHeadroom(Context context, File src, int bitrate, long durationUs)
            throws IOException {
        long estimate = (long) ((double) bitrate * (durationUs / 1000000.0d) / 8.0d * 1.1d);
        long need = Math.max(100L * 1024L * 1024L, estimate * 3L / 2L);
        File dir = src.getParentFile();
        if (dir != null) {
            try {
                StatFs stat = new StatFs(dir.getAbsolutePath());
                long free = stat.getAvailableBytes();
                StarlightLog.job("storage free=" + free + " need=" + need);
                if (free < need) {
                    throw new IOException("insufficient storage free=" + free + " need=" + need);
                }
            } catch (IOException ioe) {
                throw ioe;
            } catch (Throwable th) {
                Log.w(TAG, "storage check: " + th);
            }
        }
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= 29 && pm != null) {
                int thermal = pm.getCurrentThermalStatus();
                StarlightLog.job("thermal=" + thermal);
                if (thermal >= PowerManager.THERMAL_STATUS_CRITICAL) {
                    throw new IOException("thermal critical=" + thermal);
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable th) {
            Log.w(TAG, "thermal check: " + th);
        }
    }

    private static MediaCodec createCodec(String mime, boolean encoder) throws IOException {
        String name = findHardwareCodec(mime, encoder);
        if (name != null) {
            try {
                Log.i(TAG, (encoder ? "encoder=" : "decoder=") + name);
                StarlightLog.job((encoder ? "encoder=" : "decoder=") + name);
                return MediaCodec.createByCodecName(name);
            } catch (Throwable th) {
                Log.w(TAG, "createByCodecName " + name + ": " + th);
            }
        }
        if (encoder) {
            throw new IOException("no hardware encoder for " + mime);
        }
        return MediaCodec.createDecoderByType(mime);
    }

    private static String findHardwareCodec(String mime, boolean encoder) {
        try {
            MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            MediaCodecInfo best = null;
            int bestScore = -1;
            for (MediaCodecInfo info : list.getCodecInfos()) {
                if (info.isEncoder() != encoder) {
                    continue;
                }
                try {
                    info.getCapabilitiesForType(mime);
                } catch (Throwable th) {
                    continue;
                }
                boolean hardware = Build.VERSION.SDK_INT < 29 || info.isHardwareAccelerated();
                if (!hardware) {
                    continue;
                }
                String lower = info.getName().toLowerCase(Locale.US);
                int score = 0;
                if (lower.contains("qti") || lower.contains("qcom")) {
                    score += 4;
                }
                if (lower.contains("c2.")) {
                    score += 2;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = info;
                }
            }
            return best == null ? null : best.getName();
        } catch (Throwable th) {
            Log.w(TAG, "codec probe: " + th);
            return null;
        }
    }

    private static void configureEncoder(MediaCodec codec, MediaFormat format) throws IOException {
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return;
        } catch (Throwable first) {
            Log.w(TAG, "encoder configure full: " + first);
            try {
                codec.reset();
            } catch (Throwable ignored) {
            }
        }
        String[] drop = {
                MediaFormat.KEY_BITRATE_MODE,
                MediaFormat.KEY_MAX_B_FRAMES,
                MediaFormat.KEY_PRIORITY,
                MediaFormat.KEY_OPERATING_RATE,
                MediaFormat.KEY_COLOR_STANDARD,
                MediaFormat.KEY_COLOR_RANGE,
                MediaFormat.KEY_COLOR_TRANSFER
        };
        for (String key : drop) {
            try {
                format.removeKey(key);
            } catch (Throwable ignored) {
            }
        }
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Log.i(TAG, "encoder configure fallback");
            StarlightLog.job("encoder configure fallback");
        } catch (Throwable second) {
            throw new IOException("encoder configure failed", second);
        }
    }

    private static void copyColor(MediaFormat src, MediaFormat dst) {
        try {
            if (src.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                dst.setInteger(MediaFormat.KEY_COLOR_STANDARD, src.getInteger(MediaFormat.KEY_COLOR_STANDARD));
            }
            if (src.containsKey(MediaFormat.KEY_COLOR_RANGE)) {
                dst.setInteger(MediaFormat.KEY_COLOR_RANGE, src.getInteger(MediaFormat.KEY_COLOR_RANGE));
            }
            if (src.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                dst.setInteger(MediaFormat.KEY_COLOR_TRANSFER, src.getInteger(MediaFormat.KEY_COLOR_TRANSFER));
            }
        } catch (Throwable ignored) {
        }
    }

    private static int[] targetSize(int srcW, int srcH, int requested, int maxEdge) {
        int scale = Math.max(1, Math.min(4, requested));
        long w = (long) srcW * scale;
        long h = (long) srcH * scale;
        double factor = Math.min(1.0d, ((double) maxEdge) / Math.max(w, h));
        factor = Math.min(factor, Math.sqrt(((double) MAX_PIXELS) / ((double) w * h)));
        int outW = even((int) Math.round(w * factor));
        int outH = even((int) Math.round(h * factor));
        return new int[]{Math.max(2, outW), Math.max(2, outH)};
    }

    private static int even(int value) {
        return value & ~1;
    }

    private static void quiet(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
        }
    }

    private static final class Sink {
        Context context;
        Uri uri;
        ParcelFileDescriptor pfd;
        File temp;
        File destination;
        MediaMuxer muxer;
        MediaFormat audioFormat;
        int rotation;
        int vTrack = -1;
        int aTrack = -1;
        boolean started;
        int encoded;
        long lastPtsUs;

        static Sink create(Context context, File src) throws IOException {
            if (Build.VERSION.SDK_INT >= 29) {
                Uri uri = null;
                ParcelFileDescriptor pfd = null;
                try {
                    ContentResolver resolver = context.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, outputName(src));
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    values.put(MediaStore.MediaColumns.DATE_EXPIRES,
                            (System.currentTimeMillis() + 24L * 60L * 60L * 1000L) / 1000L);
                    uri = resolver.insert(MediaStore.Video.Media.getContentUri(
                            MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
                    if (uri != null) {
                        pfd = resolver.openFileDescriptor(uri, "rw");
                        if (pfd != null) {
                            Sink sink = new Sink();
                            sink.context = context;
                            sink.uri = uri;
                            sink.pfd = pfd;
                            sink.muxer = new MediaMuxer(pfd.getFileDescriptor(),
                                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                            return sink;
                        }
                    }
                } catch (Throwable th) {
                    Log.w(TAG, "MediaStore sink failed: " + th);
                }
                if (pfd != null) {
                    try { pfd.close(); } catch (Throwable ignored) {}
                }
                if (uri != null) {
                    try { context.getContentResolver().delete(uri, null, null); } catch (Throwable ignored) {}
                }
            }
            File dir = src.getParentFile();
            if (dir == null) {
                dir = new File("/sdcard/DCIM/Camera");
            }
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("no output directory");
            }
            Sink sink = new Sink();
            sink.context = context;
            sink.temp = new File(dir, ".starlight_" + System.currentTimeMillis() + ".tmp");
            sink.destination = uniqueFile(dir, outputName(src));
            sink.muxer = new MediaMuxer(sink.temp.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            return sink;
        }

        void start(MediaFormat videoFormat) throws IOException {
            vTrack = muxer.addTrack(videoFormat);
            if (audioFormat != null) {
                aTrack = muxer.addTrack(audioFormat);
            }
            if (rotation != 0) {
                muxer.setOrientationHint(rotation);
            }
            muxer.start();
            started = true;
        }

        long size() {
            if (pfd != null) {
                return pfd.getStatSize();
            }
            return temp == null ? 0L : temp.length();
        }

        String describe() {
            if (uri != null) {
                return uri.toString();
            }
            return destination == null ? "?" : destination.getAbsolutePath();
        }

        void publish() throws IOException {
            if (muxer != null) {
                muxer.stop();
                muxer.release();
                muxer = null;
            }
            if (uri != null) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(uri, done, null, null);
                if (pfd != null) {
                    pfd.close();
                    pfd = null;
                }
            } else {
                if (temp == null || destination == null || !temp.renameTo(destination)) {
                    throw new IOException("rename failed");
                }
                temp = null;
                MediaScannerConnection.scanFile(context,
                        new String[]{destination.getAbsolutePath()},
                        new String[]{"video/mp4"}, null);
            }
        }

        void fail() {
            if (muxer != null) {
                try { muxer.stop(); } catch (Throwable ignored) {}
                try { muxer.release(); } catch (Throwable ignored) {}
                muxer = null;
            }
            if (uri != null) {
                try { context.getContentResolver().delete(uri, null, null); } catch (Throwable ignored) {}
                uri = null;
            }
            if (temp != null) {
                //noinspection ResultOfMethodCallIgnored
                temp.delete();
                temp = null;
            }
            if (pfd != null) {
                try { pfd.close(); } catch (Throwable ignored) {}
                pfd = null;
            }
        }

        void close() {
            if (muxer != null) {
                try { muxer.release(); } catch (Throwable ignored) {}
                muxer = null;
            }
            if (pfd != null) {
                try { pfd.close(); } catch (Throwable ignored) {}
                pfd = null;
            }
        }

        private static String outputName(File src) {
            String name = src.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            name = name.replace("_Starlight", "").replace("_Upscaled", "");
            return "Upscaled_" + name + ".mp4";
        }

        private static File uniqueFile(File dir, String name) {
            File file = new File(dir, name);
            if (!file.exists()) {
                return file;
            }
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            String ext = dot > 0 ? name.substring(dot) : "";
            int n = 1;
            while (file.exists()) {
                file = new File(dir, base + "_" + n + ext);
                n++;
            }
            return file;
        }
    }
}
