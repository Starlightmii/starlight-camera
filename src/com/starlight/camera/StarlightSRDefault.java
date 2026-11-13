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
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Starlight SR v2.
 *
 * The "Tensei" upscaler was reverse engineered and found to contain no neural
 * network at all: its pipeline is Canvas bicubic resample + saturation/contrast
 * 1.15 + a RenderScript 3x3 cross-kernel sharpen [0,-1,0,-1,5,-1,0,-1,0].
 *
 * This class keeps that pipeline (so output is familiar) and adds:
 *   - multi-step 2x resampling for 3x/4x (less blur than one big step)
 *   - edge-gated unsharp mask (sharpens detail, leaves flat noise alone)
 *   - banded processing so a 24 MP result does not need a second full copy
 *   - EXIF orientation baked into pixels
 *   - 98 quality JPEG, atomic temp-file rename, MediaScanner notification
 */
public final class StarlightSRDefault {
    public static final String TAG = "StarlightSRDefault";
    public static final int DEFAULT_QUALITY = 98;

    private StarlightSRDefault() {
    }

    public static boolean process(Context context, File src, int scale, int quality, long maxPixels) throws Throwable {
        return process(context, src, scale, quality, maxPixels, 0.35f);
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
        try {
            source = orient(src, source);
            int scaleClamped = Math.max(2, Math.min(4, scale));
            int tw = source.getWidth() * scaleClamped;
            int th = source.getHeight() * scaleClamped;
            double cap = Math.min(1.0d, Math.sqrt(((double) maxPixels) / (((double) tw) * ((double) th))));
            tw = Math.max(source.getWidth(), (int) Math.round(tw * cap));
            th = Math.max(source.getHeight(), (int) Math.round(th * cap));
            if (tw <= source.getWidth() || th <= source.getHeight()) {
                Log.i(TAG, "skip, source already at cap " + source.getWidth() + "x" + source.getHeight());
                return false;
            }

            Bitmap out = resample(source, tw, th);
            if (out != source) {
                source.recycle();
            }
            source = null;

            // Edge-gated detail pass. This is the part Tensei does not have.
            detailEnhance(out, sharpen, 3);

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
            MediaScannerConnection.scanFile(context, new String[]{dst.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
            Log.i(TAG, "done " + dst.getName() + " " + tw + "x" + th + " bytes=" + dst.length()
                    + " ms=" + (System.currentTimeMillis() - started));
            return true;
        } finally {
            if (source != null) {
                source.recycle();
            }
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

    private static Bitmap resample(Bitmap source, int targetW, int targetH) {
        Bitmap current = source;
        // 4x as 2x + 2x keeps more real detail than a single 4x bicubic step.
        while (current.getWidth() * 2 <= targetW && current.getHeight() * 2 <= targetH) {
            int nw = current.getWidth() * 2;
            int nh = current.getHeight() * 2;
            Bitmap next = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(next);
            canvas.drawBitmap(current, null, new Rect(0, 0, nw, nh), paint(false));
            if (current != source) {
                current.recycle();
            }
            current = next;
        }
        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawBitmap(current, null, new Rect(0, 0, targetW, targetH), paint(true));
        if (current != source) {
            current.recycle();
        }
        return out;
    }

    private static Paint paint(boolean color) {
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        if (color) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(1.15f);
            // Tensei contrast: out = in * 1.15 + (-0.5 * 1.15 + 0.5) * 255
            float contrast = 1.15f;
            float translate = (-0.5f * contrast + 0.5f) * 255.0f;
            matrix.postConcat(new ColorMatrix(new float[]{
                    contrast, 0.0f, 0.0f, 0.0f, translate,
                    0.0f, contrast, 0.0f, 0.0f, translate,
                    0.0f, 0.0f, contrast, 0.0f, translate,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            }));
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        }
        return paint;
    }

    /**
     * Luma unsharp mask with a noise gate. Flat areas (|detail| <= threshold)
     * are untouched, so sky/wall noise is not amplified. Edges get a small
     * luma boost applied equally to R/G/B to avoid colour fringes.
     */
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
