package com.starlight.camera;

import android.app.ActivityManager;
import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

/** One-shot hardware capability probe used by the video planner. */
final class StarlightCapabilities {
    private static final String TAG = "StarlightCaps";

    private StarlightCapabilities() {
    }

    static final class Probe {
        String encoder;
        String decoder;
        int maxEdge;
        boolean hardware4k;
        boolean lowRam;
        int tier;
    }

    static Probe probe(Context context, String mime, int requestedEdge, int fps) {
        Probe p = new Probe();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            p.lowRam = am != null && am.isLowRamDevice();
        } catch (Throwable ignored) {
        }
        p.encoder = bestCodec(mime, true);
        p.decoder = bestCodec(mime, false);
        p.maxEdge = supportedEdge(p.encoder, mime, requestedEdge, fps);
        p.hardware4k = p.maxEdge >= 3840;
        p.tier = p.lowRam || p.maxEdge <= 1920 ? 3 : (p.hardware4k ? 2 : 1);
        StarlightLog.job("caps tier=" + p.tier + " lowRam=" + p.lowRam
                + " edge=" + p.maxEdge + " enc=" + p.encoder + " dec=" + p.decoder
                + " sdk=" + Build.VERSION.SDK_INT);
        return p;
    }

    static String bestCodec(String mime, boolean encoder) {
        try {
            MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            MediaCodecInfo best = null;
            int bestScore = -1;
            for (MediaCodecInfo info : list.getCodecInfos()) {
                if (info.isEncoder() != encoder) continue;
                try {
                    info.getCapabilitiesForType(mime);
                } catch (Throwable th) {
                    continue;
                }
                boolean hw = Build.VERSION.SDK_INT < 29 || info.isHardwareAccelerated();
                if (!hw) continue;
                String lower = info.getName().toLowerCase(Locale.US);
                int score = 0;
                if (lower.contains("qti") || lower.contains("qcom")) score += 4;
                if (lower.contains("c2.")) score += 2;
                if (score > bestScore) {
                    bestScore = score;
                    best = info;
                }
            }
            return best == null ? null : best.getName();
        } catch (Throwable th) {
            Log.w(TAG, "bestCodec: " + th);
            return null;
        }
    }

    static int supportedEdge(String encoderName, String mime, int requestedEdge, int fps) {
        int[] edges = requestedEdge >= 3840
                ? new int[]{3840, 2560, 1920, 1280}
                : (requestedEdge >= 2560 ? new int[]{2560, 1920, 1280}
                : new int[]{1920, 1280});
        for (int edge : edges) {
            if (edge > requestedEdge) continue;
            int w = edge;
            int h = edge == 3840 ? 2160 : (edge == 2560 ? 1440 : (edge == 1920 ? 1080 : 720));
            if (supports(encoderName, mime, w, h, fps)) {
                return edge;
            }
        }
        return 1280;
    }

    private static boolean supports(String name, String mime, int w, int h, int fps) {
        if (name == null) return false;
        try {
            MediaCodecInfo info = null;
            for (MediaCodecInfo candidate : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (candidate.getName().equals(name)) {
                    info = candidate;
                    break;
                }
            }
            if (info == null) return false;
            MediaCodecInfo.VideoCapabilities vc =
                    info.getCapabilitiesForType(mime).getVideoCapabilities();
            int aw = vc.getWidthAlignment();
            int ah = vc.getHeightAlignment();
            int ww = ((w + aw - 1) / aw) * aw;
            int hh = ((h + ah - 1) / ah) * ah;
            return vc.isSizeSupported(ww, hh) && vc.areSizeAndRateSupported(ww, hh, fps);
        } catch (Throwable th) {
            Log.w(TAG, "supports " + name + ": " + th);
            return false;
        }
    }
}
