package com.starlight.camera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Best-effort progress surface for ColorOS live alerts / dynamic island.
 * A silent ongoing progress notification is the only public API available to
 * a normal app; ColorOS may promote it to a live alert.
 */
public final class StarlightProgressNotifier {
    private static final String CHANNEL = "starlight_sr_progress";
    private static final int ID = 0x5352;

    private StarlightProgressNotifier() {
    }

    private static NotificationManager manager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void channel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = manager(context);
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Starlight SR", NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Photo and video upscaling progress");
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }

    public static void update(Context context, String title, int percent) {
        try {
            channel(context);
            NotificationManager nm = manager(context);
            if (nm == null) {
                return;
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL)
                    : new Notification.Builder(context);
            b.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(title)
                    .setContentText("Upscaling " + Math.max(0, Math.min(100, percent)) + "%")
                    .setProgress(100, Math.max(0, Math.min(100, percent)), false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setCategory(Notification.CATEGORY_PROGRESS);
            nm.notify(ID, b.build());
        } catch (Throwable ignored) {
        }
    }

    public static void stop(Context context) {
        try {
            NotificationManager nm = manager(context);
            if (nm != null) {
                nm.cancel(ID);
            }
        } catch (Throwable ignored) {
        }
    }
}
