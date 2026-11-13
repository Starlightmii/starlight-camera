package com.starlight.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Experimental video-SR trigger:
 *   adb shell am broadcast -a com.starlight.camera.VIDEO_SR
 */
public class StarlightVideoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        StarlightLog.init(context);
        String action = intent == null ? "null" : intent.getAction();
        mark(context, "entered " + action);
        Log.e("StarlightDebug", "entered " + action);
        if ("com.starlight.camera.DEBUG_ENABLE".equals(action)) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean("pref_starlight_smartname_key", true);
            editor.putBoolean("pref_starlight_upscale_key", true);
            editor.putBoolean("pref_starlight_upscale_button_key", true);
            editor.putBoolean("pref_starlight_overlay_key", true);
            editor.putBoolean("pref_starlight_glass_key", true);
            editor.putString("pref_starlight_upscale_scale_value_key", "2");
            editor.putString("pref_starlight_photo_quality_key", "default");
            editor.putBoolean("pref_starlight_video_upscale_key", true);
            editor.apply();
            Log.i(StarlightVideoSR.TAG, "debug prefs enabled");
            return;
        }
        if ("com.starlight.camera.DEBUG_DEFAULT".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("pref_starlight_photo_quality_key", "default").apply();
            mark(context, "photo quality default");
            return;
        }
        if ("com.starlight.camera.DEBUG_MAX".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("pref_starlight_photo_quality_key", "max").apply();
            mark(context, "photo quality max");
            return;
        }
        if ("com.starlight.camera.DEBUG_HW_OFF".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("pref_starlight_hw_accel_key", false).apply();
            mark(context, "hw accel off");
            return;
        }
        if ("com.starlight.camera.DEBUG_HW_ON".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("pref_starlight_hw_accel_key", true).apply();
            mark(context, "hw accel on");
            return;
        }
        if ("com.starlight.camera.DEBUG_VIDEO_OFF".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("pref_starlight_video_upscale_key", false).apply();
            mark(context, "video upscale off");
            return;
        }
        if ("com.starlight.camera.DEBUG_VIDEO_ON".equals(action)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("pref_starlight_video_upscale_key", true).apply();
            mark(context, "video upscale on");
            return;
        }
        if ("com.starlight.camera.DEBUG_UPSCALE".equals(action)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File latest = latestPhoto(context);
                        mark(context, "latest " + latest);
                        Log.i(StarlightSR.TAG, "debug latest " + latest);
                        if (latest != null) {
                            mark(context, "process start " + latest.getName());
                            boolean max = "max".equals(PreferenceManager.getDefaultSharedPreferences(context)
                                    .getString("pref_starlight_photo_quality_key", "default"));
                            boolean ok = max
                                    ? StarlightSR.process(context, latest, 2, 100, 40000000L)
                                    : StarlightSRDefault.process(context, latest, 2, 98, 24000000L);
                            mark(context, "process ok=" + ok);
                            Log.i(StarlightSR.TAG, "debug process ok=" + ok);
                        }
                    } catch (Throwable th) {
                        mark(context, "process failed " + th);
                        Log.e(StarlightSR.TAG, "debug process failed", th);
                    }
                }
            }, "starlight-debug-sr").start();
            return;
        }
        final String path = intent == null ? null : intent.getStringExtra("path");
        if (path != null && path.length() > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StarlightVideoSR.process(context, new File(path), 2, 24, 0);
                }
            }, "starlight-video-path-sr").start();
            return;
        }
        StarlightSrService.startVideo(context);
    }

    private static void mark(Context context, String text) {
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) {
                dir = context.getFilesDir();
            }
            if (dir != null) {
                FileOutputStream out = new FileOutputStream(new File(dir, "receiver.log"), true);
                out.write((System.currentTimeMillis() + " " + text + "\n").getBytes("UTF-8"));
                out.close();
            }
        } catch (Throwable th) {
            Log.e("StarlightDebug", "mark failed", th);
        }
    }

    private static File latestPhoto(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME},
                    null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (cursor != null) {
                int column = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    String name = nameColumn >= 0 ? cursor.getString(nameColumn) : null;
                    if (name != null && (name.startsWith("Upscaled_") || name.contains("_Upscaled"))) {
                        continue;
                    }
                    if (column >= 0) {
                        return new File(cursor.getString(column));
                    }
                }
            }
        } catch (Throwable th) {
            Log.e(StarlightSR.TAG, "query latest failed", th);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
