package com.starlight.camera;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Starlight Gemini two-pass video pipeline.
 *
 * Pass 1: BT.709 chroma bilateral + motion-gated temporal accumulation.
 *         Output is RGBA8 with RGB color and alpha = BT.709 luma.
 * Pass 2: directional edge de-blur, dual-band frequency separation,
 *         luma-only gain, S-curve, IGN dithering. Renders directly to encoder.
 *
 * This build is 1:1 in time (one output frame per source frame) so the
 * temporal history never invents interpolated frames or ghosts.
 */
final class StarlightVideoGL {
    private static final String TAG = "StarlightVideoGL";
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_CONTEXT_PRIORITY_LEVEL_IMG = 0x3100;
    private static final int EGL_CONTEXT_PRIORITY_LOW_IMG = 0x3103;
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private static final float[] QUAD = {
            -1.0f, -1.0f, 0.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f, 1.0f,
             1.0f,  1.0f, 1.0f, 1.0f
    };

    // Gemini coordinate rule: OES samples use the SurfaceTexture matrix,
    // FBO samples use raw texture coordinates. Never mix them.
    private static final String VERTEX_OES =
            "attribute vec4 aPos;\n" +
            "attribute vec4 aTex;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "varying vec2 vOesCoord;\n" +
            "varying vec2 vFboCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPos;\n" +
            "  vOesCoord = (uSTMatrix * aTex).xy;\n" +
            "  vFboCoord = aTex.xy;\n" +
            "}\n";

    private static final String VERTEX_2D =
            "attribute vec4 aPos;\n" +
            "attribute vec4 aTex;\n" +
            "varying vec2 vFboCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPos;\n" +
            "  vFboCoord = aTex.xy;\n" +
            "}\n";

    private static final String FRAG_PASS1 =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision highp float;\n" +
            "varying vec2 vOesCoord;\n" +
            "varying vec2 vFboCoord;\n" +
            "uniform samplerExternalOES uCurrentOES;\n" +
            "uniform sampler2D uHistoryTex;\n" +
            "uniform vec2 uTexSize;\n" +
            "uniform float uHasHistory;\n" +
            "uniform float uTemporalStr;\n" +
            "const vec3 LUMA_709 = vec3(0.2126, 0.7152, 0.0722);\n" +
            "vec3 rgbToYCbCr(vec3 rgb) {\n" +
            "  float y = dot(rgb, LUMA_709);\n" +
            "  float cb = (rgb.b - y) * 0.5389 + 0.5;\n" +
            "  float cr = (rgb.r - y) * 0.6350 + 0.5;\n" +
            "  return vec3(y, cb, cr);\n" +
            "}\n" +
            "vec3 yCbCrToRgb(vec3 ycc) {\n" +
            "  float y = ycc.x;\n" +
            "  float cb = ycc.y - 0.5;\n" +
            "  float cr = ycc.z - 0.5;\n" +
            "  float r = y + 1.5748 * cr;\n" +
            "  float g = y - 0.1873 * cb - 0.4681 * cr;\n" +
            "  float b = y + 1.8556 * cb;\n" +
            "  return clamp(vec3(r, g, b), 0.0, 1.0);\n" +
            "}\n" +
            "void main() {\n" +
            "  vec2 invSize = 1.0 / uTexSize;\n" +
            "  vec3 cM = texture2D(uCurrentOES, vOesCoord).rgb;\n" +
            "  vec3 cN = texture2D(uCurrentOES, vOesCoord + vec2(0.0, invSize.y)).rgb;\n" +
            "  vec3 cS = texture2D(uCurrentOES, vOesCoord + vec2(0.0, -invSize.y)).rgb;\n" +
            "  vec3 cE = texture2D(uCurrentOES, vOesCoord + vec2(invSize.x, 0.0)).rgb;\n" +
            "  vec3 cW = texture2D(uCurrentOES, vOesCoord + vec2(-invSize.x, 0.0)).rgb;\n" +
            "  vec3 yccM = rgbToYCbCr(cM);\n" +
            "  vec3 yccN = rgbToYCbCr(cN);\n" +
            "  vec3 yccS = rgbToYCbCr(cS);\n" +
            "  vec3 yccE = rgbToYCbCr(cE);\n" +
            "  vec3 yccW = rgbToYCbCr(cW);\n" +
            "  vec4 lumaDiff = abs(vec4(yccN.x, yccS.x, yccE.x, yccW.x) - yccM.x);\n" +
            "  vec4 chromaW = max(vec4(0.0), 1.0 - lumaDiff * 14.0);\n" +
            "  float wSum = 1.0 + dot(chromaW, vec4(1.0));\n" +
            "  vec2 cleanCbCr = (yccM.yz + yccN.yz * chromaW.x + yccS.yz * chromaW.y + yccE.yz * chromaW.z + yccW.yz * chromaW.w) / wSum;\n" +
            "  vec3 spatialCleanRgb = yCbCrToRgb(vec3(yccM.x, cleanCbCr));\n" +
            "  if (uHasHistory > 0.5) {\n" +
            "    vec4 histSample = texture2D(uHistoryTex, vFboCoord);\n" +
            "    vec3 histRgb = histSample.rgb;\n" +
            "    float histLuma = histSample.a;\n" +
            "    vec3 minBox = min(spatialCleanRgb, min(min(cN, cS), min(cE, cW)));\n" +
            "    vec3 maxBox = max(spatialCleanRgb, max(max(cN, cS), max(cE, cW)));\n" +
            "    float lumaDelta = abs(yccM.x - histLuma);\n" +
            "    float motionConfidence = 1.0 - smoothstep(0.012, 0.060, lumaDelta);\n" +
            "    float boxSlack = 0.018 * motionConfidence;\n" +
            "    vec3 clampedHist = clamp(histRgb, minBox - boxSlack, maxBox + boxSlack);\n" +
            "    float blendFactor = uTemporalStr * motionConfidence;\n" +
            "    spatialCleanRgb = mix(spatialCleanRgb, clampedHist, blendFactor);\n" +
            "  }\n" +
            "  float finalLuma = dot(spatialCleanRgb, LUMA_709);\n" +
            "  gl_FragColor = vec4(spatialCleanRgb, finalLuma);\n" +
            "}\n";

