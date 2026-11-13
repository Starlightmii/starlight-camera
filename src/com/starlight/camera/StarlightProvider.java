package com.starlight.camera;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: /tmp/opencode/tensei_work/live_dex/classes4.dex */
public class StarlightProvider extends ContentProvider {
    private static final String PKG = "com.google.android.GoogleCameraLMCR17";
    private static final String PREF_AUX = "pref_aux_key";
    private static final String PREF_DOCK = "pref_starlight_dock_key";
    private static final String PREF_FLASH = "pref_camera_back_flashmode_key";
    private static final String PREF_GLASS = "pref_starlight_glass_key";
    private static final String PREF_GLASS_INTENSITY = "pref_starlight_glass_intensity";
    private static final String PREF_HDR = "pref_hdrnet_key";
    private static final String PREF_MEDIA = "pref_starlight_media_opener_key";
    private static final String PREF_MOTION = "pref_starlight_motion_key";
    private static final String PREF_OVERLAY = "pref_starlight_overlay_key";
    private static final String PREF_SMART = "pref_starlight_smartname_key";
    private static final String PREF_STAB = "pref_starlight_stab_key";
    private static final String PREF_UPSCALE = "pref_starlight_upscale_key";
    private static final String PREF_UPSCALE_BUTTON = "pref_starlight_upscale_button_key";
    private static final String PREF_UPSCALE_SCALE = "pref_starlight_upscale_scale_value_key";
    private static final String PREF_UPSCALE_SCALE_LEGACY = "pref_starlight_upscale_scale_key";
    private static final String PREF_VIDEO_UPSCALE = "pref_starlight_video_upscale_key";
    private static final String PREF_PHOTO_QUALITY = "pref_starlight_photo_quality_key";
    private static final String TAG = "StarlightCam";
    private static final int UPSCALE_JPEG_QUALITY = 100;
    private static final int UPSCALE_MAX_PIXELS = 40000000;
    private static final int UPSCALE_DEFAULT_MAX_PIXELS = 24000000;
    private static Context sCtx;
    private static volatile boolean sPhotoProcessing = false;
    private static volatile int sPhotoProgress = 0;
    private static long[] sLastCpu;
    private static Toast sScaleToast;
    private long lastCpuSet = 0;
    private Handler mainHandler;
    private Handler upscaleHandler;
    private HandlerThread upscaleThread;
    private HandlerThread worker;
    private Handler workerHandler;
    private static final double[] ZOOM_STOPS = {0.6d, 1.0d, 2.0d, 5.0d, 10.0d, 30.0d};
    private static final Pattern TS = Pattern.compile("(\\d{8}_\\d{6})");
    private static final Pattern DONE = Pattern.compile("^(Person|Selfie|Macro|Wide|Night|Photo|Pet|Document|Laptop|Video|Timelapse|108MP|B&W)_\\d{8}_\\d{6}_Starlight\\.[A-Za-z0-9]+$");
    private static final Set<String> SEEN = Collections.synchronizedSet(new HashSet());
    private static final Set<String> UPSCALED = Collections.synchronizedSet(new HashSet());

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        sCtx = context.getApplicationContext();
        StarlightLog.init(context);
        final Context logContext = context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                StarlightLog.dumpExitReasons(logContext);
            }
        }, "starlight-exit-reasons").start();
        migrateUpscaleScale(context);
        migrateBooleanPrefs(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.worker = new HandlerThread("starlight-cam");
        this.worker.start();
        this.workerHandler = new Handler(this.worker.getLooper());
        this.upscaleThread = new HandlerThread("starlight-upscale", 10);
        this.upscaleThread.start();
        this.upscaleHandler = new Handler(this.upscaleThread.getLooper());
        Log.i(TAG, "upscale worker started");
        try {
            ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Lifecycle());
            Log.i(TAG, "lifecycle callbacks registered");
        } catch (Throwable th) {
            Log.e(TAG, "lifecycle register failed", th);
        }
        try {
            MediaObserver mediaObserver = new MediaObserver(this.workerHandler);
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver);
            contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver);
            Log.i(TAG, "media observer registered");
        } catch (Throwable th2) {
            Log.e(TAG, "media observer failed", th2);
        }
        return true;
    }

    public static Intent applyMediaOpener(Intent intent) {
        try {
            if (sCtx == null) {
                return intent;
            }
            String string = PreferenceManager.getDefaultSharedPreferences(sCtx).getString(PREF_MEDIA, "0");
            if ("1".equals(string) && installed("com.google.android.apps.photos")) {
                intent.setPackage("com.google.android.apps.photos");
            } else if ("2".equals(string)) {
                intent.setPackage(null);
            } else {
                intent.setPackage("com.coloros.gallery3d");
            }
        } catch (Throwable th) {
        }
        return intent;
    }

    private static boolean installed(String str) {
        try {
            return (sCtx == null || sCtx.getPackageManager().getPackageInfo(str, 0) == null) ? false : true;
        } catch (Throwable th) {
            return false;
        }
    }

    public static void logHfr(String str, Object obj, Object obj2) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File("/data/data/com.google.android.GoogleCameraLMCR17/files/hfr.log"), true);
            StringBuilder sb = new StringBuilder();
            sb.append(System.currentTimeMillis()).append(' ').append(str).append("\n");
            describe(sb, "a", obj);
            describe(sb, "b", obj2);
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th) {
        }
    }

    public static void logStack(String str) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File("/data/data/com.google.android.GoogleCameraLMCR17/files/hfr.log"), true);
            StringBuilder sb = new StringBuilder();
            sb.append(System.currentTimeMillis()).append(' ').append(str).append("\n");
            for (StackTraceElement stackTraceElement : new Exception().getStackTrace()) {
                sb.append("    at ").append(stackTraceElement.toString()).append('\n');
            }
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th) {
        }
    }

    public static void logThrowable(String str, Throwable th) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File("/data/data/com.google.android.GoogleCameraLMCR17/files/hfr.log"), true);
            StringBuilder sb = new StringBuilder();
            sb.append(System.currentTimeMillis()).append(' ').append(str).append("\n");
            for (int i = 0; th != null && i < 5; i++) {
                sb.append("  CAUSE ").append(i).append(": ").append(th.getClass().getName()).append(": ").append(th.getMessage()).append('\n');
                for (StackTraceElement stackTraceElement : th.getStackTrace()) {
                    sb.append("    at ").append(stackTraceElement.toString()).append('\n');
                }
                th = th.getCause();
            }
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th2) {
        }
    }

    private static void describe(StringBuilder sb, String str, Object obj) {
        sb.append("  ").append(str).append(": ");
        if (obj == null) {
            sb.append("null\n");
            return;
        }
        sb.append(obj.getClass().getName());
        if (obj instanceof List) {
            List list = (List) obj;
            sb.append(" size=").append(list.size()).append(" val=").append(list).append('\n');
            return;
        }
        if (obj instanceof Map) {
            Map map = (Map) obj;
            sb.append(" size=").append(map.size()).append('\n');
            for (Object obj2 : map.keySet()) {
                Object obj3 = map.get(obj2);
                sb.append("    ").append(obj2).append(" -> ").append(obj3);
                if (obj3 instanceof List) {
                    sb.append(" size=").append(((List) obj3).size());
                }
                sb.append('\n');
            }
            return;
        }
        if (obj.getClass().getName().equals("ghx")) {
            try {
                sb.append(" K=").append(obj.getClass().getMethod("K", new Class[0]).invoke(obj, new Object[0]));
                List list2 = (List) obj.getClass().getMethod("w", new Class[0]).invoke(obj, new Object[0]);
                sb.append(" sizes=").append(list2).append('\n');
                Method method = obj.getClass().getMethod("v", Class.forName("lig"));
                for (Object obj4 : list2) {
                    sb.append("    ").append(obj4).append(" ranges=").append(method.invoke(obj, obj4)).append('\n');
                }
                return;
            } catch (Throwable th) {
                sb.append(" reflect-failed=").append(th).append('\n');
                return;
            }
        }
        sb.append(' ').append(obj).append('\n');
    }

    private final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        private Lifecycle() {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPreCreated(Activity activity, Bundle bundle) {
            if (StarlightProvider.isCamera(activity)) {
                StarlightProvider.this.applyNativeFlags(activity);
            }
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
            if (StarlightProvider.isCamera(activity)) {
                StarlightProvider.this.applyNativeFlags(activity);
                StarlightProvider.this.installOverlay(activity);
            }
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isCamera(Activity activity) {
        if (activity == null) {
            return false;
        }
        String name = activity.getClass().getName();
        if (activity.getPackageName().equals(PKG)) {
            return name.endsWith("CameraLauncher") || name.endsWith("CameraActivity");
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyNativeFlags(Activity activity) {
        int i;
        try {
            Class<?> cls = Class.forName("dep");
            Object obj = cls.getField("sh").get(null);
            if (obj != null) {
                Class<?> cls2 = Class.forName("dcu");
                Class<?> cls3 = Class.forName("ddg");
                Object obj2 = cls2.getField("J").get(null);
                Method method = cls.getMethod("k", cls3);
                logHfr("dcu-before", method.invoke(obj, obj2), null);
                cls.getMethod("s", cls3, Boolean.TYPE).invoke(obj, obj2, false);
                logHfr("dcu-after", method.invoke(obj, obj2), null);
                Log.i(TAG, "slow motion / timelapse enabled");
            }
        } catch (Throwable th) {
            Log.w(TAG, "dcu flag: " + th);
        }
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            String str = "1";
            try {
                i = Integer.parseInt(defaultSharedPreferences.getAll().containsKey(PREF_STAB) ? String.valueOf(defaultSharedPreferences.getAll().get(PREF_STAB)) : "1");
            } catch (Throwable th2) {
                i = 1;
            }
            SharedPreferences.Editor editorPutString = defaultSharedPreferences.edit().putString("pref_video_stabilization_key", i == 0 ? "0" : "1");
            if (i < 2) {
                str = "0";
            }
            editorPutString.putString("pref_ois_key", str).apply();
        } catch (Throwable th3) {
            Log.w(TAG, "stab pref: " + th3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void installOverlay(final Activity activity) {
        try {
            final ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();
            if (viewGroup.findViewWithTag("starlight_overlay") != null) {
                return;
            }
            View viewFindViewWithTag = viewGroup.findViewWithTag("previewUi");
            if ((viewFindViewWithTag instanceof ViewGroup) && viewFindViewWithTag.findViewWithTag("starlight_scrim") == null) {
                ViewGroup viewGroup2 = (ViewGroup) viewFindViewWithTag;
                View view = new View(activity);
                view.setTag("starlight_scrim");
                view.setBackground(drawable(activity, "starlight_scrim_top"));
                view.setClickable(false);
                view.setFocusable(false);
                view.setImportantForAccessibility(2);
                viewGroup2.addView(view, 0, new FrameLayout.LayoutParams(-1, dp(activity, 180), 48));
                View view2 = new View(activity);
                view2.setTag("starlight_scrim");
                view2.setBackground(drawable(activity, "starlight_scrim_bottom"));
                view2.setClickable(false);
                view2.setFocusable(false);
                view2.setImportantForAccessibility(2);
                viewGroup2.addView(view2, 0, new FrameLayout.LayoutParams(-1, dp(activity, 220), 80));
            }
            FrameLayout frameLayout = new FrameLayout(activity);
            frameLayout.setTag("starlight_overlay");
            int iDp = dp(activity, 24);
            Drawable drawable = prefBool(activity, PREF_GLASS, true) ? drawable(activity, "starlight_stats_bg_v2") : null;
            if (drawable != null) {
                frameLayout.setBackground(drawable);
            } else {
                frameLayout.setBackground(solidPill(activity, 1343825464, 1353242879));
            }
            frameLayout.setClickable(false);
            frameLayout.setFocusable(false);
            frameLayout.setElevation(dp(activity, 4));
            frameLayout.setClipToOutline(true);
            frameLayout.setOutlineProvider(new ViewOutlineProvider() { // from class: com.starlight.camera.StarlightProvider.1
                @Override // android.view.ViewOutlineProvider
                public void getOutline(View view3, Outline outline) {
                    outline.setRoundRect(0, 0, view3.getWidth(), view3.getHeight(), view3.getHeight() / 2.0f);
                }
            });
            View view3 = new View(activity);
            Drawable drawable2 = drawable(activity, "starlight_shimmer");
            if (drawable2 != null) {
                view3.setBackground(drawable2);
            }
            view3.setAlpha(0.65f);
            view3.setImportantForAccessibility(2);
            frameLayout.addView(view3, new FrameLayout.LayoutParams(dp(activity, 110), iDp, 8388627));
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(16);
            int iDp2 = dp(activity, 13);
            linearLayout.setPadding(iDp2, 0, iDp2, 0);
            View view4 = new View(activity);
            Drawable drawable3 = drawable(activity, "starlight_dot");
            if (drawable3 == null) {
                drawable3 = solidOrb(activity);
            }
            view4.setBackground(drawable3);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(activity, 8), dp(activity, 8));
            layoutParams.rightMargin = dp(activity, 8);
            linearLayout.addView(view4, layoutParams);
            final TextView textView = new TextView(activity);
            textView.setTextColor(-853761);
            textView.setTextSize(2, 10.0f);
            textView.setTypeface(Typeface.create("sans-serif-medium", 0));
            textView.setLetterSpacing(0.03f);
            textView.setIncludeFontPadding(false);
            textView.setShadowLayer(4.0f, 0.0f, 1.0f, Integer.MIN_VALUE);
            linearLayout.addView(textView, new LinearLayout.LayoutParams(-2, -2));
            frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
            View view5 = new View(activity);
            Drawable drawable4 = drawable(activity, "starlight_glass_glow");
            if (drawable4 != null) {
                view5.setBackground(drawable4);
            }
            view5.setClickable(false);
            view5.setFocusable(false);
            view5.setImportantForAccessibility(2);
            int i = topInsetPx(activity, viewGroup);
            int iMin = Math.min(Math.max(dp(activity, 4) + i, dp(activity, 36)), dp(activity, 38));
            viewGroup.addView(view5, new FrameLayout.LayoutParams(dp(activity, 200), dp(activity, 38), 49));
            anchorTopOnScreen(viewGroup, view5, iMin - dp(activity, 4));
            viewGroup.addView(frameLayout, new FrameLayout.LayoutParams(-2, iDp, 49));
            anchorTopOnScreen(viewGroup, frameLayout, iMin);
            final TextView textViewMakeChip = makeChip(activity, "\u2726", 20.0f, prefBool(activity, PREF_GLASS, true));
            textViewMakeChip.setTag("starlight_upscale_button");
            textViewMakeChip.setMinWidth(dp(activity, 56));
            textViewMakeChip.setMinHeight(dp(activity, 56));
            textViewMakeChip.setPadding(0, 0, 0, 0);
            textViewMakeChip.setGravity(17);
            textViewMakeChip.setLetterSpacing(0.0f);
            textViewMakeChip.setElevation(dp(activity, 6));
            textViewMakeChip.setOnClickListener(new View.OnClickListener() { // from class: com.starlight.camera.StarlightProvider.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view6) {
                    boolean video = StarlightProvider.isVideoMode(viewGroup);
                    String key = video ? StarlightProvider.PREF_VIDEO_UPSCALE : StarlightProvider.PREF_UPSCALE;
                    boolean zPrefBool = StarlightProvider.prefBool(activity, key, false);
                    boolean zNew = !zPrefBool;
                    StarlightProvider.prefEdit(activity).putBoolean(key, zNew).apply();
                    int iMax = Math.max(2, Math.min(4, StarlightProvider.prefInt(activity, StarlightProvider.PREF_UPSCALE_SCALE, 2)));
                    if (StarlightProvider.sScaleToast != null) {
                        StarlightProvider.sScaleToast.cancel();
                    }
                    String label = video
                            ? (zNew ? "Video SR ON \u00b7 " + iMax + "x auto" : "Video SR OFF \u00b7 hold to process last video")
                            : (zNew ? "Starlight SR ON \u00b7 " + iMax + "x auto" : "Starlight SR OFF \u00b7 hold to process last photo");
                    Toast unused = StarlightProvider.sScaleToast = Toast.makeText(activity, label, 0);
                    StarlightProvider.sScaleToast.show();
                    StarlightProvider.updateUpscaleChip(activity, textViewMakeChip, video);
                    StarlightProvider.animateChipPulse(textViewMakeChip);
                }
            });
            textViewMakeChip.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.starlight.camera.StarlightProvider.3
                @Override // android.view.View.OnLongClickListener
                public boolean onLongClick(View view6) {
                    if (StarlightProvider.isVideoMode(viewGroup)) {
                        StarlightProvider.this.triggerLatestVideoUpscale(activity);
                    } else {
                        StarlightProvider.this.triggerLatestUpscale(activity);
                    }
                    return true;
                }
            });
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(dp(activity, 56), dp(activity, 56), 8388659);
            layoutParams2.leftMargin = dp(activity, 11);
            layoutParams2.topMargin = dp(activity, 607);
            viewGroup.addView(textViewMakeChip, layoutParams2);
            if (!prefBool(activity, PREF_UPSCALE_BUTTON, true)) {
                textViewMakeChip.setVisibility(View.GONE);
            }
            positionUnderRes(viewGroup, textViewMakeChip);
            textViewMakeChip.bringToFront();
            StarlightProcessingBar processingBar = new StarlightProcessingBar(activity);
            processingBar.setTag("starlight_processing_bar");
            processingBar.setVisibility(View.GONE);
            processingBar.setClickable(false);
            processingBar.setFocusable(false);
            processingBar.setImportantForAccessibility(2);
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, dp(activity, 6), 80);
            barParams.leftMargin = dp(activity, 20);
            barParams.rightMargin = dp(activity, 20);
            barParams.bottomMargin = dp(activity, 36);
            viewGroup.addView(processingBar, barParams);
            updateUpscaleChip(activity, textViewMakeChip, isVideoMode(viewGroup));
            Log.i(TAG, "overlay capTop=" + iMin + "px insetTop=" + i + "px capH=" + iDp + "px");
            if (prefBool(activity, PREF_MOTION, true)) {
                startPulse(view4);
                startGlow(view5);
                startShimmer(view3, frameLayout);
                bindTilt(view5);
            }
            Log.i(TAG, "stats overlay installed");
            final int identifier = activity.getResources().getIdentifier("washington_ui", "id", PKG);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.4
                @Override // java.lang.Runnable
                public void run() {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        try {
                            boolean z = true;
                            boolean zPrefBool = StarlightProvider.prefBool(activity, StarlightProvider.PREF_OVERLAY, true);
                            textView.setVisibility(zPrefBool ? 0 : 8);
                            if (zPrefBool) {
                                textView.setText(StarlightProvider.readStatsSpanned(activity));
                            }
                            View viewFindViewById = identifier != 0 ? viewGroup.findViewById(identifier) : null;
                            boolean z2 = viewFindViewById != null && viewFindViewById.getVisibility() == 0;
                            if (z2) {
                                StarlightProvider.nudgeVideoQuickButton(viewGroup);
                            }
                            StarlightProvider.installPreviewOpener(activity, viewGroup);
                            StarlightProvider.updateProgressUi(activity, viewGroup);
                            z = StarlightProvider.prefBool(activity, StarlightProvider.PREF_UPSCALE_BUTTON, true);
                            textViewMakeChip.setVisibility(z ? 0 : 8);
                            if (z) {
                                int iMax = Math.max(2, Math.min(4, StarlightProvider.prefInt(activity, StarlightProvider.PREF_UPSCALE_SCALE, 2)));
                                String activeKey = z2 ? StarlightProvider.PREF_VIDEO_UPSCALE : StarlightProvider.PREF_UPSCALE;
                                boolean zPrefBool2 = StarlightProvider.prefBool(activity, activeKey, false);
                                if (textViewMakeChip.isSelected() != zPrefBool2 || !textViewMakeChip.getText().toString().contains(iMax + "x")) {
                                    StarlightProvider.updateUpscaleChip(activity, textViewMakeChip, z2);
                                }
                            }
                        } catch (Throwable th) {
                            Log.w(StarlightProvider.TAG, "overlay: " + th);
                        }
                        handler.postDelayed(this, 700L);
                    }
                }
            });
        } catch (Throwable th) {
            Log.e(TAG, "installOverlay", th);
        }
    }

    private static LinearLayout createPanel(Activity activity, View.OnClickListener onClickListener, TextView[] textViewArr, TextView[] textViewArr2, TextView[] textViewArr3, boolean z) {
        int i;
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setTag("starlight_dock_panel");
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 14));
        Drawable drawable = z ? drawable(activity, "starlight_glass_panel") : null;
        if (drawable != null) {
            linearLayout.setBackground(drawable);
        } else {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(-234219998);
            gradientDrawable.setCornerRadius(dp(activity, 30));
            gradientDrawable.setStroke(dp(activity, 1), 1124073471);
            linearLayout.setBackground(gradientDrawable);
        }
        LinearLayout linearLayout2 = new LinearLayout(activity);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        LinearLayout linearLayout3 = new LinearLayout(activity);
        linearLayout3.setOrientation(1);
        linearLayout3.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView textView = new TextView(activity);
        textView.setText("STARLIGHT");
        textView.setTextSize(2, 15.0f);
        textView.setTypeface(Typeface.create("sans-serif-black", 0));
        textView.setTextColor(-854017);
        textView.setLetterSpacing(0.14f);
        LinearLayout linearLayout4 = new LinearLayout(activity);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(16);
        View view = new View(activity);
        Drawable drawable2 = drawable(activity, "starlight_dot");
        if (drawable2 != null) {
            view.setBackground(drawable2);
        } else {
            view.setBackgroundColor(-9443329);
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(activity, 6), dp(activity, 6));
        layoutParams.rightMargin = dp(activity, 8);
        linearLayout4.addView(view, layoutParams);
        linearLayout4.addView(textView);
        TextView textView2 = new TextView(activity);
        textView2.setText("LIQUID GLASS");
        textView2.setTextSize(2, 8.5f);
        textView2.setLetterSpacing(0.22f);
        textView2.setTextColor(-9443329);
        textView2.setTypeface(Typeface.create("sans-serif-medium", 0));
        linearLayout3.addView(linearLayout4);
        linearLayout3.addView(textView2);
        if (prefBool(activity, PREF_MOTION, true)) {
            startPulse(view);
        }
        linearLayout2.addView(linearLayout3);
        TextView textViewMakeChip = makeChip(activity, "×", 18.0f, z);
        textViewMakeChip.setTag("close");
        textViewMakeChip.setMinWidth(dp(activity, 34));
        textViewMakeChip.setMinHeight(dp(activity, 34));
        textViewMakeChip.setOnClickListener(onClickListener);
        linearLayout2.addView(textViewMakeChip);
        linearLayout.addView(linearLayout2);
        linearLayout.addView(divider(activity));
        linearLayout.addView(sectionLabel(activity, "QUICK CONTROLS"));
        String[] strArr = {"OVERLAY", "STAB", "HDR", "FLASH", "TIMER ▸", "SETTINGS"};
        LinearLayout linearLayoutChipRow = chipRow(activity);
        int i2 = 0;
        while (true) {
            if (i2 >= 3) {
                break;
            }
            textViewArr[i2] = makeChip(activity, strArr[i2], 8.5f, z);
            textViewArr[i2].setTag("toggle:" + i2);
            textViewArr[i2].setOnClickListener(onClickListener);
            linearLayoutChipRow.addView(textViewArr[i2], weight(activity));
            i2++;
        }
        linearLayout.addView(linearLayoutChipRow);
        LinearLayout linearLayoutChipRow2 = chipRow(activity);
        for (i = 3; i < 6; i++) {
            textViewArr[i] = makeChip(activity, strArr[i], 8.5f, z);
            textViewArr[i].setTag("toggle:" + i);
            textViewArr[i].setOnClickListener(onClickListener);
            linearLayoutChipRow2.addView(textViewArr[i], weight(activity));
        }
        linearLayout.addView(linearLayoutChipRow2);
        linearLayout.addView(sectionLabel(activity, "SENSOR"));
        String[] strArr2 = {"DSLR", "MACRO", "WIDE", "B&W", "108"};
        LinearLayout linearLayoutChipRow3 = chipRow(activity);
        for (int i3 = 0; i3 < 5; i3++) {
            textViewArr2[i3] = makeChip(activity, strArr2[i3], 8.0f, z);
            textViewArr2[i3].setTag("sensor:" + i3);
            textViewArr2[i3].setOnClickListener(onClickListener);
            linearLayoutChipRow3.addView(textViewArr2[i3], weight(activity));
        }
        linearLayout.addView(linearLayoutChipRow3);
        linearLayout.addView(sectionLabel(activity, "ZOOM"));
        String[] strArr3 = {"0.6", "1", "2", "5", "10", "30"};
        LinearLayout linearLayoutChipRow4 = chipRow(activity);
        for (int i4 = 0; i4 < 6; i4++) {
            textViewArr3[i4] = makeChip(activity, strArr3[i4], 8.5f, z);
            textViewArr3[i4].setTag("zoom:" + ZOOM_STOPS[i4]);
            textViewArr3[i4].setOnClickListener(onClickListener);
            linearLayoutChipRow4.addView(textViewArr3[i4], weight(activity));
        }
        linearLayout.addView(linearLayoutChipRow4);
        TextView textView3 = new TextView(activity);
        textView3.setText("STARLIGHT  •  SWEETIN");
        textView3.setTextSize(2, 7.5f);
        textView3.setLetterSpacing(0.16f);
        textView3.setTextColor(1725425138);
        textView3.setGravity(17);
        textView3.setPadding(0, dp(activity, 10), 0, 0);
        linearLayout.addView(textView3);
        updateToggleChips(activity, textViewArr);
        updateSensorChips(activity, textViewArr2);
        updateZoomChips(activity, textViewArr3);
        return linearLayout;
    }

    private static TextView makeChip(Context context, String str, float f, boolean z) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(2, f);
        textView.setGravity(17);
        textView.setTypeface(Typeface.create("sans-serif-medium", 0));
        textView.setLetterSpacing(0.04f);
        textView.setTextColor(-856492033);
        textView.setPadding(dp(context, 6), 0, dp(context, 6), 0);
        Drawable drawable = z ? drawable(context, "starlight_glass_chip") : null;
        if (drawable == null) {
            drawable = solidPill(context, -16116190, 872415231);
        }
        textView.setBackground(ripple(drawable));
        textView.setMinHeight(dp(context, 34));
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setOnTouchListener(new View.OnTouchListener() { // from class: com.starlight.camera.StarlightProvider.5
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == 0) {
                    view.performHapticFeedback(1);
                    view.animate().scaleX(0.94f).scaleY(0.94f).setInterpolator(new DecelerateInterpolator()).setDuration(70L).start();
                    return false;
                }
                if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setInterpolator(new OvershootInterpolator(1.3f)).setDuration(160L).start();
                    return false;
                }
                return false;
            }
        });
        return textView;
    }

    private static Drawable solidPill(Context context, int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, 100));
        gradientDrawable.setStroke(dp(context, 1), i2);
        return gradientDrawable;
    }

    private static Drawable solidOrb(Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(1);
        gradientDrawable.setColor(-15921382);
        gradientDrawable.setStroke(dp(context, 1), -2137072641);
        return gradientDrawable;
    }

    private static Drawable ripple(Drawable drawable) {
        return new RippleDrawable(ColorStateList.valueOf(872415231), drawable, null);
    }

    private static LinearLayout chipRow(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return linearLayout;
    }

    private static LinearLayout.LayoutParams weight(Context context) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(context, 34), 1.0f);
        layoutParams.setMargins(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3));
        return layoutParams;
    }

    private static TextView sectionLabel(Context context, String str) {
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextSize(2, 8.5f);
        textView.setLetterSpacing(0.18f);
        textView.setTextColor(-865081345);
        textView.setTypeface(Typeface.create("sans-serif-medium", 0));
        textView.setPadding(0, dp(context, 12), 0, dp(context, 4));
        return textView;
    }

    private static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(780134399);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(context, 1));
        layoutParams.topMargin = dp(context, 10);
        layoutParams.bottomMargin = dp(context, 2);
        view.setLayoutParams(layoutParams);
        return view;
    }

    private static void onToggle(final Activity activity, int i, TextView[] textViewArr) {
        String str = "off";
        String str2 = "1";
        try {
            if (i == 0) {
                prefEdit(activity).putBoolean(PREF_OVERLAY, prefBool(activity, PREF_OVERLAY, true) ? false : true).apply();
            } else if (i == 1) {
                prefEdit(activity).putString(PREF_STAB, String.valueOf((prefInt(activity, PREF_STAB, 1) + 1) % 3)).apply();
                applyStabPrefs(activity);
            } else if (i != 2) {
                if (i == 3) {
                    String strPrefString = prefString(activity, PREF_FLASH, "off");
                    if ("off".equals(strPrefString)) {
                        str = "auto";
                    } else if ("auto".equals(strPrefString)) {
                        str = "on";
                    }
                    prefEdit(activity).putString(PREF_FLASH, str).putString("pref_camera_flashmode_key", str).apply();
                } else if (i == 4) {
                    closePanel(activity);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.starlight.camera.StarlightProvider.6
                        @Override // java.lang.Runnable
                        public void run() {
                            StarlightProvider.openOptionsMenu(activity);
                        }
                    }, 180L);
                } else if (i == 5) {
                    closePanel(activity);
                    openSettings(activity);
                }
            } else {
                boolean z = "1".equals(prefString(activity, PREF_HDR, "1")) ? false : true;
                SharedPreferences.Editor editorPrefEdit = prefEdit(activity);
                if (!z) {
                    str2 = "0";
                }
                editorPrefEdit.putString(PREF_HDR, str2).putBoolean("hdrnet_enabled", z).apply();
            }
            updateToggleChips(activity, textViewArr);
        } catch (Throwable th) {
            Log.w(TAG, "toggle: " + th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateUpscaleChip(Activity activity, TextView textView) {
        updateUpscaleChip(activity, textView, false);
    }

    public static void updateUpscaleChip(Activity activity, TextView textView, boolean video) {
        if (textView == null) {
            return;
        }
        int iMax = Math.max(2, Math.min(4, prefInt(activity, PREF_UPSCALE_SCALE, 2)));
        boolean zPrefBool = prefBool(activity, video ? PREF_VIDEO_UPSCALE : PREF_UPSCALE, false);
        textView.setSelected(zPrefBool);
        SpannableString label = new SpannableString("\u2726\n" + (video ? "V" : "") + iMax + "x");
        label.setSpan(new RelativeSizeSpan(0.42f), 2, label.length(), 33);
        textView.setText(label);
        textView.setTextColor(zPrefBool ? -16379361 : -420284417);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);
        gradientDrawable.setColor(zPrefBool ? -426776065 : 1712924216);
        gradientDrawable.setStroke(dp(activity, zPrefBool ? 2 : 1), zPrefBool ? -3344385 : 1722341631);
        textView.setBackground(ripple(gradientDrawable));
        String what = video ? "video" : "photo";
        textView.setContentDescription(zPrefBool
                ? "Starlight auto " + what + " upscale on, " + iMax + "x. Tap to turn off, hold to process last " + what
                : "Starlight auto " + what + " upscale off. Tap to turn on, hold to process last " + what);
    }

    private static boolean isVideoMode(ViewGroup viewGroup) {
        try {
            int identifier = viewGroup.getResources().getIdentifier("washington_ui", "id", PKG);
            View view = identifier != 0 ? viewGroup.findViewById(identifier) : null;
            return view != null && view.getVisibility() == View.VISIBLE;
        } catch (Throwable th) {
            return false;
        }
    }

    private static void installPreviewOpener(final Activity activity, ViewGroup viewGroup) {
        try {
            int id = viewGroup.getResources().getIdentifier("thumbnail_button", "id", PKG);
            final View preview = id != 0 ? viewGroup.findViewById(id) : null;
            if (preview == null) {
                return;
            }
            // Install once per thumbnail view. The camera may reset its own
            // click listener, but the touch interceptor is ours and survives;
            // do not churn listeners on every overlay tick.
            if ("starlight_preview_opener".equals(preview.getTag())) {
                return;
            }
            preview.setTag("starlight_preview_opener");
            preview.setClickable(true);
            preview.setContentDescription("Open recent photo or video");
            preview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMediaOpener(activity);
                }
            });
            preview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true;
                }
            });
        } catch (Throwable th) {
            Log.w(TAG, "preview opener: " + th);
        }
    }

    private static void openMediaOpener(Activity activity) {
        String[] mime = new String[1];
        Uri uri = latestMediaUri(activity, mime);
        if (uri == null) {
            Toast.makeText(activity, "No recent photo or video", 0).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime[0] == null ? "image/*" : mime[0]);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        applyMediaOpener(intent);
        try {
            activity.startActivity(intent);
        } catch (Throwable th) {
            try {
                Intent chooser = Intent.createChooser(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, mime[0] == null ? "image/*" : mime[0])
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Open with");
                activity.startActivity(chooser);
            } catch (Throwable th2) {
                Toast.makeText(activity, "No gallery app found", 0).show();
            }
        }
    }

    private static Uri latestMediaUri(Context context, String[] mimeOut) {
        Uri image = null;
        Uri video = null;
        long imageTime = -1L;
        long videoTime = -1L;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"_id", "date_added"},
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?",
                    new String[]{"Camera"}, "date_added DESC");
            if (cursor != null && cursor.moveToFirst()) {
                imageTime = cursor.getLong(1);
                image = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        try {
            cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{"_id", "date_added"},
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME + "=?",
                    new String[]{"Camera"}, "date_added DESC");
            if (cursor != null && cursor.moveToFirst()) {
                videoTime = cursor.getLong(1);
                video = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        if (image != null && imageTime >= videoTime) {
            mimeOut[0] = "image/*";
            return image;
        }
        if (video != null) {
            mimeOut[0] = "video/*";
            return video;
        }
        mimeOut[0] = "image/*";
        return image;
    }

    private static void updateProgressUi(Activity activity, ViewGroup viewGroup) {
        try {
            boolean running = sPhotoProcessing || StarlightVideoSR.isProcessing();
            int pct = running ? (sPhotoProcessing ? sPhotoProgress : StarlightVideoSR.progress()) : 0;
            StarlightProcessingBar bar = (StarlightProcessingBar) viewGroup.findViewWithTag("starlight_processing_bar");
            if (bar != null) {
                bar.setProgress(pct);
                bar.setRunning(running);
                bar.setVisibility(running ? View.VISIBLE : View.GONE);
            }
            if (running) {
                StarlightProgressNotifier.update(activity, sPhotoProcessing ? "Starlight Photo SR" : "Starlight Video SR", pct);
            } else {
                StarlightProgressNotifier.stop(activity);
            }
        } catch (Throwable th) {
            Log.w(TAG, "progress ui: " + th);
        }
    }

    public static boolean isPhotoProcessing() {
        return sPhotoProcessing;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void animateChipPulse(final TextView textView) {
        if (textView == null) {
            return;
        }
        textView.animate().cancel();
        textView.setScaleX(1.0f);
        textView.setScaleY(1.0f);
        textView.animate().scaleX(1.13f).scaleY(1.13f).alpha(0.86f).setDuration(85L).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() { // from class: com.starlight.camera.StarlightProvider.7
            @Override // java.lang.Runnable
            public void run() {
                textView.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(190L).setInterpolator(new OvershootInterpolator(1.7f)).start();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void nudgeVideoQuickButton(ViewGroup viewGroup) {
        try {
            int identifier = viewGroup.getResources().getIdentifier("washington_ui", "id", PKG);
            View viewFindViewById = identifier != 0 ? viewGroup.findViewById(identifier) : null;
            if (viewFindViewById == null) {
                return;
            }
            float f = -dp(viewGroup.getContext(), 50);
            if (viewFindViewById.getTranslationY() != f) {
                viewFindViewById.setTranslationY(f);
            }
        } catch (Throwable th) {
            Log.w(TAG, "nudgeVideoQuickButton: " + th);
        }
    }

    private static void positionUnderRes(final ViewGroup viewGroup, final TextView textView) {
        final int identifier = viewGroup.getResources().getIdentifier("button_resraw", "id", PKG);
        Runnable runnable = new Runnable() { // from class: com.starlight.camera.StarlightProvider.8
            @Override // java.lang.Runnable
            public void run() {
                try {
                    int iDp = StarlightProvider.dp(viewGroup.getContext(), 11);
                    int iDp2 = StarlightProvider.dp(viewGroup.getContext(), 607);
                    View viewFindViewById = identifier != 0 ? viewGroup.findViewById(identifier) : null;
                    if (viewFindViewById == null || viewFindViewById.getVisibility() != 0 || viewFindViewById.getWidth() <= 0 || viewFindViewById.getHeight() <= 0) {
                        int videoId = viewGroup.getResources().getIdentifier("washington_ui", "id", PKG);
                        View videoView = videoId != 0 ? viewGroup.findViewById(videoId) : null;
                        if (videoView != null && videoView.getVisibility() == 0 && videoView.getWidth() > 0 && videoView.getHeight() > 0) {
                            viewFindViewById = videoView;
                        }
                    }
                    if (viewFindViewById != null && viewFindViewById.getVisibility() == 0 && viewFindViewById.getWidth() > 0 && viewFindViewById.getHeight() > 0) {
                        int[] iArr = new int[2];
                        viewFindViewById.getLocationOnScreen(iArr);
                        int width = textView.getWidth();
                        if (width <= 0) {
                            width = StarlightProvider.dp(viewGroup.getContext(), 56);
                        }
                        iDp = iArr[0] + ((viewFindViewById.getWidth() - width) / 2);
                        iDp2 = iArr[1] + viewFindViewById.getHeight() + StarlightProvider.dp(viewGroup.getContext(), 4);
                    }
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
                    layoutParams.gravity = 8388659;
                    layoutParams.leftMargin = Math.max(0, iDp);
                    layoutParams.topMargin = Math.max(0, iDp2);
                    layoutParams.bottomMargin = 0;
                    textView.setLayoutParams(layoutParams);
                } catch (Throwable th) {
                    Log.w(StarlightProvider.TAG, "positionUnderRes: " + th);
                }
            }
        };
        textView.post(runnable);
        textView.postDelayed(runnable, 700L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerLatestUpscale(final Activity activity) {
        Toast.makeText(activity, "Upscaling latest photo", 0).show();
        this.workerHandler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.9
            @Override // java.lang.Runnable
            public void run() {
                Cursor cursorQuery = null;
                try {
                    cursorQuery = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{"_data", "_display_name"}, null, null, "date_added DESC");
                    while (cursorQuery != null && cursorQuery.moveToNext()) {
                        String string = cursorQuery.getString(0);
                        String string2 = cursorQuery.getString(1);
                        if (string != null && string2 != null && !string2.startsWith("Upscaled_") && !string2.contains("_Upscaled")) {
                            File file = new File(string);
                            if (file.exists() && file.length() > 0) {
                                Log.i(StarlightProvider.TAG, "manual upscale latest " + string2);
                                StarlightProvider.this.maybeUpscale(file, true);
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                    return;
                                }
                                return;
                            }
                        }
                    }
                    Log.w(StarlightProvider.TAG, "manual upscale: no photo found");
                    if (cursorQuery == null) {
                    }
                } catch (Throwable th) {
                    try {
                        Log.w(StarlightProvider.TAG, "manual upscale: " + th);
                    } finally {
                        if (0 != 0) {
                            cursorQuery.close();
                        }
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerLatestVideoUpscale(Activity activity) {
        Toast.makeText(activity, "Upscaling latest video (experimental)", 0).show();
        StarlightVideoSR.processLatest(activity);
    }

    private static void applyStabPrefs(Context context) {
        try {
            int iPrefInt = prefInt(context, PREF_STAB, 1);
            prefEdit(context).putString("pref_video_stabilization_key", iPrefInt == 0 ? "0" : "1").putString("pref_ois_key", iPrefInt >= 2 ? "1" : "0").apply();
        } catch (Throwable th) {
        }
    }

    private static void updateToggleChips(Activity activity, TextView[] textViewArr) {
        if (textViewArr == null || textViewArr.length < 5) {
            return;
        }
        setChip(textViewArr[0], "OVERLAY", prefBool(activity, PREF_OVERLAY, true));
        setChip(textViewArr[1], "STAB", prefInt(activity, PREF_STAB, 1) != 0);
        setChip(textViewArr[2], "HDR", "1".equals(prefString(activity, PREF_HDR, "1")));
        setChip(textViewArr[3], "FLASH", !"off".equals(prefString(activity, PREF_FLASH, "off")));
        setChip(textViewArr[4], "TIMER ▸", false);
        if (textViewArr.length > 5) {
            setChip(textViewArr[5], "SETTINGS", false);
        }
    }

    private static void setChip(TextView textView, String str, boolean z) {
        if (textView == null) {
            return;
        }
        textView.setText(str);
        textView.setSelected(z);
        textView.setTextColor(z ? -1 : -856492033);
    }

    private static void updateSensorChips(Activity activity, TextView[] textViewArr) {
        int iPrefInt = prefInt(activity, PREF_AUX, 0);
        int i = 0;
        while (i < textViewArr.length) {
            if (textViewArr[i] != null) {
                boolean z = i == iPrefInt;
                textViewArr[i].setSelected(z);
                textViewArr[i].setTextColor(z ? -1 : -856492033);
            }
            i++;
        }
    }

    private static void updateZoomChips(Activity activity, TextView[] textViewArr) {
        double dCurrentZoom = currentZoom(activity);
        double d = 1.0E9d;
        int i = 0;
        for (int i2 = 0; i2 < ZOOM_STOPS.length; i2++) {
            double dAbs = Math.abs(ZOOM_STOPS[i2] - dCurrentZoom);
            if (dAbs < d) {
                i = i2;
                d = dAbs;
            }
        }
        int i3 = 0;
        while (i3 < textViewArr.length) {
            if (textViewArr[i3] != null) {
                boolean z = i3 == i;
                textViewArr[i3].setSelected(z);
                textViewArr[i3].setTextColor(z ? -1 : -856492033);
            }
            i3++;
        }
    }

    private static double currentZoom(Activity activity) {
        try {
            int identifier = activity.getResources().getIdentifier("zoom_slider", "id", PKG);
            View viewFindViewById = identifier != 0 ? activity.findViewById(identifier) : null;
            if (viewFindViewById != null && viewFindViewById.getContentDescription() != null) {
                Matcher matcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(viewFindViewById.getContentDescription().toString());
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
                return 1.0d;
            }
            return 1.0d;
        } catch (Throwable th) {
            return 1.0d;
        }
    }

    private static SeekBar findZoomSeekBar(View view, int i) {
        SeekBar seekBar = null;
        if (view == null) {
            return null;
        }
        if ((view instanceof SeekBar) && view.getId() == i) {
            seekBar = (SeekBar) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                SeekBar seekBarFindZoomSeekBar = findZoomSeekBar(viewGroup.getChildAt(i2), i);
                if (seekBarFindZoomSeekBar != null && (seekBar == null || zoomScore(seekBarFindZoomSeekBar) > zoomScore(seekBar))) {
                    seekBar = seekBarFindZoomSeekBar;
                }
            }
        }
        return seekBar;
    }

    private static int zoomScore(SeekBar seekBar) {
        return (seekBar.getVisibility() == 0 ? 1000000 : 0) + seekBar.getMax();
    }

    private static void applyZoom(Activity activity, double d) {
        try {
            int identifier = activity.getResources().getIdentifier("zoom_slider", "id", PKG);
            SeekBar seekBarFindZoomSeekBar = identifier != 0 ? findZoomSeekBar(activity.getWindow().getDecorView(), identifier) : null;
            if (seekBarFindZoomSeekBar == null) {
                Log.w(TAG, "zoom slider missing");
                return;
            }
            int max = seekBarFindZoomSeekBar.getMax();
            boolean z = max != 100000;
            if (z) {
                seekBarFindZoomSeekBar.setMax(100000);
            }
            int max2 = seekBarFindZoomSeekBar.getMax();
            double d2 = ZOOM_STOPS[0];
            double d3 = ZOOM_STOPS[ZOOM_STOPS.length - 1];
            boolean z2 = z;
            int iMax = Math.max(0, Math.min(max2, (int) Math.round((((double) max2) * Math.log(Math.max(d2, Math.min(d3, d)) / d2)) / Math.log(d3 / d2))));
            Field declaredField = SeekBar.class.getDeclaredField("mOnSeekBarChangeListener");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(seekBarFindZoomSeekBar);
            if (obj == null) {
                if (z2) {
                    seekBarFindZoomSeekBar.setMax(max);
                }
                Log.w(TAG, "zoom listener null");
                return;
            }
            Method method = obj.getClass().getMethod("onStartTrackingTouch", SeekBar.class);
            Method method2 = obj.getClass().getMethod("onProgressChanged", SeekBar.class, Integer.TYPE, Boolean.TYPE);
            Method method3 = obj.getClass().getMethod("onStopTrackingTouch", SeekBar.class);
            method.setAccessible(true);
            method2.setAccessible(true);
            method3.setAccessible(true);
            try {
                method.invoke(obj, seekBarFindZoomSeekBar);
                seekBarFindZoomSeekBar.setProgress(iMax);
                method2.invoke(obj, seekBarFindZoomSeekBar, Integer.valueOf(iMax), true);
                method3.invoke(obj, seekBarFindZoomSeekBar);
                if (z2) {
                    seekBarFindZoomSeekBar.setMax(max);
                }
                Log.i(TAG, "zoom " + d + " max " + max2 + " progress " + iMax + " restored " + seekBarFindZoomSeekBar.getMax() + " cls " + seekBarFindZoomSeekBar.getClass().getName());
            } catch (Throwable th) {
                if (z2) {
                    seekBarFindZoomSeekBar.setMax(max);
                }
                throw th;
            }
        } catch (Throwable th2) {
            Log.w(TAG, "zoom: " + th2);
        }
    }

    private static void selectSensor(Activity activity, int i) {
        try {
            prefEdit(activity).putString(PREF_AUX, String.valueOf(i)).apply();
            int identifier = activity.getResources().getIdentifier(new String[]{"AuxButtonMain", "AuxButtonTele", "AuxButtonWide", "AuxButtonInfinity", "AuxButton5"}[i], "id", PKG);
            View viewFindViewById = identifier != 0 ? activity.findViewById(identifier) : null;
            if (viewFindViewById != null) {
                Log.i(TAG, "sensor select " + i);
                viewFindViewById.performClick();
            } else {
                Log.w(TAG, "sensor view missing " + i);
            }
        } catch (Throwable th) {
            Log.w(TAG, "sensor: " + th);
        }
        closePanel(activity);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void openOptionsMenu(Activity activity) {
        try {
            String[] strArr = {"options_menu_closed", "options_menu_container", "options_menu_button"};
            for (int i = 0; i < 3; i++) {
                String str = strArr[i];
                int identifier = activity.getResources().getIdentifier(str, "id", PKG);
                View viewFindViewById = identifier != 0 ? activity.findViewById(identifier) : null;
                if (viewFindViewById != null) {
                    viewFindViewById.performClick();
                    Log.i(TAG, "options menu click " + str);
                    return;
                }
            }
            Log.w(TAG, "options menu view missing");
        } catch (Throwable th) {
            Log.w(TAG, "options: " + th);
        }
    }

    private static void togglePanel(Activity activity, View view) {
        if (view.getVisibility() == 0) {
            closePanel(activity);
            return;
        }
        view.setAlpha(0.0f);
        view.setTranslationY(-dp(activity, 10));
        view.setVisibility(0);
        view.animate().alpha(1.0f).translationY(0.0f).setDuration(180L).start();
    }

    private static void closePanel(Activity activity) {
        try {
            final View viewFindViewWithTag = activity.getWindow().getDecorView().findViewWithTag("starlight_dock_panel");
            if (viewFindViewWithTag != null && viewFindViewWithTag.getVisibility() == 0) {
                viewFindViewWithTag.animate().alpha(0.0f).translationY(-dp(activity, 8)).setDuration(140L).withEndAction(new Runnable() { // from class: com.starlight.camera.StarlightProvider.10
                    @Override // java.lang.Runnable
                    public void run() {
                        viewFindViewWithTag.setVisibility(8);
                        viewFindViewWithTag.setAlpha(1.0f);
                        viewFindViewWithTag.setTranslationY(0.0f);
                    }
                }).start();
            }
        } catch (Throwable th) {
        }
    }

    private static void openSettings(Activity activity) {
        try {
            Intent intent = new Intent();
            intent.setClassName(activity, "com.google.android.apps.camera.legacy.app.settings.CameraSettingsActivity");
            activity.startActivity(intent);
            Log.i(TAG, "settings opened");
        } catch (Throwable th) {
            Log.w(TAG, "settings: " + th);
        }
    }

    private static void startPulse(final View view) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(1200L);
        valueAnimatorOfFloat.setRepeatCount(-1);
        valueAnimatorOfFloat.setRepeatMode(2);
        valueAnimatorOfFloat.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.starlight.camera.StarlightProvider.11
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                view.setAlpha((0.65f * fFloatValue) + 0.35f);
                float f = (fFloatValue * 0.35f) + 0.85f;
                view.setScaleX(f);
                view.setScaleY(f);
            }
        });
        valueAnimatorOfFloat.start();
    }

    private static void startShimmer(final View view, final View view2) {
        final Runnable[] runnableArr = new Runnable[1];
        runnableArr[0] = new Runnable() { // from class: com.starlight.camera.StarlightProvider.12
            @Override // java.lang.Runnable
            public void run() {
                int width = view2.getWidth();
                if (width > 0) {
                    ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, "translationX", -StarlightProvider.dp(view.getContext(), 90), width + StarlightProvider.dp(view.getContext(), 20));
                    objectAnimatorOfFloat.setDuration(2800L);
                    objectAnimatorOfFloat.setStartDelay(400L);
                    objectAnimatorOfFloat.setRepeatCount(-1);
                    objectAnimatorOfFloat.setRepeatMode(1);
                    objectAnimatorOfFloat.setInterpolator(new LinearInterpolator());
                    objectAnimatorOfFloat.start();
                    return;
                }
                view2.postDelayed(runnableArr[0], 80L);
            }
        };
        view2.post(runnableArr[0]);
    }

    private static void startGlow(final View view) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(1900L);
        valueAnimatorOfFloat.setRepeatCount(-1);
        valueAnimatorOfFloat.setRepeatMode(2);
        valueAnimatorOfFloat.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.starlight.camera.StarlightProvider.13
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                view.setAlpha((0.35f * fFloatValue) + 0.2f);
                float f = (fFloatValue * 0.3f) + 0.85f;
                view.setScaleX(f);
                view.setScaleY(f);
            }
        });
        valueAnimatorOfFloat.start();
    }

    private static void bindTilt(final View view) {
        try {
            final SensorManager sensorManager = (SensorManager) view.getContext().getSystemService("sensor");
            final Sensor defaultSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(1);
            if (defaultSensor == null) {
                return;
            }
            final float[] fArr = new float[2];
            final SensorEventListener sensorEventListener = new SensorEventListener() { // from class: com.starlight.camera.StarlightProvider.14
                @Override // android.hardware.SensorEventListener
                public void onSensorChanged(SensorEvent sensorEvent) {
                    float fMax = Math.max(-1.0f, Math.min(1.0f, (-sensorEvent.values[0]) / 9.81f));
                    float fMax2 = Math.max(-1.0f, Math.min(1.0f, sensorEvent.values[1] / 9.81f));
                    fArr[0] = (fArr[0] * 0.86f) + (fMax * 0.14f);
                    fArr[1] = (fArr[1] * 0.86f) + (fMax2 * 0.14f);
                    view.setTranslationX(fArr[0] * StarlightProvider.dp(view.getContext(), 11));
                    view.setTranslationY(fArr[1] * StarlightProvider.dp(view.getContext(), 7));
                }

                @Override // android.hardware.SensorEventListener
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            final boolean[] zArr = {false};
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: com.starlight.camera.StarlightProvider.15
                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewAttachedToWindow(View view2) {
                    if (!zArr[0]) {
                        sensorManager.registerListener(sensorEventListener, defaultSensor, 2);
                        zArr[0] = true;
                    }
                }

                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewDetachedFromWindow(View view2) {
                    if (zArr[0]) {
                        sensorManager.unregisterListener(sensorEventListener);
                        zArr[0] = false;
                    }
                }
            });
            if (view.isAttachedToWindow()) {
                sensorManager.registerListener(sensorEventListener, defaultSensor, 2);
                zArr[0] = true;
            }
        } catch (Throwable th) {
            Log.w(TAG, "tilt: " + th);
        }
    }

    private static void applyGlassIntensity(Activity activity, View view) {
        try {
            int i = 255;
            if (!prefBool(activity, PREF_GLASS, true)) {
                Drawable background = view.getBackground();
                if (background != null) {
                    background.mutate().setAlpha(255);
                    return;
                }
                return;
            }
            int iPrefInt = prefInt(activity, PREF_GLASS_INTENSITY, 1);
            if (iPrefInt <= 0) {
                i = 225;
            } else if (iPrefInt < 2) {
                i = 242;
            }
            Drawable background2 = view.getBackground();
            if (background2 != null) {
                background2.mutate().setAlpha(i);
            }
        } catch (Throwable th) {
        }
    }

    private static Drawable drawable(Context context, String str) {
        try {
            int identifier = context.getResources().getIdentifier(str, "drawable", PKG);
            if (identifier != 0) {
                return context.getResources().getDrawable(identifier, context.getTheme());
            }
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private static void migrateBooleanPrefs(Context context) {
        try {
            String[] keys = new String[]{
                    PREF_SMART, PREF_UPSCALE, PREF_UPSCALE_BUTTON,
                    PREF_VIDEO_UPSCALE, PREF_OVERLAY, PREF_GLASS, PREF_MOTION
            };
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            boolean changed = false;
            for (String key : keys) {
                Object value = prefs.getAll().get(key);
                if (value instanceof String) {
                    String text = (String) value;
                    if ("1".equals(text) || "true".equalsIgnoreCase(text)) {
                        editor.putBoolean(key, true);
                        changed = true;
                    } else if ("0".equals(text) || "false".equalsIgnoreCase(text)) {
                        editor.putBoolean(key, false);
                        changed = true;
                    }
                }
            }
            if (changed) {
                editor.apply();
                Log.i(TAG, "migrated boolean prefs");
            }
        } catch (Throwable th) {
            Log.w(TAG, "boolean migration: " + th);
        }
    }

    private static void migrateUpscaleScale(Context context) {
        Object obj;
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (defaultSharedPreferences.contains(PREF_UPSCALE_SCALE) || (obj = defaultSharedPreferences.getAll().get(PREF_UPSCALE_SCALE_LEGACY)) == null) {
                return;
            }
            try {
                int iMax = Math.max(2, Math.min(4, Integer.parseInt(String.valueOf(obj))));
                defaultSharedPreferences.edit().putString(PREF_UPSCALE_SCALE, String.valueOf(iMax)).remove(PREF_UPSCALE_SCALE_LEGACY).apply();
                Log.i(TAG, "migrated upscale scale " + obj + " -> " + iMax);
            } catch (Throwable th) {
            }
        } catch (Throwable th2) {
            Log.w(TAG, "scale migration: " + th2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static SharedPreferences.Editor prefEdit(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
    }

    private static String prefString(Context context, String str, String str2) {
        Object obj = PreferenceManager.getDefaultSharedPreferences(context).getAll().get(str);
        return obj == null ? str2 : String.valueOf(obj);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int prefInt(Context context, String str, int i) {
        try {
            return Integer.parseInt(prefString(context, str, String.valueOf(i)));
        } catch (Throwable th) {
            return i;
        }
    }

    private static String sensorLabel(Activity activity) {
        return "• " + new String[]{"DSLR", "MACRO", "WIDE", "B&W", "108MP"}[Math.max(0, Math.min(4, prefInt(activity, PREF_AUX, 0)))];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int dp(Context context, int i) {
        return (int) TypedValue.applyDimension(1, i, context.getResources().getDisplayMetrics());
    }

    private static int statusBarHeight(Context context) {
        try {
            int identifier = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (identifier > 0) {
                return context.getResources().getDimensionPixelSize(identifier);
            }
        } catch (Throwable th) {
        }
        return dp(context, 28);
    }

    private static int topInsetPx(Context context, View view) {
        if (view != null) {
            try {
                WindowInsets rootWindowInsets = view.getRootWindowInsets();
                if (rootWindowInsets != null) {
                    DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
                    if (displayCutout != null && displayCutout.getSafeInsetTop() > 0) {
                        return displayCutout.getSafeInsetTop();
                    }
                    int systemWindowInsetTop = rootWindowInsets.getSystemWindowInsetTop();
                    if (systemWindowInsetTop > 0) {
                        return systemWindowInsetTop;
                    }
                }
            } catch (Throwable th) {
            }
        }
        int iStatusBarHeight = statusBarHeight(context);
        return (iStatusBarHeight <= 0 || iStatusBarHeight > dp(context, 48)) ? dp(context, 34) : iStatusBarHeight;
    }

    private static void anchorTopOnScreen(final View view, final View view2, final int i) {
        final Runnable runnable = new Runnable() { // from class: com.starlight.camera.StarlightProvider.16
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ViewGroup.LayoutParams layoutParams = view2.getLayoutParams();
                    if (layoutParams instanceof FrameLayout.LayoutParams) {
                        int[] iArr = new int[2];
                        view.getLocationOnScreen(iArr);
                        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) layoutParams;
                        int iMax = Math.max(0, i - iArr[1]);
                        if (layoutParams2.topMargin != iMax) {
                            layoutParams2.topMargin = iMax;
                            view2.setLayoutParams(layoutParams2);
                        }
                    }
                } catch (Throwable th) {
                }
            }
        };
        view2.post(runnable);
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.starlight.camera.StarlightProvider.17
            @Override // android.view.View.OnLayoutChangeListener
            public void onLayoutChange(View view3, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
                runnable.run();
            }
        });
    }

    private static void statSpan(SpannableStringBuilder spannableStringBuilder, String str, String str2, int i) {
        int length = spannableStringBuilder.length();
        spannableStringBuilder.append((CharSequence) str).append((CharSequence) str2);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(i), length, spannableStringBuilder.length(), 33);
    }

    private static void statSep(SpannableStringBuilder spannableStringBuilder) {
        int length = spannableStringBuilder.length();
        spannableStringBuilder.append(" · ");
        spannableStringBuilder.setSpan(new ForegroundColorSpan(1720759504), length, spannableStringBuilder.length(), 33);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static CharSequence readStatsSpanned(Context context) {
        String str;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        int iCpuPercent = cpuPercent();
        statSpan(spannableStringBuilder, "CPU ", iCpuPercent < 0 ? "--" : iCpuPercent + "%", -8399617);
        statSep(spannableStringBuilder);
        String strGpuText = gpuText();
        int iIndexOf = strGpuText.indexOf(32);
        if (iIndexOf > 0) {
            int i = iIndexOf + 1;
            statSpan(spannableStringBuilder, strGpuText.substring(0, i), strGpuText.substring(i), -7347264);
        } else {
            statSpan(spannableStringBuilder, "GPU ", "--", -7347264);
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            str = memoryInfo.totalMem > 0 ? (((memoryInfo.totalMem - memoryInfo.availMem) * 100) / memoryInfo.totalMem) + "%" : "--";
        } catch (Throwable th) {
            str = "--";
        }
        statSep(spannableStringBuilder);
        statSpan(spannableStringBuilder, "MEM ", str, -11649);
        try {
            Intent intentRegisterReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (intentRegisterReceiver != null) {
                int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
                int intExtra2 = intentRegisterReceiver.getIntExtra("temperature", -1);
                String str2 = intExtra >= 0 ? intExtra + "%" : "--";
                if (intExtra2 > 0) {
                    str2 = str2 + " " + (intExtra2 / 10) + "C";
                }
                statSep(spannableStringBuilder);
                statSpan(spannableStringBuilder, "BAT ", str2, -1510913);
            }
        } catch (Throwable th2) {
        }
        try {
            StatFs statFs = new StatFs("/data");
            statSep(spannableStringBuilder);
            statSpan(spannableStringBuilder, "FREE ", (statFs.getAvailableBytes() / 1073741824) + "G", -5716772);
        } catch (Throwable th3) {
        }
        return spannableStringBuilder;
    }

    private static String readStats(Context context) {
        String str;
        String str2;
        String str3 = "";
        int iCpuPercent = cpuPercent();
        String strGpuText = gpuText();
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            int i = memoryInfo.totalMem > 0 ? (int) (((memoryInfo.totalMem - memoryInfo.availMem) * 100) / memoryInfo.totalMem) : -1;
            str = "MEM " + (i < 0 ? "--" : i + "%");
        } catch (Throwable th) {
            str = "MEM --";
        }
        try {
            Intent intentRegisterReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (intentRegisterReceiver == null) {
                str2 = "";
            } else {
                int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
                int intExtra2 = intentRegisterReceiver.getIntExtra("temperature", -1);
                str2 = "BAT " + (intExtra < 0 ? "--" : intExtra + "%");
                if (intExtra2 > 0) {
                    try {
                        str2 = str2 + " " + (intExtra2 / 10) + "C";
                    } catch (Throwable th2) {
                    }
                }
            }
        } catch (Throwable th3) {
            str2 = "";
        }
        try {
            str3 = "FREE " + (new StatFs("/data").getAvailableBytes() / 1073741824) + "G";
        } catch (Throwable th4) {
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CPU ").append(iCpuPercent >= 0 ? iCpuPercent + "%" : "--");
        sb.append("  ·  ").append(strGpuText);
        sb.append("  ·  ").append(str);
        if (!str2.isEmpty()) {
            sb.append("  ·  ").append(str2);
        }
        if (!str3.isEmpty()) {
            sb.append("  ·  ").append(str3);
        }
        return sb.toString();
    }

    private static int cpuPercent() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/stat"));
            String line = bufferedReader.readLine();
            bufferedReader.close();
            if (line == null) {
                return -1;
            }
            String[] strArrSplit = line.trim().split("\\s+");
            long j = Long.parseLong(strArrSplit[4]) + Long.parseLong(strArrSplit[5]);
            long j2 = 0;
            for (int i = 1; i < strArrSplit.length; i++) {
                j2 += Long.parseLong(strArrSplit[i]);
            }
            if (sLastCpu == null) {
                sLastCpu = new long[]{j2, j};
                return 0;
            }
            long j3 = j2 - sLastCpu[0];
            long j4 = j - sLastCpu[1];
            sLastCpu = new long[]{j2, j};
            if (j3 <= 0) {
                return 0;
            }
            return (int) Math.max(0L, Math.min(100L, ((j3 - j4) * 100) / j3));
        } catch (Throwable th) {
            return -1;
        }
    }

    private static String gpuText() {
        String[] strArr = {"/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", "/sys/class/kgsl/kgsl-3d0/gpubusy", "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", "/sys/kernel/gpu/gpu_busy"};
        for (int i = 0; i < 4; i++) {
            String str = strArr[i];
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(str));
                String line = bufferedReader.readLine();
                bufferedReader.close();
                if (line != null && !line.trim().isEmpty()) {
                    String strTrim = line.trim();
                    if (strTrim.contains("%")) {
                        return "GPU " + ((int) Double.parseDouble(strTrim.replace("%", "").trim())) + "%";
                    }
                    if (str.endsWith("gpubusy")) {
                        String[] strArrSplit = strTrim.split("\\s+");
                        if (strArrSplit.length >= 2) {
                            long j = Long.parseLong(strArrSplit[0]);
                            long j2 = Long.parseLong(strArrSplit[1]);
                            return j2 > 0 ? "GPU " + ((j * 100) / j2) + "%" : "GPU 0%";
                        }
                    }
                    if (str.endsWith("cur_freq")) {
                        return "GPU " + (Long.parseLong(strTrim) / 1000000) + "MHz";
                    }
                    if (str.endsWith("gpu_busy")) {
                        return "GPU " + strTrim + "%";
                    }
                }
            } catch (Throwable th) {
            }
        }
        return "GPU --";
    }

    private final class MediaObserver extends ContentObserver {
        MediaObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            onChange(z);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            if (StarlightProvider.this.workerHandler != null) {
                StarlightProvider.this.workerHandler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.MediaObserver.1
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            StarlightProvider.this.scanRecent();
                        } catch (Throwable th) {
                            Log.w(StarlightProvider.TAG, "scan: " + th);
                        }
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanRecent() {
        Context context = getContext();
        if (context != null && prefBool(context, PREF_SMART, true)) {
            long jCurrentTimeMillis = (System.currentTimeMillis() / 1000) - 180;
            scan(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, jCurrentTimeMillis, true);
            scan(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, jCurrentTimeMillis, false);
        }
    }

    private void scan(Uri uri, long j, boolean z) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Cursor cursorQuery = null;
        try {
            cursorQuery = context.getContentResolver().query(uri, new String[]{"_id", "_display_name", "_data", "date_added"}, "date_added>=?", new String[]{String.valueOf(j)}, "date_added DESC");
            if (cursorQuery == null) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            int i = 0;
            while (cursorQuery.moveToNext() && i < 12) {
                i++;
                long j2 = cursorQuery.getLong(0);
                final String string = cursorQuery.getString(1);
                final String string2 = cursorQuery.getString(2);
                if (string != null && string2 != null && !string.contains("_Upscaled") && (string.indexOf("_Starlight") >= 0 || string.startsWith("DSC_") || string.startsWith("IMG_") || string.startsWith("VID_") || string.startsWith("TIMELAPSE_"))) {
                    if (string2.contains("/DCIM/")) {
                        final Uri uriWithAppendedPath = Uri.withAppendedPath(uri, String.valueOf(j2));
                        if (SEEN.add(String.valueOf(uriWithAppendedPath))) {
                            if (z) {
                                this.workerHandler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.18
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        StarlightProvider.this.nameImage(uriWithAppendedPath, string, string2);
                                    }
                                });
                            } else {
                                this.workerHandler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.19
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        StarlightProvider.this.nameVideo(uriWithAppendedPath, string, string2);
                                    }
                                });
                            }
                        }
                    }
                }
            }
            if (cursorQuery == null) {
            }
        } catch (Throwable th) {
            try {
                Log.w(TAG, "scan query: " + th);
            } finally {
                if (0 != 0) {
                    cursorQuery.close();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void nameImage(Uri uri, String str, String str2) {
        try {
            File file = new File(str2);
            long length = -1;
            int i = 0;
            for (int i2 = 0; i2 < 60; i2++) {
                if (file.exists() && file.length() > 0) {
                    i = file.length() == length ? i + 1 : 0;
                    length = file.length();
                    if (i >= 2) {
                        break;
                    }
                }
                Thread.sleep(500L);
            }
            if (file.exists() && file.length() != 0) {
                File fileRename = rename(uri, str, classify(file), file);
                if (fileRename != null && !str.contains("_Starlight")) {
                    maybeUpscale(fileRename);
                }
                return;
            }
            Log.w(TAG, "image not written: " + str);
        } catch (Throwable th) {
            Log.w(TAG, "nameImage: " + th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void nameVideo(Uri uri, String str, String str2) {
        try {
            File file = new File(str2);
            int i = 0;
            int i2 = 0;
            long j = -1;
            while (i < 5400) {
                long length = file.exists() ? file.length() : -1L;
                i2 = (length <= 0 || length != j) ? 0 : i2 + 1;
                if (i2 >= 8) {
                    break;
                }
                Thread.sleep(500L);
                i++;
                j = length;
            }
            File renamed = rename(uri, str, str.contains("TIMELAPSE") ? "Timelapse" : "Video", file);
            if (renamed != null && !str.contains("_Upscaled")
                    && StarlightPolicy.autoVideo(getContext())) {
                final File target = renamed;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean ok = StarlightVideoSR.process(getContext(), target, 2, 24, 0);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), ok ? "Video SR saved" : "Video SR failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, "starlight-auto-video-sr").start();
            }
        } catch (Throwable th) {
            Log.w(TAG, "nameVideo: " + th);
        }
    }

    private static String classify(File file) {
        int[] iArrImageSize = imageSize(file);
        int iMax = iArrImageSize == null ? 0 : Math.max(iArrImageSize[0], iArrImageSize[1]);
        int iMin = iArrImageSize == null ? 0 : Math.min(iArrImageSize[0], iArrImageSize[1]);
        if (iMax >= 8000) {
            return "108MP";
        }
        ExifInfo exif = readExif(file);
        Features featuresAnalyze = analyze(file);
        if (featuresAnalyze != null && featuresAnalyze.monochrome && featuresAnalyze.meanLuma > 10.0f) {
            return "B&W";
        }
        int iDetectFaces = detectFaces(file);
        boolean z = (exif.equiv > 0.0d && exif.equiv <= 18.0d) || (exif.focal > 0.0d && exif.focal < 2.8d);
        boolean z2 = (iMax > 0 && iMax <= 3600 && iMin > 0 && iMin <= 2700) || (exif.focal >= 3.0d && exif.focal < 3.8d && exif.equiv >= 22.0d && exif.equiv <= 30.0d);
        boolean z3 = (z2 || z || (!((exif.focal > 3.8d ? 1 : (exif.focal == 3.8d ? 0 : -1)) >= 0 && (exif.focal > 4.6d ? 1 : (exif.focal == 4.6d ? 0 : -1)) <= 0 && iMin > 2700) && !((exif.distance > 0.05d ? 1 : (exif.distance == 0.05d ? 0 : -1)) > 0 && (exif.distance > 0.25d ? 1 : (exif.distance == 0.25d ? 0 : -1)) < 0))) ? false : true;
        boolean z4 = (exif.iso >= 1600.0d && (exif.brightness < -4.0d || exif.exposure >= 0.08d)) || (exif.exposure >= 0.12d && featuresAnalyze != null && featuresAnalyze.meanLuma < 80.0f) || (exif.brightness < -6.0d && featuresAnalyze != null && featuresAnalyze.meanLuma < 60.0f);
        Log.i(TAG, "classify " + file.getName() + " size=" + iMax + "x" + iMin + " focal=" + exif.focal + " eq=" + exif.equiv + " iso=" + exif.iso + " exp=" + exif.exposure + " dist=" + exif.distance + " faces=" + iDetectFaces + " macro=" + z3 + " wide=" + z + " front=" + z2 + " night=" + z4 + " edge=" + (featuresAnalyze == null ? -1.0f : featuresAnalyze.edge) + " grid=" + (featuresAnalyze == null ? -1.0f : featuresAnalyze.grid) + " sat=" + (featuresAnalyze == null ? -1.0f : featuresAnalyze.meanSat) + " luma=" + (featuresAnalyze != null ? featuresAnalyze.meanLuma : -1.0f) + " laptop=" + (featuresAnalyze != null && featuresAnalyze.laptop) + " pet=" + (featuresAnalyze != null && featuresAnalyze.pet));
        if (z) {
            return "Wide";
        }
        if (z2) {
            if (featuresAnalyze != null && featuresAnalyze.document) {
                return "Document";
            }
            return "Selfie";
        }
        if (featuresAnalyze != null && featuresAnalyze.laptop) {
            return "Laptop";
        }
        if (iDetectFaces > 0) {
            return "Person";
        }
        if (z3) {
            return "Macro";
        }
        if (z4) {
            return "Night";
        }
        if (featuresAnalyze == null || !featuresAnalyze.document) {
            return (featuresAnalyze == null || !featuresAnalyze.pet) ? "Photo" : "Pet";
        }
        return "Document";
    }

    private static final class ExifInfo {
        double brightness;
        double distance;
        double equiv;
        double exposure;
        double focal;
        double iso;

        private ExifInfo() {
        }
    }

    private static ExifInfo readExif(File file) {
        ExifInfo exifInfo = new ExifInfo();
        try {
            ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
            exifInfo.focal = exifInterface.getAttributeDouble("FocalLength", 0.0d);
            exifInfo.equiv = exifInterface.getAttributeDouble("FocalLengthIn35mmFilm", 0.0d);
            exifInfo.iso = exifInterface.getAttributeDouble("ISOSpeedRatings", 0.0d);
            exifInfo.exposure = exifInterface.getAttributeDouble("ExposureTime", 0.0d);
            exifInfo.distance = exifInterface.getAttributeDouble("SubjectDistance", 0.0d);
            exifInfo.brightness = exifInterface.getAttributeDouble("BrightnessValue", 0.0d);
        } catch (Throwable th) {
        }
        return exifInfo;
    }

    private static final class Features {
        float borderEdge;
        float brightRatio;
        float centerEdge;
        boolean document;
        float edge;
        float grid;
        boolean laptop;
        float meanLuma;
        float meanSat;
        boolean monochrome;
        boolean pet;
        float skinRatio;
        float strongRatio;
        float warmRatio;

        private Features() {
        }
    }

    private static Features analyze(File file) {
        Bitmap bitmapDecodeFile;
        float fMin;
        Features features;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (options.outWidth > 0 && options.outHeight > 0) {
                int i = 1;
                while (Math.max(options.outWidth, options.outHeight) / i > 320) {
                    i *= 2;
                }
                options.inJustDecodeBounds = false;
                options.inSampleSize = i;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmapDecodeFile = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                if (bitmapDecodeFile == null) {
                    if (bitmapDecodeFile != null) {
                        bitmapDecodeFile.recycle();
                    }
                    return null;
                }
                try {
                    int width = bitmapDecodeFile.getWidth();
                    int height = bitmapDecodeFile.getHeight();
                    int i2 = width * height;
                    int[] iArr = new int[i2];
                    int[] iArr2 = new int[i2];
                    bitmapDecodeFile.getPixels(iArr, 0, width, 0, 0, width, height);
                    int i3 = 0;
                    long j = 0;
                    long j2 = 0;
                    long j3 = 0;
                    long j4 = 0;
                    long j5 = 0;
                    while (i3 < i2) {
                        try {
                            int i4 = iArr[i3];
                            int i5 = (i4 >> 16) & 255;
                            int[] iArr3 = iArr;
                            int i6 = (i4 >> 8) & 255;
                            int i7 = i4 & 255;
                            int i8 = width;
                            int iMax = Math.max(i5, Math.max(i6, i7));
                            int i9 = i2;
                            int iMin = Math.min(i5, Math.min(i6, i7));
                            long j6 = j5;
                            int i10 = (((i5 * 77) + (i6 * 150)) + (i7 * 29)) >> 8;
                            iArr2[i3] = i10;
                            int i11 = iMax - iMin;
                            j3 += (long) i11;
                            j4 += (long) i10;
                            if (i5 > 95 && i6 > 40 && i7 > 20 && i11 > 15 && Math.abs(i5 - i6) > 15 && i5 > i6 && i5 > i7) {
                                j2++;
                            }
                            if (i5 > i6 + 10 && i5 > i7 + 20) {
                                j++;
                            }
                            j5 = i10 > 180 ? j6 + 1 : j6;
                            i3++;
                            iArr = iArr3;
                            width = i8;
                            i2 = i9;
                        } catch (Throwable th) {
                            th = th;
                            bitmapDecodeFile = bitmapDecodeFile;
                            try {
                                Log.w(TAG, "analyze: " + th);
                            } finally {
                                if (bitmapDecodeFile != null) {
                                    bitmapDecodeFile.recycle();
                                }
                            }
                        }
                    }
                    int i12 = width;
                    Features features2 = new Features();
                    float f = i2;
                    features2.meanSat = j3 / f;
                    features2.meanLuma = j4 / f;
                    features2.skinRatio = j2 / f;
                    features2.warmRatio = j / f;
                    features2.brightRatio = j5 / f;
                    features2.monochrome = features2.meanSat < 6.0f;
                    int i13 = i12 / 4;
                    int i14 = i12 - (i12 / 4);
                    int i15 = height / 4;
                    int i16 = height - (height / 4);
                    int i17 = 1;
                    features = features2;
                    long j7 = 0;
                    long j8 = 0;
                    long j9 = 0;
                    int i18 = 0;
                    int i19 = 0;
                    int i20 = 0;
                    long j10 = 0;
                    long j11 = 0;
                    long j12 = 0;
                    while (i17 < height - 1) {
                        int i21 = 1;
                        while (i21 < i12 - 1) {
                            features = features2;
                            {
                                int i22 = (i17 * i12) + i21;
                                int i23 = i22 - i12;
                                int i24 = i23 - 1;
                                int i25 = i20;
                                int i26 = (-iArr2[i24]) - (iArr2[i23] * 2);
                                int i27 = i23 + 1;
                                int i28 = i22 + i12;
                                int i29 = i28 - 1;
                                int i30 = (i26 - iArr2[i27]) + iArr2[i29] + (iArr2[i28] * 2);
                                int i31 = i28 + 1;
                                int i32 = i30 + iArr2[i31];
                                long j13 = j9;
                                int i33 = (((-iArr2[i24]) - (iArr2[i22 - 1] * 2)) - iArr2[i29]) + iArr2[i27] + (iArr2[i22 + 1] * 2) + iArr2[i31];
                                int iAbs = Math.abs(i32);
                                int iAbs2 = Math.abs(i33);
                                int i34 = (iAbs + iAbs2) >> 3;
                                long j14 = j8;
                                long j15 = i34;
                                long j16 = j7 + j15;
                                j10 += (long) iAbs;
                                j11 += (long) iAbs2;
                                if (i34 > 20) {
                                    j12++;
                                }
                                i18++;
                                if (i21 < i13 || i21 >= i14 || i17 < i15 || i17 >= i16) {
                                    j9 = j13 + j15;
                                    i20 = i25 + 1;
                                    j8 = j14;
                                } else {
                                    j8 = j14 + j15;
                                    i19++;
                                    i20 = i25;
                                    j9 = j13;
                                }
                                i21++;
                                features2 = features;
                                j7 = j16;
                            }
                        }
                        i17++;
                        features2 = features;
                    }
                    Features features3 = features2;
                    float f2 = 0.0f;
                    features3.edge = i18 == 0 ? 0.0f : j7 / i18;
                    features3.centerEdge = i19 == 0 ? 0.0f : j8 / i19;
                    features3.borderEdge = i20 == 0 ? 0.0f : j9 / i20;
                    if (i18 == 0) {
                        fMin = 0.0f;
                    } else {
                        long j17 = j10;
                        long j18 = j11;
                        fMin = Math.min(j17, j18) / Math.max(1.0f, Math.max(j17, j18));
                    }
                    features3.grid = fMin;
                    if (i18 != 0) {
                        f2 = j12 / i18;
                    }
                    features3.strongRatio = f2;
                    features3.document = (features3.brightRatio > 0.55f && features3.meanSat < 20.0f && features3.edge > 6.0f) || (features3.brightRatio > 0.35f && features3.meanLuma > 150.0f && features3.meanSat < 25.0f && features3.edge > 8.0f);
                    features3.laptop = !features3.document && features3.meanLuma > 30.0f && features3.meanLuma < 190.0f && features3.meanSat < 42.0f && features3.edge > 6.0f && features3.grid > 0.68f && features3.strongRatio > 0.04f && features3.borderEdge > 4.0f && features3.centerEdge < features3.borderEdge * 1.25f;
                    features3.pet = !features3.document && !features3.laptop && features3.meanSat > 18.0f && features3.meanLuma > 30.0f && features3.meanLuma < 200.0f && features3.warmRatio > 0.08f && features3.centerEdge > 5.0f && features3.centerEdge > features3.borderEdge * 1.3f;
                    if (bitmapDecodeFile != null) {
                        bitmapDecodeFile.recycle();
                    }
                    return features3;
                } catch (Throwable th2) {
                    Log.w(TAG, "features: " + th2);
                }
            }
            return null;
        } catch (Throwable th3) {
            Log.w(TAG, "features: " + th3);
            bitmapDecodeFile = null;
        }
        return null;
    }

    private static boolean isMonochrome(File file) {
        boolean z = false;
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int i = 1;
            while (Math.max(options.outWidth, options.outHeight) / i > 256) {
                i *= 2;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = i;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmapDecodeFile != null) {
                long j = 0;
                long jMax = 0;
                for (int i2 = 0; i2 < bitmapDecodeFile.getHeight(); i2 += 2) {
                    for (int i3 = 0; i3 < bitmapDecodeFile.getWidth(); i3 += 2) {
                        int pixel = bitmapDecodeFile.getPixel(i3, i2);
                        int i4 = (pixel >> 16) & 255;
                        int i5 = (pixel >> 8) & 255;
                        int i6 = pixel & 255;
                        jMax += (long) (Math.max(i4, Math.max(i5, i6)) - Math.min(i4, Math.min(i5, i6)));
                        j++;
                    }
                }
                if (j > 0 && jMax / j < 6.0d) {
                    z = true;
                }
                if (bitmapDecodeFile != null) {
                    bitmapDecodeFile.recycle();
                }
                return z;
            }
            if (bitmapDecodeFile != null) {
                bitmapDecodeFile.recycle();
            }
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                bitmap.recycle();
            }
            return false;
        }
    }

    private static int[] imageSize(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return new int[]{options.outWidth, options.outHeight};
        } catch (Throwable th) {
            return null;
        }
    }

    private static int detectFaces(File file) {
        Bitmap bitmapDecodeFile = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int attributeInt = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int i = 1;
            while (Math.max(options.outWidth, options.outHeight) / i > 1024) {
                i *= 2;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = i;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmapDecodeFile = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmapDecodeFile == null) {
                if (bitmapDecodeFile != null) {
                    bitmapDecodeFile.recycle();
                }
                return 0;
            }
            try {
                attributeInt = new ExifInterface(file.getAbsolutePath()).getAttributeInt("Orientation", 1);
            } catch (Throwable th) {
            }
            Matrix matrix = new Matrix();
            if (attributeInt == 6) {
                matrix.postRotate(90.0f);
            } else if (attributeInt == 3) {
                matrix.postRotate(180.0f);
            } else if (attributeInt == 8) {
                matrix.postRotate(270.0f);
            }
            if (!matrix.isIdentity()) {
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmapDecodeFile, 0, 0, bitmapDecodeFile.getWidth(), bitmapDecodeFile.getHeight(), matrix, false);
                if (bitmapCreateBitmap != bitmapDecodeFile) {
                    bitmapDecodeFile.recycle();
                }
                bitmapDecodeFile = bitmapCreateBitmap;
            }
            FaceDetector.Face[] faceArr = new FaceDetector.Face[8];
            int iFindFaces = new FaceDetector(bitmapDecodeFile.getWidth(), bitmapDecodeFile.getHeight(), 8).findFaces(bitmapDecodeFile, faceArr);
            float fMin = Math.min(bitmapDecodeFile.getWidth(), bitmapDecodeFile.getHeight()) * 0.035f;
            int i2 = 0;
            for (int i3 = 0; i3 < iFindFaces; i3++) {
                FaceDetector.Face face = faceArr[i3];
                if (face != null) {
                    float fConfidence = face.confidence();
                    float fEyesDistance = face.eyesDistance();
                    Log.i(TAG, "face cand " + i3 + " conf=" + fConfidence + " eyes=" + fEyesDistance + " minEyes=" + fMin);
                    if (fConfidence >= 0.4f && fEyesDistance >= fMin) {
                        i2++;
                    }
                }
            }
            if (bitmapDecodeFile != null) {
                bitmapDecodeFile.recycle();
            }
            return i2;
        } catch (Throwable th2) {
            try {
                Log.w(TAG, "face: " + th2);
                return 0;
            } finally {
                if (bitmapDecodeFile != null) {
                    bitmapDecodeFile.recycle();
                }
            }
        }
    }

    private File rename(Uri uri, String str, String str2, File file) {
        String string;
        try {
            Context context = getContext();
            if (context == null) {
                return null;
            }
            int iLastIndexOf = str.lastIndexOf(46);
            String strSubstring = iLastIndexOf >= 0 ? str.substring(iLastIndexOf) : "";
            Matcher matcher = TS.matcher(str);
            String strGroup = matcher.find() ? matcher.group(1) : null;
            if (strGroup == null) {
                strGroup = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            }
            String str3 = ("B&W".equals(str2) ? "B&W" : str2.replaceAll("[^A-Za-z0-9]", "")) + "_" + strGroup + "_Starlight" + strSubstring;
            if (str3.equals(str)) {
                return file;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str3);
            int iUpdate = context.getContentResolver().update(uri, contentValues, null, null);
            Log.i(TAG, "rename " + str + " -> " + str3 + " rows=" + iUpdate);
            File parentFile = file != null ? file.getParentFile() : null;
            File file2 = parentFile != null ? new File(parentFile, str3) : null;
            if (file2 != null && file2.exists() && file2.length() > 0) {
                return file2;
            }
            if (iUpdate <= 0) {
                Cursor cursorQuery = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursorQuery == null) {
                    string = null;
                } else {
                    string = cursorQuery.moveToFirst() ? cursorQuery.getString(0) : null;
                    cursorQuery.close();
                }
                File file3 = string != null ? new File(string) : file;
                if (file3 != null) {
                    if (file2 == null) {
                        file2 = new File(file3.getParentFile(), str3);
                    }
                    if (file3.renameTo(file2)) {
                        MediaScannerConnection.scanFile(context, new String[]{file2.getAbsolutePath()}, null, null);
                        try {
                            context.getContentResolver().delete(uri, null, null);
                        } catch (Throwable th) {
                        }
                        Log.i(TAG, "file rename fallback " + file2);
                        return file2;
                    }
                }
            }
            if (file != null && file.exists() && file.length() > 0) {
                return file;
            }
            if (file2 == null || !file2.exists()) {
                return null;
            }
            return file2;
        } catch (Throwable th2) {
            Log.w(TAG, "rename: " + th2);
            return null;
        }
    }

    private void maybeUpscale(File file) {
        maybeUpscale(file, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeUpscale(final File file, boolean z) {
        String absolutePath;
        try {
            Context context = getContext();
            if (context != null && file != null && file.exists() && file.length() != 0) {
                if (z || prefBool(context, PREF_UPSCALE, false)) {
                    String name = file.getName();
                    if (!name.contains("_Upscaled") && !name.startsWith("Upscaled_")) {
                        File parentFile = file.getParentFile();
                        if (!z && parentFile != null) {
                            File file2 = new File(parentFile, "Upscaled_" + upscaleStem(name) + ".jpg");
                            if (file2.exists() && file2.length() > 0) {
                                Log.i(TAG, "upscale exists, skip " + name);
                                return;
                            }
                        }
                        try {
                            absolutePath = file.getCanonicalPath();
                        } catch (Throwable th) {
                            absolutePath = file.getAbsolutePath();
                        }
                        synchronized (UPSCALED) {
                            if (!z) {
                                if (!UPSCALED.add(absolutePath)) {
                                    return;
                                }
                            }
                            UPSCALED.add(absolutePath);
                            final int iMax = StarlightPolicy.photoScale(context,
                                    prefInt(context, PREF_UPSCALE_SCALE, 2));
                            Log.i(TAG, "upscale queued " + name + " scale=" + iMax + " force=" + z);
                            this.upscaleHandler.post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.20
                                @Override // java.lang.Runnable
                                public void run() {
                                    try {
                                        StarlightProvider.this.upscalePhoto(file, iMax,
                                                !z && StarlightPolicy.deleteOriginal(context));
                                    } catch (Throwable th2) {
                                        Log.w(StarlightProvider.TAG, "upscale: " + th2);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        } catch (Throwable th2) {
            Log.w(TAG, "upscale queue: " + th2);
        }
    }

    private static boolean isMaxPhotoQuality(Context context) {
        try {
            return context != null && "max".equals(prefString(context, PREF_PHOTO_QUALITY, "default"));
        } catch (Throwable th) {
            return false;
        }
    }

    private static String upscaleStem(String str) {
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf > 0) {
            str = str.substring(0, iLastIndexOf);
        }
        String strReplace = str.replace("_Starlight", "").replace("_Upscaled", "");
        if (strReplace.startsWith("DSC_") || strReplace.startsWith("IMG_") || strReplace.startsWith("VID_") || strReplace.startsWith("TIMELAPSE_")) {
            return strReplace.substring(strReplace.indexOf(95) + 1);
        }
        return strReplace;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void upscalePhoto(File file, int i) {
        upscalePhoto(file, i, false);
    }

    /**
     * @param deleteOriginal true for auto-upscale: the camera already saved the
     *                       original, so publish only the upscaled image.
     *                       Hold-to-process passes false and keeps the source.
     */
    public boolean upscalePhoto(File file, int i, boolean deleteOriginal) {
        long j = StarlightPolicy.photoCap(getContext(), isMaxPhotoQuality(getContext()));
        j = memorySafePhotoCap(getContext(), file, i, j);
        sPhotoProcessing = true;
        sPhotoProgress = 5;
        StarlightLog.job("photo start " + file.getName() + " scale=" + i
                + " deleteOriginal=" + deleteOriginal);
        StarlightProgressNotifier.update(getContext(), "Starlight Photo SR", 5);
        StarlightJobService.start(getContext(), "Upscaling photo");
        try {
        for (int i2 = 0; i2 < 3; i2++) {
            try {
                if (upscaleOnce(file, i, j)) {
                    if (deleteOriginal) {
                        removeOriginalAfterUpscale(file);
                    }
                    final Context toastContext = getContext();
                    if (toastContext != null) {
                        final int scale = i;
                        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.starlight.camera.StarlightProvider.21
                            @Override // java.lang.Runnable
                            public void run() {
                                Toast.makeText(toastContext, "Starlight SR " + scale + "x saved", 0).show();
                            }
                        });
                    }
                    sPhotoProcessing = false;
                    sPhotoProgress = 100;
                    StarlightLog.job("photo done " + file.getName());
                    return true;
                }
            } catch (OutOfMemoryError e) {
                j /= 2;
                System.gc();
                Log.w(TAG, "upscale OOM, retry cap=" + j);
                StarlightLog.e("photo OOM cap=" + j, e);
            } catch (Throwable th) {
                Log.w(TAG, "upscale failed: " + th);
                StarlightLog.e("photo failed " + file.getName(), th);
                sPhotoProcessing = false;
                return false;
            }
        }
        sPhotoProcessing = false;
        Log.w(TAG, "upscale skipped after retries: " + file.getName());
        StarlightLog.e("photo skipped after retries " + file.getName(), null);
        return false;
        } finally {
            StarlightJobService.stop(getContext());
        }
    }

    /** Keep the whole-image GL readback inside a conservative memory budget. */
    private static long memorySafePhotoCap(Context context, File file, int scale, long requested) {
        if (context == null || file == null) {
            return requested;
        }
        try {
            int[] size = imageSize(file);
            if (size == null || size[0] <= 0 || size[1] <= 0) {
                return requested;
            }
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return requested;
            }
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            long targetPixels = (long) size[0] * size[1] * scale * scale;
            long estimatedBytes = targetPixels * 12L; // source + output + readback
            long budget = Math.max(128L * 1024L * 1024L, info.availMem / 3L);
            if (am.isLowRamDevice()) {
                budget = Math.min(budget, 160L * 1024L * 1024L);
            }
            if (estimatedBytes > budget) {
                long capped = Math.max(4_000_000L, budget / 12L);
                if (capped < requested) {
                    StarlightLog.job("photo memory cap " + requested + " -> " + capped
                            + " avail=" + info.availMem + " lowRam=" + am.isLowRamDevice());
                    return capped;
                }
            }
        } catch (Throwable th) {
            StarlightLog.e("memorySafePhotoCap", th);
        }
        return requested;
    }

    /** Auto-upscale should leave one gallery item: the upscaled output. */
    private void removeOriginalAfterUpscale(File original) {
        try {
            File parent = original.getParentFile();
            File expected = parent == null ? null
                    : new File(parent, "Upscaled_" + upscaleStem(original.getName()) + ".jpg");
            if (expected == null || !expected.exists() || expected.length() <= 0) {
                StarlightLog.e("auto output missing, keeping original " + original.getName(), null);
                return;
            }
            Context context = getContext();
            boolean deleted = false;
            if (context != null) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{"_id"},
                            MediaStore.MediaColumns.DATA + "=?",
                            new String[]{original.getAbsolutePath()}, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        Uri row = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
                        deleted = context.getContentResolver().delete(row, null, null) > 0;
                    }
                } catch (Throwable th) {
                    StarlightLog.e("delete original row " + original.getName(), th);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (!deleted && original.exists()) {
                deleted = original.delete();
            }
            StarlightLog.job("auto removed original=" + deleted + " " + original.getName());
        } catch (Throwable th) {
            StarlightLog.e("removeOriginalAfterUpscale", th);
        }
    }

    /* JADX WARN: Code duplicated, block: B:127:0x02da A[Catch: all -> 0x0287, TRY_ENTER, TRY_LEAVE, TryCatch #1 {all -> 0x0287, blocks: (B:87:0x01f6, B:108:0x0260, B:127:0x02da, B:134:0x02eb, B:85:0x01dd, B:81:0x01d6), top: B:172:0x018a, inners: #13 }] */
    /* JADX WARN: Code duplicated, block: B:129:0x02e0  */
    /* JADX WARN: Code duplicated, block: B:132:0x02e5 A[Catch: all -> 0x037f, TRY_ENTER, TRY_LEAVE, TryCatch #17 {all -> 0x037f, blocks: (B:105:0x0233, B:106:0x025a, B:112:0x0289, B:132:0x02e5, B:139:0x030d), top: B:200:0x0233 }] */
    /* JADX WARN: Code duplicated, block: B:134:0x02eb A[Catch: all -> 0x0287, TRY_ENTER, TRY_LEAVE, TryCatch #1 {all -> 0x0287, blocks: (B:87:0x01f6, B:108:0x0260, B:127:0x02da, B:134:0x02eb, B:85:0x01dd, B:81:0x01d6), top: B:172:0x018a, inners: #13 }] */
    /* JADX WARN: Code duplicated, block: B:136:0x0308  */
    /* JADX WARN: Code duplicated, block: B:139:0x030d A[Catch: all -> 0x037f, TRY_ENTER, TRY_LEAVE, TryCatch #17 {all -> 0x037f, blocks: (B:105:0x0233, B:106:0x025a, B:112:0x0289, B:132:0x02e5, B:139:0x030d), top: B:200:0x0233 }] */
    /* JADX WARN: Code duplicated, block: B:143:0x036d  */
    /* JADX WARN: Code duplicated, block: B:164:0x038e  */
    /* JADX WARN: Code duplicated, block: B:166:0x0393  */
    /* JADX WARN: Code duplicated, block: B:205:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:206:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:207:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v48, types: [java.lang.StringBuilder] */
    /* JADX WARN: Type inference failed for: r10v12 */
    /* JADX WARN: Type inference failed for: r10v2 */
    /* JADX WARN: Type inference failed for: r10v3, types: [android.graphics.Bitmap] */
    /* JADX WARN: Type inference failed for: r10v4 */
    /* JADX WARN: Type inference failed for: r10v6 */
    /* JADX WARN: Type inference failed for: r10v8 */
    /* JADX WARN: Type inference failed for: r4v0, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r4v3 */
    /* JADX WARN: Type inference failed for: r4v4 */
    /* JADX WARN: Type inference failed for: r4v5 */
    /* JADX WARN: Type inference failed for: r4v6, types: [java.io.File, java.lang.Object] */
    /* JADX WARN: Type inference failed for: r4v8, types: [java.io.File] */
    private boolean upscaleOnce(File file, int i, long j) throws Throwable {
        Context context = getContext();
        if (context == null) {
            return true;
        }
        boolean max = isMaxPhotoQuality(context);
        boolean potato = StarlightPolicy.potato(context);
        int quality = StarlightPolicy.photoQuality(context, max);
        long cap = StarlightPolicy.photoCap(context, max);
        boolean ok;
        if (potato) {
            ok = StarlightSR.process(context, file, StarlightPolicy.photoScale(context, i),
                    quality, cap, StarlightPolicy.photoSharpen(context));
        } else if (max) {
            ok = StarlightSR.process(context, file, i, quality, cap,
                    StarlightPolicy.photoSharpen(context));
        } else {
            ok = StarlightSRDefault.process(context, file, i, quality, cap);
        }
        if (ok) {
            Log.i(TAG, "upscale done " + file.getName() + " scale=" + i);
        }
        return ok;
    }

    private static Bitmap bicubicUpscale(Context context, Bitmap bitmap, int i, int i2) throws Throwable {
        int i3 = i;
        int i4 = i2;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] iArr = new int[i3];
        int[] iArr2 = new int[i3];
        int[] iArr3 = new int[i3];
        int[] iArr4 = new int[i3];
        int[] iArr5 = new int[i3];
        int[] iArr6 = new int[i3];
        int[] iArr7 = new int[i3];
        int[] iArr8 = new int[i3];
        fillCubic(width, i, iArr, iArr2, iArr3, iArr4, iArr5, iArr6, iArr7, iArr8);
        int[] iArr9 = new int[i4];
        int[] iArr10 = new int[i4];
        int[] iArr11 = new int[i4];
        int[] iArr12 = new int[i4];
        int[] iArr13 = new int[i4];
        int[] iArr14 = new int[i4];
        int[] iArr15 = new int[i4];
        int[] iArr16 = new int[i4];
        fillCubic(height, i2, iArr9, iArr10, iArr11, iArr12, iArr13, iArr14, iArr15, iArr16);
        Bitmap bitmap2 = null;
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, height, Bitmap.Config.ARGB_8888);
            try {
                int[] iArr17 = new int[width];
                int[] iArr18 = new int[i3];
                int i5 = 0;
                while (i5 < height) {
                    int i6 = i5;
                    int[] iArr19 = iArr18;
                    int[] iArr20 = iArr17;
                    bitmap.getPixels(iArr17, 0, width, 0, i5, width, 1);
                    int i7 = 0;
                    while (i7 < i3) {
                        int i8 = iArr20[iArr[i7]];
                        int i9 = iArr20[iArr2[i7]];
                        int i10 = iArr20[iArr3[i7]];
                        int i11 = iArr20[iArr4[i7]];
                        int i12 = iArr5[i7];
                        int i13 = iArr6[i7];
                        int i14 = iArr7[i7];
                        int i15 = iArr8[i7];
                        int i16 = width;
                        int i17 = height;
                        int[] iArr21 = iArr;
                        int i18 = (((((((i8 >> 16) & 255) * i12) + (((i9 >> 16) & 255) * i13)) + (((i10 >> 16) & 255) * i14)) + (((i11 >> 16) & 255) * i15)) + 16384) >> 15;
                        int[] iArr22 = iArr2;
                        int i19 = (((((((i8 >> 8) & 255) * i12) + (((i9 >> 8) & 255) * i13)) + (((i10 >> 8) & 255) * i14)) + (((i11 >> 8) & 255) * i15)) + 16384) >> 15;
                        int i20 = ((((((i8 & 255) * i12) + ((i9 & 255) * i13)) + ((i10 & 255) * i14)) + ((i11 & 255) * i15)) + 16384) >> 15;
                        if (i18 < 0) {
                            i18 = 0;
                        } else if (i18 > 255) {
                            i18 = 255;
                        }
                        if (i19 < 0) {
                            i19 = 0;
                        } else if (i19 > 255) {
                            i19 = 255;
                        }
                        if (i20 < 0) {
                            i20 = 0;
                        } else if (i20 > 255) {
                            i20 = 255;
                        }
                        iArr19[i7] = (i18 << 16) | (-16777216) | (i19 << 8) | i20;
                        i7++;
                        width = i16;
                        iArr = iArr21;
                        height = i17;
                        iArr2 = iArr22;
                    }
                    bitmapCreateBitmap.setPixels(iArr19, 0, i, 0, i6, i, 1);
                    i5 = i6 + 1;
                    width = width;
                    iArr = iArr;
                    iArr18 = iArr19;
                    iArr17 = iArr20;
                    height = height;
                    iArr2 = iArr2;
                }
                char c = 255;
                long jCurrentTimeMillis2 = System.currentTimeMillis();
                Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(i3, i4, Bitmap.Config.ARGB_8888);
                int[] iArr23 = new int[i3];
                int[] iArr24 = new int[i3];
                int[] iArr25 = new int[i3];
                int[] iArr26 = new int[i3];
                int[] iArr27 = new int[i3];
                int i21 = 0;
                while (i21 < i4) {
                    int i22 = i21;
                    int[] iArr28 = iArr27;
                    int[] iArr29 = iArr26;
                    int[] iArr30 = iArr25;
                    int[] iArr31 = iArr24;
                    int[] iArr32 = iArr23;
                    bitmapCreateBitmap.getPixels(iArr23, 0, i, 0, iArr9[i21], i, 1);
                    bitmapCreateBitmap.getPixels(iArr31, 0, i, 0, iArr10[i22], i, 1);
                    bitmapCreateBitmap.getPixels(iArr30, 0, i, 0, iArr11[i22], i, 1);
                    bitmapCreateBitmap.getPixels(iArr29, 0, i, 0, iArr12[i22], i, 1);
                    int i23 = iArr13[i22];
                    int i24 = iArr14[i22];
                    int i25 = iArr15[i22];
                    int i26 = iArr16[i22];
                    int i27 = 0;
                    while (i27 < i3) {
                        int i28 = iArr32[i27];
                        int i29 = iArr31[i27];
                        int i30 = iArr30[i27];
                        int i31 = iArr29[i27];
                        long j = jCurrentTimeMillis;
                        int i32 = (((((((i28 >> 16) & 255) * i23) + (((i29 >> 16) & 255) * i24)) + (((i30 >> 16) & 255) * i25)) + (((i31 >> 16) & 255) * i26)) + 16384) >> 15;
                        int i33 = (((((((i28 >> 8) & 255) * i23) + (((i29 >> 8) & 255) * i24)) + (((i30 >> 8) & 255) * i25)) + (((i31 >> 8) & 255) * i26)) + 16384) >> 15;
                        int i34 = ((((((i28 & 255) * i23) + ((i29 & 255) * i24)) + ((i30 & 255) * i25)) + ((i31 & 255) * i26)) + 16384) >> 15;
                        if (i32 < 0) {
                            i32 = 0;
                        } else if (i32 > 255) {
                            i32 = 255;
                        }
                        if (i33 < 0) {
                            i33 = 0;
                        } else if (i33 > 255) {
                            i33 = 255;
                        }
                        if (i34 < 0) {
                            i34 = 0;
                        } else if (i34 > 255) {
                            i34 = 255;
                        }
                        iArr28[i27] = i34 | (i32 << 16) | (-16777216) | (i33 << 8);
                        i27++;
                        c = 255;
                        jCurrentTimeMillis = j;
                        i3 = i;
                    }
                    bitmapCreateBitmap2.setPixels(iArr28, 0, i, 0, i22, i, 1);
                    i21 = i22 + 1;
                    i4 = i2;
                    c = c;
                    iArr24 = iArr31;
                    iArr27 = iArr28;
                    iArr25 = iArr30;
                    iArr26 = iArr29;
                    iArr23 = iArr32;
                    jCurrentTimeMillis = jCurrentTimeMillis;
                    i3 = i;
                }
                Log.i(TAG, "bicubic stages h=" + (jCurrentTimeMillis2 - jCurrentTimeMillis) + " v=" + (System.currentTimeMillis() - jCurrentTimeMillis2) + "ms " + i + "x" + i2);
                if (bitmapCreateBitmap != null) {
                    bitmapCreateBitmap.recycle();
                }
                return bitmapCreateBitmap2;
            } catch (Throwable th) {
                bitmap2 = bitmapCreateBitmap;
                if (bitmap2 != null) {
                    bitmap2.recycle();
                }
                throw th;
            }
        } catch (Throwable th2) {
            throw th2;
        }
    }

    private static void fillCubic(int i, int i2, int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int[] iArr5, int[] iArr6, int[] iArr7, int[] iArr8) {
        float f = i / i2;
        for (int i3 = 0; i3 < i2; i3++) {
            float f2 = ((i3 + 0.5f) * f) - 0.5f;
            int iFloor = (int) Math.floor(f2);
            float f3 = f2 - iFloor;
            float f4 = f3 * f3;
            float f5 = f4 * f3;
            float f6 = f5 * 0.5f;
            float f7 = ((f3 * (-0.5f)) + f4) - f6;
            float f8 = f5 * 1.5f;
            float f9 = (1.0f - (2.5f * f4)) + f8;
            float f10 = ((f3 * 0.5f) + (2.0f * f4)) - f8;
            int i4 = i - 1;
            iArr[i3] = clamp(iFloor - 1, 0, i4);
            iArr2[i3] = clamp(iFloor, 0, i4);
            iArr3[i3] = clamp(iFloor + 1, 0, i4);
            iArr4[i3] = clamp(iFloor + 2, 0, i4);
            iArr5[i3] = Math.round(f7 * 32768.0f);
            iArr6[i3] = Math.round(f9 * 32768.0f);
            iArr7[i3] = Math.round(f10 * 32768.0f);
            iArr8[i3] = Math.round(((f4 * (-0.5f)) + f6) * 32768.0f);
        }
    }

    private static int clamp(int i, int i2, int i3) {
        if (i < i2) {
            return i2;
        }
        return i > i3 ? i3 : i;
    }

    private static float sharpenStrength(File file) {
        ExifInfo exif = readExif(file);
        if (exif.iso >= 1600.0d || exif.exposure >= 0.15d) {
            return 0.12f;
        }
        if (exif.iso >= 800.0d || exif.exposure >= 0.08d) {
            return 0.18f;
        }
        return 0.26f;
    }

    private static final class RsSharpen {
        private RsSharpen() {
        }

        static Bitmap apply(Context context, Bitmap bitmap, float f) throws Throwable {
            RenderScript renderScriptCreate;
            try {
                renderScriptCreate = RenderScript.create(context);
                try {
                    Allocation allocationCreateFromBitmap = Allocation.createFromBitmap(renderScriptCreate, bitmap);
                    Allocation allocationCreateTyped = Allocation.createTyped(renderScriptCreate, allocationCreateFromBitmap.getType());
                    ScriptIntrinsicConvolve3x3 scriptIntrinsicConvolve3x3Create = ScriptIntrinsicConvolve3x3.create(renderScriptCreate, Element.U8_4(renderScriptCreate));
                    float f2 = -f;
                    scriptIntrinsicConvolve3x3Create.setCoefficients(new float[]{0.0f, f2, 0.0f, f2, (f * 4.0f) + 1.0f, f2, 0.0f, f2, 0.0f});
                    scriptIntrinsicConvolve3x3Create.setInput(allocationCreateFromBitmap);
                    scriptIntrinsicConvolve3x3Create.forEach(allocationCreateTyped);
                    Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    allocationCreateTyped.copyTo(bitmapCreateBitmap);
                    if (renderScriptCreate != null) {
                        renderScriptCreate.destroy();
                    }
                    return bitmapCreateBitmap;
                } catch (Throwable th) {
                    if (renderScriptCreate != null) {
                        renderScriptCreate.destroy();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                Log.w(TAG, "sharpen: " + th2);
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean prefBool(Context context, String str, boolean z) {
        Object obj = PreferenceManager.getDefaultSharedPreferences(context).getAll().get(str);
        if (obj == null) {
            return z;
        }
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        }
        String strValueOf = String.valueOf(obj);
        return "1".equals(strValueOf) || "true".equalsIgnoreCase(strValueOf);
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }
}
