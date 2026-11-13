package com.starlight.camera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;

/** Video SR runs in a separate :sr process so its heap cannot starve the camera. */
public class StarlightSrService extends Service {
    private static final String CHANNEL = "starlight_sr";
    private static final int NOTIFICATION_ID = 9129;
    private static final String ACTION_VIDEO = "com.starlight.camera.action.SR_VIDEO";

    public static void startVideo(Context context) {
        try {
            Intent intent = new Intent(context, StarlightSrService.class);
            intent.setAction(ACTION_VIDEO);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable th) {
            StarlightLog.job("sr service start failed: " + th);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.createNotificationChannel(new NotificationChannel(
                            CHANNEL, "Starlight SR", NotificationManager.IMPORTANCE_LOW));
                }
            }
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL)
                    : new Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Starlight Camera")
                    .setContentText("Video SR in :sr")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, builder.build());
            }
        } catch (Throwable th) {
            StarlightLog.job("sr foreground failed: " + th);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                StarlightLog.init(StarlightSrService.this);
                File latest = StarlightVideoSR.latestVideo(StarlightSrService.this);
                if (latest == null) {
                    toast("Video SR: no video found");
                    stopSelf(startId);
                    return;
                }
                toast("Video SR (:sr): " + latest.getName());
                boolean ok = StarlightVideoTranscoder.process(
                        StarlightSrService.this, latest, 2, 24, 0);
                toast(ok ? "Video SR saved" : "Video SR failed (see logcat)");
                stopSelf(startId);
            }
        }, "starlight-sr-video").start();
        return START_NOT_STICKY;
    }

    private void toast(final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        try {
            stopForeground(true);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