    private static final String FRAG_PASS2 =
            "precision highp float;\n" +
            "varying vec2 vFboCoord;\n" +
            "uniform sampler2D uCleanTex;\n" +
            "uniform vec2 uTexSize;\n" +
            "uniform float uDeBlurStr;\n" +
            "uniform float uFineDetailStr;\n" +
            "uniform float uClarityStr;\n" +
            "uniform float uNoiseFloor;\n" +
            "uniform float uFrameSeed;\n" +
            "float softCore(float d, float threshold) {\n" +
            "  float s = sign(d);\n" +
            "  float a = abs(d);\n" +
            "  return s * max(0.0, a - threshold);\n" +
            "}\n" +
            "void main() {\n" +
            "  vec2 invSize = 1.0 / uTexSize;\n" +
            "  vec4 sM = texture2D(uCleanTex, vFboCoord);\n" +
            "  vec3 rgbM = sM.rgb;\n" +
            "  float yM = sM.a;\n" +
            "  float yN = texture2D(uCleanTex, vFboCoord + vec2(0.0, invSize.y)).a;\n" +
            "  float yS = texture2D(uCleanTex, vFboCoord + vec2(0.0, -invSize.y)).a;\n" +
            "  float yE = texture2D(uCleanTex, vFboCoord + vec2(invSize.x, 0.0)).a;\n" +
            "  float yW = texture2D(uCleanTex, vFboCoord + vec2(-invSize.x, 0.0)).a;\n" +
            "  float yNE = texture2D(uCleanTex, vFboCoord + vec2(invSize.x, invSize.y)).a;\n" +
            "  float yNW = texture2D(uCleanTex, vFboCoord + vec2(-invSize.x, invSize.y)).a;\n" +
            "  float ySE = texture2D(uCleanTex, vFboCoord + vec2(invSize.x, -invSize.y)).a;\n" +
            "  float ySW = texture2D(uCleanTex, vFboCoord + vec2(-invSize.x, -invSize.y)).a;\n" +
            "  float gx = (yNE + 2.0 * yE + ySE) - (yNW + 2.0 * yW + ySW);\n" +
            "  float gy = (yNW + 2.0 * yN + yNE) - (ySW + 2.0 * yS + ySE);\n" +
            "  float gradMag = length(vec2(gx, gy));\n" +
            "  vec2 normDir = (gradMag > 0.0001) ? (vec2(gx, gy) / gradMag) : vec2(1.0, 0.0);\n" +
            "  vec2 tangDir = vec2(-normDir.y, normDir.x);\n" +
            "  float yTangF = texture2D(uCleanTex, vFboCoord + tangDir * invSize * 0.75).a;\n" +
            "  float yTangB = texture2D(uCleanTex, vFboCoord - tangDir * invSize * 0.75).a;\n" +
            "  float yEdgeSmooth = 0.5 * yM + 0.25 * (yTangF + yTangB);\n" +
            "  float yNormF = texture2D(uCleanTex, vFboCoord + normDir * invSize * 1.15).a;\n" +
            "  float yNormB = texture2D(uCleanTex, vFboCoord - normDir * invSize * 1.15).a;\n" +
            "  float minAcross = min(yM, min(yNormF, yNormB));\n" +
            "  float maxAcross = max(yM, max(yNormF, yNormB));\n" +
            "  float midAcross = 0.5 * (yNormF + yNormB);\n" +
            "  float targetPush = (yEdgeSmooth >= midAcross) ? maxAcross : minAcross;\n" +
            "  float edgeStrength = smoothstep(0.03, 0.22, gradMag);\n" +
            "  float yDeBlurred = mix(yEdgeSmooth, targetPush, uDeBlurStr * edgeStrength);\n" +
            "  float blurFine = (4.0 * yM + 2.0 * (yN + yS + yE + yW) + (yNE + yNW + ySE + ySW)) / 16.0;\n" +
            "  float rawFineDetail = yM - blurFine;\n" +
            "  float coredFineDetail = softCore(rawFineDetail, uNoiseFloor) * (1.0 - 0.65 * edgeStrength);\n" +
            "  float yN2 = texture2D(uCleanTex, vFboCoord + vec2(0.0, 2.0 * invSize.y)).a;\n" +
            "  float yS2 = texture2D(uCleanTex, vFboCoord + vec2(0.0, -2.0 * invSize.y)).a;\n" +
            "  float yE2 = texture2D(uCleanTex, vFboCoord + vec2(2.0 * invSize.x, 0.0)).a;\n" +
            "  float yW2 = texture2D(uCleanTex, vFboCoord + vec2(-2.0 * invSize.x, 0.0)).a;\n" +
            "  float blurWide = 0.20 * (blurFine + yN2 + yS2 + yE2 + yW2);\n" +
            "  float midBandClarity = softCore(blurFine - blurWide, uNoiseFloor * 0.5);\n" +
            "  float toneProtection = 4.0 * yM * (1.0 - yM);\n" +
            "  toneProtection = clamp(toneProtection + 0.25, 0.25, 1.0);\n" +
            "  float newLuma = yDeBlurred + (coredFineDetail * uFineDetailStr + midBandClarity * uClarityStr) * toneProtection;\n" +
            "  float localMin = min(yM, min(min(yN, yS), min(yE, yW)));\n" +
            "  float localMax = max(yM, max(max(yN, yS), max(yE, yW)));\n" +
            "  float headroom = (localMax - localMin) * 0.12 + 0.008;\n" +
            "  newLuma = clamp(newLuma, localMin - headroom, localMax + headroom);\n" +
            "  float lumaDelta = newLuma - yM;\n" +
            "  vec3 finalRgb = rgbM + vec3(lumaDelta);\n" +
            "  vec3 sCurve = finalRgb * finalRgb * (3.0 - 2.0 * finalRgb);\n" +
            "  finalRgb = mix(finalRgb, sCurve, 0.035);\n" +
            "  vec2 pixelPos = vFboCoord * uTexSize + vec2(uFrameSeed * 5.588238, uFrameSeed * 3.14159);\n" +
            "  float ign = fract(52.9829189 * fract(dot(pixelPos, vec2(0.06711056, 0.00583715))));\n" +
            "  finalRgb += (ign - 0.5) / 255.0;\n" +
            "  gl_FragColor = vec4(clamp(finalRgb, 0.0, 1.0), 1.0);\n" +
            "}\n";

