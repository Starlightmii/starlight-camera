package com.starlight.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/**
 * Thin animated rainbow progress bar shown at the bottom of the camera while
 * Starlight SR is running. The bar is intentionally below the mode labels so
 * it does not cover the preview thumbnail or the mode switcher.
 */
public final class StarlightProcessingBar extends View {
    private static final int[] COLORS = {
            0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759,
            0xFF00C7BE, 0xFF007AFF, 0xFFAF52DE, 0xFFFF2D55, 0xFFFF3B30
    };

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private LinearGradient gradient;
    private float offset;
    private int progress;
    private boolean running;
    private long lastNs;

    public StarlightProcessingBar(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        track.setColor(0x66000000);
        fill.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        if (clamped != progress) {
            progress = clamped;
            invalidate();
        }
    }

    public void setRunning(boolean value) {
        if (running != value) {
            running = value;
            lastNs = 0L;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0) {
            gradient = new LinearGradient(0.0f, 0.0f, w, 0.0f, COLORS, null, Shader.TileMode.REPEAT);
            fill.setShader(gradient);
            glow.setShader(gradient);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        float radius = h * 0.5f;
        canvas.drawRoundRect(0.0f, 0.0f, w, h, radius, radius, track);

        if (gradient != null && progress > 0) {
            long now = System.nanoTime();
            if (lastNs != 0L) {
                offset += ((now - lastNs) / 1000000000.0f) * w * 0.35f;
            }
            lastNs = now;
            offset %= w;
            matrix.setTranslate(offset, 0.0f);
            gradient.setLocalMatrix(matrix);

            float end = w * progress / 100.0f;
            canvas.save();
            canvas.clipRect(0.0f, 0.0f, end, h);
            glow.setStrokeWidth(h * 2.4f);
            glow.setAlpha(90);
            canvas.drawLine(0.0f, h * 0.5f, w, h * 0.5f, glow);
            canvas.drawRoundRect(0.0f, 0.0f, end, h, radius, radius, fill);
            canvas.restore();
        }
        if (running) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }
}
