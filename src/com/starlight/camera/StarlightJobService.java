package com.starlight.camera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** Small dataSync foreground service that keeps long SR jobs alive. */
public class StarlightJobService extends Service {
    private static final String CHANNEL = "starlight_jobs";
    private static final int NOTIFICATION_ID = 9128;

    public static void start(Context context, String text) {
        try {
            Intent intent = new Intent(context, StarlightJobService.class);
            intent.putExtra("text", text);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable th) {
            StarlightLog.job("fgs start failed: " + th);
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, StarlightJobService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String text = intent == null ? "Starlight processing" : intent.getStringExtra("text");
            if (text == null) text = "Starlight processing";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.createNotificationChannel(new NotificationChannel(
                            CHANNEL, "Starlight jobs", NotificationManager.IMPORTANCE_LOW));
                }
            }
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL)
                    : new Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Starlight Camera")
                    .setContentText(text)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true);
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, builder.build());
            }
        } catch (Throwable th) {
            StarlightLog.job("fgs foreground failed: " + th);
        }
        return START_NOT_STICKY;
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