    private final int width;
    private final int height;
    private final HandlerThread thread;
    private final Handler handler;
    private SurfaceTexture surfaceTexture;
    private Surface inputSurface;
    private final Surface encoderSurface;
    private final float[] stMatrix = new float[16];
    private final int[] cleanTextures = new int[2];
    private final int[] cleanFbos = new int[2];
    private int oesTexture;
    private int historyIndex;
    private boolean hasHistory;
    private int frameSeed;
    private boolean frameAvailable;
    private boolean closed;

    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    private int pass1Program, p1Pos, p1Tex, p1St, p1Current, p1History, p1Size, p1HasHistory, p1Temporal;
    private int pass2Program, p2Pos, p2Tex, p2Clean, p2Size, p2DeBlur, p2Fine, p2Clarity, p2Noise, p2Seed;

    private StarlightVideoGL(Surface encoderSurface, HandlerThread thread,
                             Handler handler, int width, int height) {
        this.encoderSurface = encoderSurface;
        this.thread = thread;
        this.handler = handler;
        this.width = width;
        this.height = height;
    }

    static StarlightVideoGL create(Surface encoderSurface, int srcWidth, int srcHeight,
                                   int outWidth, int outHeight) {
        HandlerThread thread = new HandlerThread("StarlightVideoGL");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        StarlightVideoGL gl = new StarlightVideoGL(encoderSurface, thread, handler,
                Math.max(2, outWidth), Math.max(2, outHeight));
        if (!gl.init()) {
            gl.close();
            return null;
        }
        return gl;
    }

