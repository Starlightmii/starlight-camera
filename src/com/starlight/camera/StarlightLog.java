package com.starlight.camera;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Rolling file log + no-root process-death diagnostics for Starlight jobs. */
final class StarlightLog {
    private static final String TAG = "StarlightLog";
    private static final long MAX_BYTES = 512 * 1024L;
    private static final Object LOCK = new Object();
    private static File logFile;
    private static boolean installed;

    private StarlightLog() {
    }

    static void init(Context context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            if (logFile != null) {
                return;
            }
            File dir = context.getExternalFilesDir("starlight");
            if (dir == null) {
                dir = new File(context.getFilesDir(), "starlight");
            }
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "cannot create log dir");
                return;
            }
            logFile = new File(dir, "starlight.log");
            if (logFile.length() > MAX_BYTES) {
                File old = new File(dir, "starlight.log.1");
                if (old.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    old.delete();
                }
                //noinspection ResultOfMethodCallIgnored
                logFile.renameTo(old);
            }
            if (!installed) {
                installed = true;
                final Thread.UncaughtExceptionHandler previous =
                        Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable throwable) {
                        e("uncaught " + thread.getName(), throwable);
                        if (previous != null) {
                            previous.uncaughtException(thread, throwable);
                        }
                    }
                });
            }
            i("init sdk=" + Build.VERSION.SDK_INT + " device=" + Build.DEVICE
                    + " model=" + Build.MODEL);
        }
    }

    static void i(String message) {
        write("I", message, null);
    }

    static void e(String message, Throwable throwable) {
        write("E", message, throwable);
    }

    static void job(String message) {
        write("J", message, null);
    }

    static void dumpExitReasons(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 30) {
            return;
        }
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return;
            }
            for (ApplicationExitInfo info : am.getHistoricalProcessExitReasons(
                    context.getPackageName(), 0, 5)) {
                i("exit reason=" + info.getReason() + " status=" + info.getStatus()
                        + " importance=" + info.getImportance() + " pss=" + info.getPss()
                        + " ts=" + info.getTimestamp() + " desc=" + info.getDescription());
                if (info.getReason() == ApplicationExitInfo.REASON_ANR
                        || info.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    InputStream in = info.getTraceInputStream();
                    if (in != null) {
                        saveTrace("exit-" + info.getTimestamp() + ".txt", in);
                    }
                }
            }
        } catch (Throwable th) {
            e("dumpExitReasons", th);
        }
    }

    private static void saveTrace(String name, InputStream in) {
        OutputStream out = null;
        try {
            File dir = logFile == null ? null : logFile.getParentFile();
            if (dir == null) {
                return;
            }
            out = new FileOutputStream(new File(dir, name));
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        } catch (Throwable th) {
            Log.w(TAG, "saveTrace: " + th);
        } finally {
            try { in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
        }
    }

    private static void write(String level, String message, Throwable throwable) {
        String line = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date())
                + " " + level + " " + message;
        Log.println(level.equals("E") ? Log.ERROR : Log.INFO, TAG, line);
        if (throwable != null) {
            Log.e(TAG, message, throwable);
        }
        synchronized (LOCK) {
            if (logFile == null) {
                return;
            }
            FileOutputStream out = null;
            try {
                if (logFile.length() > MAX_BYTES) {
                    File old = new File(logFile.getParentFile(), "starlight.log.1");
                    //noinspection ResultOfMethodCallIgnored
                    old.delete();
                    //noinspection ResultOfMethodCallIgnored
                    logFile.renameTo(old);
                }
                out = new FileOutputStream(logFile, true);
                out.write((line + "\n").getBytes("UTF-8"));
                if (throwable != null) {
                    out.write(Log.getStackTraceString(throwable).getBytes("UTF-8"));
                    out.write('\n');
                }
            } catch (Throwable ignored) {
            } finally {
                try { if (out != null) out.close(); } catch (Throwable ignored) {}
            }
        }
    }
}
