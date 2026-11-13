package com.starlight.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;

/** Central low-end/potato policy and capability caps. */
final class StarlightPolicy {
    static final String PREF_POTATO = "pref_starlight_potato_key";
    static final String PREF_VIDEO_TIER = "pref_starlight_video_tier_key";
    static final String PREF_DELETE_ORIGINAL = "pref_starlight_delete_original_key";

    private StarlightPolicy() {
    }

    static boolean potato(Context context) {
        return bool(context, PREF_POTATO, false);
    }

    static String videoTier(Context context) {
        String value = string(context, PREF_VIDEO_TIER, "auto");
        if ("off".equals(value) || "2160".equals(value) || "1440".equals(value)
                || "1080".equals(value) || "auto".equals(value)) {
            return value;
        }
        return "auto";
    }

    static boolean autoVideo(Context context) {
        return !potato(context) && "1".equals(string(context, "pref_starlight_video_upscale_key", "0"));
    }

    static boolean deleteOriginal(Context context) {
        return bool(context, PREF_DELETE_ORIGINAL, true);
    }

    static int videoMaxEdge(Context context) {
        if (potato(context)) {
            return 1920;
        }
        String tier = videoTier(context);
        if ("1080".equals(tier)) return 1920;
        if ("1440".equals(tier)) return 2560;
        return 3840;
    }

    static int videoFps(Context context, int srcFps) {
        int fps = Math.max(15, Math.min(60, srcFps <= 0 ? 30 : srcFps));
        return potato(context) ? Math.min(30, fps) : fps;
    }

    static float videoSharpen(Context context) {
        return potato(context) ? 0.08f : 0.18f;
    }

    static float photoSharpen(Context context) {
        return potato(context) ? 0.10f : 0.45f;
    }

    static long photoCap(Context context, boolean maxQuality) {
        if (potato(context)) {
            return 16000000L;
        }
        return maxQuality ? 40000000L : 24000000L;
    }

    static int photoQuality(Context context, boolean maxQuality) {
        if (potato(context)) {
            return 95;
        }
        return maxQuality ? 98 : 98;
    }

    static int thermalStatus(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return Build.VERSION.SDK_INT >= 29 && pm != null ? pm.getCurrentThermalStatus() : 0;
        } catch (Throwable th) {
            return 0;
        }
    }

    static boolean powerSave(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isPowerSaveMode();
        } catch (Throwable th) {
            return false;
        }
    }

    static int photoScale(Context context, int requested) {
        return potato(context) ? 2 : Math.max(2, Math.min(4, requested));
    }

    private static boolean bool(Context context, String key, boolean def) {
        try {
            return prefs(context).getBoolean(key, def);
        } catch (Throwable th) {
            return def;
        }
    }

    private static String string(Context context, String key, String def) {
        try {
            return prefs(context).getString(key, def);
        } catch (Throwable th) {
            return def;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