    Surface getInputSurface() {
        return inputSurface;
    }

    boolean awaitFrame(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (this) {
            while (!frameAvailable) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) return false;
                try {
                    wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            frameAvailable = false;
            return true;
        }
    }

    void updateTexImage() {
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(stMatrix);
    }

    boolean renderFrame(long ptsUs, float sharpen, float denoise, float sat, long frameIntervalUs) {
        if (closed) return false;
        if (!makeCurrent()) return false;
        try {
            renderPass1(denoise);
            if (!renderPass2(ptsUs, sharpen, sat)) return false;
            historyIndex = 1 - historyIndex;
            hasHistory = true;
            frameSeed = (frameSeed + 1) & 63;
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "renderFrame: " + t);
            return false;
        }
    }

    private boolean init() {
        if (!setupEgl()) return false;
        if (!makeCurrent()) return false;
        try {
            pass1Program = link(VERTEX_OES, FRAG_PASS1);
            pass2Program = link(VERTEX_2D, FRAG_PASS2);
            if (pass1Program == 0 || pass2Program == 0) return false;

            p1Pos = GLES20.glGetAttribLocation(pass1Program, "aPos");
            p1Tex = GLES20.glGetAttribLocation(pass1Program, "aTex");
            p1St = GLES20.glGetUniformLocation(pass1Program, "uSTMatrix");
            p1Current = GLES20.glGetUniformLocation(pass1Program, "uCurrentOES");
            p1History = GLES20.glGetUniformLocation(pass1Program, "uHistoryTex");
            p1Size = GLES20.glGetUniformLocation(pass1Program, "uTexSize");
            p1HasHistory = GLES20.glGetUniformLocation(pass1Program, "uHasHistory");
            p1Temporal = GLES20.glGetUniformLocation(pass1Program, "uTemporalStr");

            p2Pos = GLES20.glGetAttribLocation(pass2Program, "aPos");
            p2Tex = GLES20.glGetAttribLocation(pass2Program, "aTex");
            p2Clean = GLES20.glGetUniformLocation(pass2Program, "uCleanTex");
            p2Size = GLES20.glGetUniformLocation(pass2Program, "uTexSize");
            p2DeBlur = GLES20.glGetUniformLocation(pass2Program, "uDeBlurStr");
            p2Fine = GLES20.glGetUniformLocation(pass2Program, "uFineDetailStr");
            p2Clarity = GLES20.glGetUniformLocation(pass2Program, "uClarityStr");
            p2Noise = GLES20.glGetUniformLocation(pass2Program, "uNoiseFloor");
            p2Seed = GLES20.glGetUniformLocation(pass2Program, "uFrameSeed");

            oesTexture = createTexture(GL_TEXTURE_EXTERNAL_OES, width, height);
            for (int i = 0; i < 2; i++) {
                cleanTextures[i] = createTexture(GLES20.GL_TEXTURE_2D, width, height);
                cleanFbos[i] = createFbo(cleanTextures[i]);
            }
            if (oesTexture == 0 || cleanTextures[0] == 0 || cleanTextures[1] == 0
                    || cleanFbos[0] == 0 || cleanFbos[1] == 0) return false;

            SurfaceTexture st = new SurfaceTexture(oesTexture);
            st.setDefaultBufferSize(width, height);
            st.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (StarlightVideoGL.this) {
                        frameAvailable = true;
                        StarlightVideoGL.this.notifyAll();
                    }
                }
            }, handler);
            surfaceTexture = st;
            inputSurface = new Surface(st);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "init: " + t);
            return false;
        }
    }

    private boolean setupEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false;
        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false;
        int[] attrib = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] count = new int[1];
        if (!EGL14.eglChooseConfig(eglDisplay, attrib, 0, configs, 0, 1, count, 0)
                || count[0] <= 0) return false;
        int[] ctxAttrib = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_CONTEXT_PRIORITY_LEVEL_IMG, EGL_CONTEXT_PRIORITY_LOW_IMG,
                EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                ctxAttrib, 0);
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            int[] fallback = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0],
                    EGL14.EGL_NO_CONTEXT, fallback, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false;
        int[] surfaceAttrib = {EGL14.EGL_NONE};
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], encoderSurface,
                surfaceAttrib, 0);
        return eglSurface != EGL14.EGL_NO_SURFACE;
    }

    private boolean makeCurrent() {
        return eglDisplay != EGL14.EGL_NO_DISPLAY
                && eglSurface != EGL14.EGL_NO_SURFACE
                && EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    }

    private void renderPass1(float denoise) {
        int write = 1 - historyIndex;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, cleanFbos[write]);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glUseProgram(pass1Program);
        bindTexture(0, GL_TEXTURE_EXTERNAL_OES, oesTexture);
        bindTexture(1, GLES20.GL_TEXTURE_2D, hasHistory ? cleanTextures[historyIndex] : cleanTextures[write]);
        GLES20.glUniform1i(p1Current, 0);
        GLES20.glUniform1i(p1History, 1);
        GLES20.glUniform2f(p1Size, width, height);
        GLES20.glUniform1f(p1HasHistory, hasHistory ? 1.0f : 0.0f);
        GLES20.glUniform1f(p1Temporal, 0.15f + 0.30f * clamp01(denoise));
        GLES20.glUniformMatrix4fv(p1St, 1, false, stMatrix, 0);
        draw(p1Pos, p1Tex);
    }

    private boolean renderPass2(long ptsUs, float sharpen, float detail) {
        int clean = 1 - historyIndex;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glUseProgram(pass2Program);
        bindTexture(0, GLES20.GL_TEXTURE_2D, cleanTextures[clean]);
        GLES20.glUniform1i(p2Clean, 0);
        GLES20.glUniform2f(p2Size, width, height);
        GLES20.glUniform1f(p2DeBlur, 0.38f * (0.6f + 0.8f * clamp01(sharpen)));
        GLES20.glUniform1f(p2Fine, 0.45f * (0.5f + clamp01(detail)));
        GLES20.glUniform1f(p2Clarity, 0.18f * (0.5f + clamp01(detail)));
        GLES20.glUniform1f(p2Noise, 0.007f);
        GLES20.glUniform1f(p2Seed, (float) frameSeed);
        draw(p2Pos, p2Tex);
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, ptsUs * 1000L);
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    private void draw(int posLoc, int texLoc) {
        FloatBuffer buf = ByteBuffer.allocateDirect(QUAD.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(QUAD).position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, buf);
        buf.position(2);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 16, buf);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
    }

    private static void bindTexture(int unit, int target, int tex) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
        GLES20.glBindTexture(target, tex);
    }

    private static int createTexture(int target, int w, int h) {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        if (ids[0] == 0) return 0;
        GLES20.glBindTexture(target, ids[0]);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null);
        return ids[0];
    }

    private static int createFbo(int tex) {
        int[] ids = new int[1];
        GLES20.glGenFramebuffers(1, ids, 0);
        if (ids[0] == 0) return 0;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, ids[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, tex, 0);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "FBO incomplete: " + status);
            return 0;
        }
        return ids[0];
    }

    private static int link(String vertex, String fragment) {
        int vs = compile(GLES20.GL_VERTEX_SHADER, vertex);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment);
        if (vs == 0 || fs == 0) return 0;
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "link: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "compile: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    void close() {
        if (closed) return;
        closed = true;
        try {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface);
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext);
                }
                EGL14.eglTerminate(eglDisplay);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (inputSurface != null) inputSurface.release();
        } catch (Throwable ignored) {
        }
        try {
            if (surfaceTexture != null) surfaceTexture.release();
        } catch (Throwable ignored) {
        }
        try {
            thread.quitSafely();
        } catch (Throwable ignored) {
        }
    }
}
