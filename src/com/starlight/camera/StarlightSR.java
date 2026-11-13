package com.starlight.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicResize;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Starlight SR vMax.
 *
 * Maximum-quality local pipeline:
 *   - Lanczos3 separable resampling (multi-threaded), or RenderScript
 *     ScriptIntrinsicResize bicubic when the GPU path is available.
 *   - Exact Tensei color matrix: saturation 1.15, contrast 1.15, translate
 *     -19.125.
 *   - Edge-gated luma unsharp (0.45 / threshold 2), banded.
 *   - JPEG quality 100.
 *   - EXIF copied from the source, orientation baked into pixels.
 *   - 40 MP ceiling with OOM retry in the caller.
 *
 * The video path reuses upscaleBitmap() with a 4K ceiling.
 */
public final class StarlightSR {
    public static final String TAG = "StarlightSR";
    public static final int DEFAULT_QUALITY = 100;
    private static final int TAPS = 6;
    private static final double LOBE = 3.0d;

    private StarlightSR() {
    }

    public static boolean hwAccel(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("pref_starlight_hw_accel_key", true);
        } catch (Throwable th) {
            return true;
        }
    }

    public static boolean process(Context context, File src, int scale, int quality, long maxPixels) throws Throwable {
        return process(context, src, scale, quality, maxPixels, 0.45f);
    }

    public static boolean process(Context context, File src, int scale, int quality, long maxPixels,
                                  float sharpen) throws Throwable {
        long started = System.currentTimeMillis();
        if (context == null || src == null || !src.exists() || src.length() == 0) {
            return false;
        }
        String name = src.getName();
        if (name.startsWith("Upscaled_") || name.contains("_Upscaled")) {
            return false;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src.getAbsolutePath(), bounds);
        int sw = bounds.outWidth;
        int sh = bounds.outHeight;
        if (sw <= 0 || sh <= 0) {
            return false;
        }
        if (((long) sw) * ((long) sh) > maxPixels) {
            Log.i(TAG, "skip huge source " + sw + "x" + sh);
            return false;
        }

        Bitmap source = BitmapFactory.decodeFile(src.getAbsolutePath());
        if (source == null) {
            return false;
        }
        Bitmap out = null;
        try {
            source = orient(src, source);
            out = upscaleBitmap(context, source, scale, maxPixels, hwAccel(context), sharpen);
        } finally {
            if (source != null) {
                source.recycle();
            }
        }
        if (out == null) {
            return false;
        }

        File dst = nextOutput(src);
        File tmp = new File(dst.getParentFile(), dst.getName() + ".tmp");
        boolean written = false;
        OutputStream os = null;
        try {
            os = new FileOutputStream(tmp);
            written = out.compress(Bitmap.CompressFormat.JPEG, quality > 0 ? quality : DEFAULT_QUALITY, os);
            os.flush();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable ignored) {
                }
            }
        }
        int outW = out.getWidth();
        int outH = out.getHeight();
        out.recycle();

        if (!written || tmp.length() == 0) {
            tmp.delete();
            return false;
        }
        if (!tmp.renameTo(dst)) {
            tmp.delete();
            Log.w(TAG, "rename failed " + tmp);
            return false;
        }
        copyExif(src, dst);
        MediaScannerConnection.scanFile(context, new String[]{dst.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
        Log.i(TAG, "done " + dst.getName() + " " + outW + "x" + outH + " bytes=" + dst.length()
                + " ms=" + (System.currentTimeMillis() - started));
        return true;
    }

    /**
     * Upscale one bitmap. Caller owns the returned bitmap.
     * @param gpu true to try RenderScript bicubic first; false forces Lanczos3.
     */
    public static Bitmap upscaleBitmap(Context context, Bitmap source, int scale, long maxPixels, boolean gpu) {
        return upscaleBitmap(context, source, scale, maxPixels, gpu, 0.45f);
    }

    public static Bitmap upscaleBitmap(Context context, Bitmap source, int scale, long maxPixels,
                                       boolean gpu, float sharpen) {
        if (source == null || source.isRecycled()) {
            return null;
        }
        int scaleClamped = Math.max(2, Math.min(4, scale));
        long tw = ((long) source.getWidth()) * scaleClamped;
        long th = ((long) source.getHeight()) * scaleClamped;
        double cap = Math.min(1.0d, Math.sqrt(((double) maxPixels) / (((double) tw) * ((double) th))));
        int outW = Math.max(source.getWidth(), (int) Math.round(tw * cap));
        int outH = Math.max(source.getHeight(), (int) Math.round(th * cap));
        outW &= ~1;
        outH &= ~1;
        if (outW <= source.getWidth() || outH <= source.getHeight()) {
            Log.i(TAG, "skip, source already at cap " + source.getWidth() + "x" + source.getHeight());
            return null;
        }

        Bitmap out = null;
        boolean gpuDone = false;
        if (gpu && Build.VERSION.SDK_INT >= 21) {
            StarlightGL gl = StarlightGL.create();
            if (gl != null) {
                out = gl.resize(source, outW, outH, true, sharpen);
                gl.close();
                gpuDone = out != null;
            }
            if (out == null) {
                out = resizeBicubicRs(context, source, outW, outH);
            }
        }
        if (out == null) {
            out = resizeLanczos(source, outW, outH);
        }
        if (out == null) {
            return null;
        }
        if (!gpuDone) {
            applyColorMatrix(out);
            detailEnhance(out, sharpen, 2);
        }
        return out;
    }

    /**
     * Fast video path: RenderScript bicubic when available, otherwise a
     * hardware-accelerated bilinear scale. Much cheaper than Lanczos3 and
     * good enough for 24 fps video on sweetin.
     */
    public static Bitmap upscaleBitmapFast(Context context, Bitmap source, int scale, long maxPixels) {
        if (source == null || source.isRecycled()) {
            return null;
        }
        int scaleClamped = Math.max(1, Math.min(4, scale));
        long tw = ((long) source.getWidth()) * scaleClamped;
        long th = ((long) source.getHeight()) * scaleClamped;
        double cap = Math.min(1.0d, Math.sqrt(((double) maxPixels) / (((double) tw) * ((double) th))));
        int outW = Math.max(source.getWidth(), (int) Math.round(tw * cap));
        int outH = Math.max(source.getHeight(), (int) Math.round(th * cap));
        outW &= ~1;
        outH &= ~1;
        if (outW <= source.getWidth() || outH <= source.getHeight()) {
            return null;
        }
        Bitmap out = null;
        if (hwAccel(context)) {
            out = resizeBicubicRs(context, source, outW, outH);
        }
        if (out == null) {
            try {
                out = Bitmap.createScaledBitmap(source, outW, outH, true);
            } catch (Throwable th2) {
                return null;
            }
        }
        return postProcessVideo(out);
    }

    public static Bitmap postProcessVideo(Bitmap out) {
        if (out == null) {
            return null;
        }
        applyColorMatrix(out);
        detailEnhance(out, 0.35f, 3);
        return out;
    }

    /**
     * Reusable RenderScript bicubic resizer for video. Creating a RenderScript
     * context and allocations per frame is far more expensive than the resize
     * itself, so the video path keeps one instance for the whole clip.
     */
    public static final class FastResizer implements AutoCloseable {
        private final RenderScript rs;
        private final ScriptIntrinsicResize resize;
        private final Allocation input;
        private final Allocation output;
        private final int outW;
        private final int outH;

        private FastResizer(RenderScript rs, ScriptIntrinsicResize resize,
                            Allocation input, Allocation output, int outW, int outH) {
            this.rs = rs;
            this.resize = resize;
            this.input = input;
            this.output = output;
            this.outW = outW;
            this.outH = outH;
        }

        public static FastResizer create(Context context, int srcW, int srcH, int outW, int outH) {
            RenderScript rs = null;
            Allocation input = null;
            Allocation output = null;
            ScriptIntrinsicResize resize = null;
            Bitmap inBitmap = null;
            Bitmap outBitmap = null;
            try {
                rs = RenderScript.create(context);
                rs.setPriority(RenderScript.Priority.NORMAL);
                inBitmap = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888);
                outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
                input = Allocation.createFromBitmap(rs, inBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                output = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                resize = ScriptIntrinsicResize.create(rs);
                resize.setInput(input);
                Log.i(TAG, "FastResizer " + srcW + "x" + srcH + " -> " + outW + "x" + outH);
                return new FastResizer(rs, resize, input, output, outW, outH);
            } catch (Throwable th) {
                Log.w(TAG, "FastResizer create failed: " + th);
                if (resize != null) {
                    try { resize.destroy(); } catch (Throwable ignored) {}
                }
                if (input != null) {
                    try { input.destroy(); } catch (Throwable ignored) {}
                }
                if (output != null) {
                    try { output.destroy(); } catch (Throwable ignored) {}
                }
                if (rs != null) {
                    try { rs.destroy(); } catch (Throwable ignored) {}
                }
                return null;
            } finally {
                if (inBitmap != null) {
                    inBitmap.recycle();
                }
                if (outBitmap != null) {
                    outBitmap.recycle();
                }
            }
        }

        public Bitmap resize(Bitmap source) {
            try {
                input.copyFrom(source);
                resize.forEach_bicubic(output);
                Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
                output.copyTo(out);
                return out;
            } catch (Throwable th) {
                Log.w(TAG, "FastResizer resize failed: " + th);
                return null;
            }
        }

        @Override
        public void close() {
            try { resize.destroy(); } catch (Throwable ignored) {}
            try { input.destroy(); } catch (Throwable ignored) {}
            try { output.destroy(); } catch (Throwable ignored) {}
            try { rs.destroy(); } catch (Throwable ignored) {}
        }
    }

    private static Bitmap resizeBicubicRs(Context context, Bitmap source, int w, int h) {
        RenderScript rs = null;
        Allocation in = null;
        Allocation outAlloc = null;
        ScriptIntrinsicResize resize = null;
        try {
            rs = RenderScript.create(context);
            rs.setPriority(RenderScript.Priority.NORMAL);
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            in = Allocation.createFromBitmap(rs, source, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            outAlloc = Allocation.createFromBitmap(rs, out, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            resize = ScriptIntrinsicResize.create(rs);
            resize.setInput(in);
            resize.forEach_bicubic(outAlloc);
            outAlloc.copyTo(out);
            Log.i(TAG, "RenderScript bicubic " + w + "x" + h);
            return out;
        } catch (Throwable th) {
            Log.w(TAG, "RenderScript bicubic failed: " + th);
            return null;
        } finally {
            if (resize != null) {
                try { resize.destroy(); } catch (Throwable ignored) {}
            }
            if (in != null) {
                try { in.destroy(); } catch (Throwable ignored) {}
            }
            if (outAlloc != null) {
                try { outAlloc.destroy(); } catch (Throwable ignored) {}
            }
            if (rs != null) {
                try { rs.destroy(); } catch (Throwable ignored) {}
            }
        }
    }

    private static Bitmap resizeLanczos(Bitmap source, int w, int h) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int[] srcPixels = new int[srcW * srcH];
        source.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH);

        int[] hIdx = new int[w * TAPS];
        float[] hW = new float[w * TAPS];
        buildWeights(srcW, w, hIdx, hW);
        int[] hOut = new int[w * srcH];

        int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory < 268435456L) {
            threads = Math.min(threads, 2);
        } else if (maxMemory < 536870912L) {
            threads = Math.min(threads, 4);
        }
        runRows(srcH, threads, new RowTask() {
            @Override
            public void run(int y0, int y1) {
                for (int y = y0; y < y1; y++) {
                    int srcBase = y * srcW;
                    int outBase = y * w;
                    for (int x = 0; x < w; x++) {
                        int r = 0;
                        int g = 0;
                        int b = 0;
                        int a = 0;
                        int wi = x * TAPS;
                        for (int t = 0; t < TAPS; t++) {
                            int p = srcPixels[srcBase + hIdx[wi + t]];
                            float wt = hW[wi + t];
                            r += (int) (((p >> 16) & 0xFF) * wt);
                            g += (int) (((p >> 8) & 0xFF) * wt);
                            b += (int) ((p & 0xFF) * wt);
                            a += (int) (((p >>> 24) & 0xFF) * wt);
                        }
                        hOut[outBase + x] = (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                    }
                }
            }
        });

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] vIdx = new int[h * TAPS];
        float[] vW = new float[h * TAPS];
        buildWeights(srcH, h, vIdx, vW);
        final Object writeLock = new Object();
        runRows(h, threads, new RowTask() {
            @Override
            public void run(int y0, int y1) {
                int[] row = new int[w];
                for (int y = y0; y < y1; y++) {
                    int wi = y * TAPS;
                    for (int x = 0; x < w; x++) {
                        int r = 0;
                        int g = 0;
                        int b = 0;
                        int a = 0;
                        for (int t = 0; t < TAPS; t++) {
                            int p = hOut[vIdx[wi + t] * w + x];
                            float wt = vW[wi + t];
                            r += (int) (((p >> 16) & 0xFF) * wt);
                            g += (int) (((p >> 8) & 0xFF) * wt);
                            b += (int) ((p & 0xFF) * wt);
                            a += (int) (((p >>> 24) & 0xFF) * wt);
                        }
                        row[x] = (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                    }
                    synchronized (writeLock) {
                        out.setPixels(row, 0, w, 0, y, w, 1);
                    }
                }
            }
        });
        Log.i(TAG, "Lanczos3 " + w + "x" + h + " threads=" + threads);
        return out;
    }

    private interface RowTask {
        void run(int y0, int y1);
    }

    private static void runRows(int total, int threads, RowTask task) {
        if (threads <= 1) {
            task.run(0, total);
            return;
        }
        Thread[] workers = new Thread[threads];
        int chunk = (total + threads - 1) / threads;
        for (int i = 0; i < threads; i++) {
            final int y0 = i * chunk;
            final int y1 = Math.min(total, y0 + chunk);
            if (y0 >= y1) {
                break;
            }
            workers[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    task.run(y0, y1);
                }
            }, "starlight-sr-" + i);
            workers[i].start();
        }
        for (Thread worker : workers) {
            if (worker != null) {
                try {
                    worker.join();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void buildWeights(int srcSize, int dstSize, int[] idx, float[] weights) {
        double ratio = ((double) srcSize) / dstSize;
        for (int x = 0; x < dstSize; x++) {
            double center = (x + 0.5d) * ratio - 0.5d;
            int left = (int) Math.floor(center) - 2;
            double sum = 0.0d;
            int base = x * TAPS;
            for (int t = 0; t < TAPS; t++) {
                int sx = left + t;
                double d = center - sx;
                double wt = lanczos(d);
                idx[base + t] = sx < 0 ? 0 : (sx >= srcSize ? srcSize - 1 : sx);
                weights[base + t] = (float) wt;
                sum += wt;
            }
            if (sum != 0.0d) {
                for (int t = 0; t < TAPS; t++) {
                    weights[base + t] = (float) (weights[base + t] / sum);
                }
            }
        }
    }

    private static double lanczos(double x) {
        if (x == 0.0d) {
            return 1.0d;
        }
        if (x <= -LOBE || x >= LOBE) {
            return 0.0d;
        }
        double px = Math.PI * x;
        return LOBE * Math.sin(px) * Math.sin(px / LOBE) / (px * px);
    }

    private static void applyColorMatrix(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final int band = 256;
        int[] row = new int[width * band];
        final float contrast = 1.15f;
        final float translate = (-0.5f * contrast + 0.5f) * 255.0f;
        for (int y0 = 0; y0 < height; y0 += band) {
            int rows = Math.min(band, height - y0);
            bitmap.getPixels(row, 0, width, 0, y0, width, rows);
            int count = width * rows;
            for (int i = 0; i < count; i++) {
                int p = row[i];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                float lum = 0.213f * r + 0.715f * g + 0.072f * b;
                float nr = lum + 1.15f * (r - lum);
                float ng = lum + 1.15f * (g - lum);
                float nb = lum + 1.15f * (b - lum);
                int rr = clamp((int) (nr * contrast + translate + 0.5f));
                int gg = clamp((int) (ng * contrast + translate + 0.5f));
                int bb = clamp((int) (nb * contrast + translate + 0.5f));
                row[i] = (p & 0xFF000000) | (rr << 16) | (gg << 8) | bb;
            }
            bitmap.setPixels(row, 0, width, 0, y0, width, rows);
        }
    }

    private static void detailEnhance(Bitmap bitmap, float amount, int threshold) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width < 3 || height < 3) {
            return;
        }
        final int band = 96;
        int[] input = new int[width * (band + 2)];
        int[] output = new int[width * band];
        for (int y0 = 0; y0 < height; y0 += band) {
            int rows = Math.min(band, height - y0);
            int start = Math.max(0, y0 - 1);
            int end = Math.min(height, y0 + rows + 1);
            int got = end - start;
            bitmap.getPixels(input, 0, width, 0, start, width, got);
            for (int yy = 0; yy < rows; yy++) {
                int centerRow = (y0 + yy) - start;
                int outBase = yy * width;
                int rowBase = centerRow * width;
                int rowUp = (centerRow > 0 ? centerRow - 1 : 0) * width;
                int rowDown = (centerRow + 1 < got ? centerRow + 1 : got - 1) * width;
                for (int x = 0; x < width; x++) {
                    int pixel = input[rowBase + x];
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int luma = (77 * r + 150 * g + 29 * b) >> 8;
                    int left = x > 0 ? x - 1 : 0;
                    int right = x + 1 < width ? x + 1 : width - 1;
                    int l = (77 * ((input[rowBase + left] >> 16) & 0xFF) + 150 * ((input[rowBase + left] >> 8) & 0xFF) + 29 * (input[rowBase + left] & 0xFF)) >> 8;
                    int rr = (77 * ((input[rowBase + right] >> 16) & 0xFF) + 150 * ((input[rowBase + right] >> 8) & 0xFF) + 29 * (input[rowBase + right] & 0xFF)) >> 8;
                    int u = (77 * ((input[rowUp + x] >> 16) & 0xFF) + 150 * ((input[rowUp + x] >> 8) & 0xFF) + 29 * (input[rowUp + x] & 0xFF)) >> 8;
                    int d = (77 * ((input[rowDown + x] >> 16) & 0xFF) + 150 * ((input[rowDown + x] >> 8) & 0xFF) + 29 * (input[rowDown + x] & 0xFF)) >> 8;
                    int blur = (l + rr + u + d + 2) >> 2;
                    int detail = luma - blur;
                    int abs = detail < 0 ? -detail : detail;
                    if (abs > threshold) {
                        int boost = (int) ((detail - (detail > 0 ? threshold : -threshold)) * amount);
                        r = clamp(r + boost);
                        g = clamp(g + boost);
                        b = clamp(b + boost);
                    }
                    output[outBase + x] = (pixel & 0xFF000000) | (r << 16) | (g << 8) | b;
                }
            }
            bitmap.setPixels(output, 0, width, 0, y0, width, rows);
        }
    }

    private static void copyExif(File src, File dst) {
        try {
            ExifInterface in = new ExifInterface(src.getAbsolutePath());
            ExifInterface out = new ExifInterface(dst.getAbsolutePath());
            String[] tags = new String[]{
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_ISO_SPEED_RATINGS,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_WHITE_BALANCE,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP
            };
            for (String tag : tags) {
                String value = in.getAttribute(tag);
                if (value != null) {
                    out.setAttribute(tag, value);
                }
            }
            out.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            out.saveAttributes();
        } catch (Throwable th) {
            Log.w(TAG, "exif copy failed: " + th);
        }
    }

    private static Bitmap orient(File file, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable ignored) {
        }
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180.0f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1.0f, -1.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Throwable th) {
            Log.w(TAG, "orient failed: " + th);
            return bitmap;
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static File nextOutput(File src) {
        File parent = src.getParentFile();
        String stem = stem(src.getName());
        File dst = new File(parent, "Upscaled_" + stem + ".jpg");
        int n = 1;
        while (dst.exists()) {
            dst = new File(parent, "Upscaled_" + stem + "_" + n + ".jpg");
            n++;
        }
        return dst;
    }

    private static String stem(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        String out = name.replace("_Starlight", "").replace("_Upscaled", "");
        if (out.startsWith("DSC_") || out.startsWith("IMG_") || out.startsWith("VID_") || out.startsWith("TIMELAPSE_")) {
            return out.substring(out.indexOf('_') + 1);
        }
        return out;
    }
}
