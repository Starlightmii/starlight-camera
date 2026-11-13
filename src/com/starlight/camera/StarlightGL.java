package com.starlight.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Offscreen GLES2 renderer for Starlight SR.
 *
 * The resize, saturation/contrast matrix and edge-gated unsharp are all done
 * in one fragment shader, so the GPU does the per-pixel work instead of the
 * Java CPU loops. RenderScript is kept only as a fallback because Android 12+
 * devices often expose it as a CPU-only implementation.
 */
public final class StarlightGL implements AutoCloseable {
    private static final String TAG = "StarlightGL";

    private static final int TILE = 1024;

    private static final float[] QUAD = {
            -1.0f, -1.0f, 0.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f, 1.0f,
             1.0f,  1.0f, 1.0f, 1.0f
    };

    private static final String VERTEX_SHADER =
            "attribute vec4 aPos;\n" +
            "attribute vec2 aTex;\n" +
            "uniform vec2 uTileOffset;\n" +
            "uniform vec2 uTileSize;\n" +
            "uniform vec2 uOutSize;\n" +
            "varying vec2 vTex;\n" +
            "void main() {\n" +
            "  gl_Position = aPos;\n" +
            "  vTex = (uTileOffset + aTex * uTileSize) / uOutSize;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision highp float;\n" +
            "varying vec2 vTex;\n" +
            "uniform sampler2D uTex;\n" +
            "uniform vec2 uTexSize;\n" +
            "uniform float uSharpen;\n" +
            "uniform float uBicubic;\n" +
            "vec4 cubic(float v) {\n" +
            "  vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;\n" +
            "  vec4 s = n * n * n;\n" +
            "  float x = s.x;\n" +
            "  float y = s.y - 4.0 * s.x;\n" +
            "  float z = s.z - 4.0 * s.y + 6.0 * s.x;\n" +
            "  float w = 6.0 - x - y - z;\n" +
            "  return vec4(x, y, z, w) * (1.0 / 6.0);\n" +
            "}\n" +
            "vec4 bicubic(sampler2D tex, vec2 uv, vec2 texSize) {\n" +
            "  vec2 inv = 1.0 / texSize;\n" +
            "  vec2 p = uv * texSize - 0.5;\n" +
            "  vec2 f = fract(p);\n" +
            "  p -= f;\n" +
            "  vec4 xc = cubic(f.x);\n" +
            "  vec4 yc = cubic(f.y);\n" +
            "  vec4 c = p.xxyy + vec2(-0.5, 1.5).xyxy;\n" +
            "  vec4 s = vec4(xc.xz + xc.yw, yc.xz + yc.yw);\n" +
            "  vec4 o = c + vec4(xc.yw, yc.yw) / s;\n" +
            "  o *= inv.xxyy;\n" +
            "  vec4 s0 = texture2D(tex, o.xz);\n" +
            "  vec4 s1 = texture2D(tex, o.yz);\n" +
            "  vec4 s2 = texture2D(tex, o.xw);\n" +
            "  vec4 s3 = texture2D(tex, o.yw);\n" +
            "  float sx = s.x / (s.x + s.y);\n" +
            "  float sy = s.z / (s.z + s.w);\n" +
            "  return mix(mix(s3, s2, sx), mix(s1, s0, sx), sy);\n" +
            "}\n" +
            "float luma(vec3 c) { return dot(c, vec3(0.213, 0.715, 0.072)); }\n" +
            "void main() {\n" +
            "  vec2 uv = vTex;\n" +
            "  vec4 c = uBicubic > 0.5 ? bicubic(uTex, uv, uTexSize) : texture2D(uTex, uv);\n" +
            "  vec3 rgb = c.rgb;\n" +
            "  float lum = luma(rgb);\n" +
            "  rgb = mix(vec3(lum), rgb, 1.04);\n" +
            "  if (uSharpen > 0.0) {\n" +
            "    vec2 t = 1.0 / uTexSize;\n" +
            "    float l = luma(texture2D(uTex, uv - vec2(t.x, 0.0)).rgb);\n" +
            "    float r = luma(texture2D(uTex, uv + vec2(t.x, 0.0)).rgb);\n" +
            "    float u = luma(texture2D(uTex, uv - vec2(0.0, t.y)).rgb);\n" +
            "    float d = luma(texture2D(uTex, uv + vec2(0.0, t.y)).rgb);\n" +
            "    float blur = (l + r + u + d) * 0.25;\n" +
            "    float detail = luma(c.rgb) - blur;\n" +
            "    if (abs(detail) > 0.012) {\n" +
            "      rgb += vec3(detail * uSharpen);\n" +
            "    }\n" +
            "  }\n" +
            "  gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);\n" +
            "}\n";

    private final EGLDisplay display;
    private final EGLSurface surface;
    private final EGLContext context;
    private final EGLConfig config;
    private final int program;
    private final int aPos;
    private final int aTex;
    private final int uTex;
    private final int uTexSize;
    private final int uSharpen;
    private final int uBicubic;
    private final int uTileOffset;
    private final int uTileSize;
    private final int uOutSize;
    private final int inputTexture;
    private final FloatBuffer quad;
    private int outTexture = 0;
    private int fbo = 0;
    private int tileSize = 0;
    private int inputW = 0;
    private int inputH = 0;
    private EGLSurface windowSurface = EGL14.EGL_NO_SURFACE;
    private ByteBuffer readBuffer;

    private StarlightGL(EGLDisplay display, EGLSurface surface, EGLContext context,
                        EGLConfig config, int program, int aPos, int aTex, int uTex,
                        int uTexSize, int uSharpen, int uBicubic,
                        int uTileOffset, int uTileSize, int uOutSize,
                        int inputTexture, FloatBuffer quad) {
        this.display = display;
        this.surface = surface;
        this.context = context;
        this.config = config;
        this.program = program;
        this.aPos = aPos;
        this.aTex = aTex;
        this.uTex = uTex;
        this.uTexSize = uTexSize;
        this.uSharpen = uSharpen;
        this.uBicubic = uBicubic;
        this.uTileOffset = uTileOffset;
        this.uTileSize = uTileSize;
        this.uOutSize = uOutSize;
        this.inputTexture = inputTexture;
        this.quad = quad;
    }

    public static StarlightGL create() {
        EGLDisplay display = EGL14.EGL_NO_DISPLAY;
        EGLSurface surface = EGL14.EGL_NO_SURFACE;
        EGLContext context = EGL14.EGL_NO_CONTEXT;
        int program = 0;
        int inputTexture = 0;
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (display == EGL14.EGL_NO_DISPLAY) {
                return null;
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                return null;
            }
            int[] configAttribs = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT | EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)
                    || numConfigs[0] <= 0) {
                return null;
            }
            int[] contextAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (context == EGL14.EGL_NO_CONTEXT) {
                return null;
            }
            int[] surfaceAttribs = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};
            surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0);
            if (surface == EGL14.EGL_NO_SURFACE) {
                return null;
            }
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                return null;
            }

            int vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            if (vs == 0 || fs == 0) {
                return null;
            }
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vs);
            GLES20.glAttachShader(program, fs);
            GLES20.glLinkProgram(program);
            int[] linked = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            if (linked[0] == 0) {
                Log.w(TAG, "link: " + GLES20.glGetProgramInfoLog(program));
                return null;
            }
            int aPos = GLES20.glGetAttribLocation(program, "aPos");
            int aTex = GLES20.glGetAttribLocation(program, "aTex");
            int uTex = GLES20.glGetUniformLocation(program, "uTex");
            int uTexSize = GLES20.glGetUniformLocation(program, "uTexSize");
            int uSharpen = GLES20.glGetUniformLocation(program, "uSharpen");
            int uBicubic = GLES20.glGetUniformLocation(program, "uBicubic");
            int uTileOffset = GLES20.glGetUniformLocation(program, "uTileOffset");
            int uTileSize = GLES20.glGetUniformLocation(program, "uTileSize");
            int uOutSize = GLES20.glGetUniformLocation(program, "uOutSize");
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            inputTexture = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            FloatBuffer quad = ByteBuffer.allocateDirect(QUAD.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            quad.put(QUAD).position(0);
            Log.i(TAG, "GLES2 renderer ready");
            return new StarlightGL(display, surface, context, configs[0], program, aPos, aTex, uTex,
                    uTexSize, uSharpen, uBicubic, uTileOffset, uTileSize, uOutSize,
                    inputTexture, quad);
        } catch (Throwable th) {
            Log.w(TAG, "create failed: " + th);
            return null;
        }
    }

    public Bitmap resize(Bitmap source, int width, int height, boolean bicubic, float sharpen) {
        try {
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                return null;
            }
            ensureTile();
            if (fbo == 0) {
                return null;
            }
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
            for (int y = 0; y < height; y += TILE) {
                int th = Math.min(TILE, height - y);
                for (int x = 0; x < width; x += TILE) {
                    int tw = Math.min(TILE, width - x);
                    drawQuad(source, x, y, tw, th, width, height, bicubic, sharpen);
                    readBuffer.position(0);
                    GLES20.glReadPixels(0, 0, tw, th, GLES20.GL_RGBA,
                            GLES20.GL_UNSIGNED_BYTE, readBuffer);
                    int err = GLES20.glGetError();
                    if (err != GLES20.GL_NO_ERROR) {
                        Log.w(TAG, "tile readback glError=0x" + Integer.toHexString(err));
                        out.recycle();
                        return null;
                    }
                    Bitmap tile = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
                    readBuffer.position(0);
                    tile.copyPixelsFromBuffer(readBuffer);
                    canvas.drawBitmap(tile, x, y, null);
                    tile.recycle();
                }
            }
            return out;
        } catch (Throwable th) {
            Log.w(TAG, "resize failed: " + th);
            return null;
        }
    }

    private void ensureTile() {
        if (fbo != 0 && readBuffer != null) {
            return;
        }
        releaseOutput();
        tileSize = TILE;
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        outTexture = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, outTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, TILE, TILE, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        fbo = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, outTexture, 0);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "FBO incomplete");
            releaseOutput();
            return;
        }
        readBuffer = ByteBuffer.allocateDirect(TILE * TILE * 4).order(ByteOrder.nativeOrder());
    }

    /**
     * Attach an encoder input surface. In this mode frames are rendered
     * directly into MediaCodec's surface and never read back to a Bitmap,
     * which removes the per-frame 33 MB RGBA readback and the CPU YUV copy.
     */
    public boolean attachSurface(Surface surface) {
        try {
            if (windowSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(display, windowSurface);
                windowSurface = EGL14.EGL_NO_SURFACE;
            }
            windowSurface = EGL14.eglCreateWindowSurface(display, config, surface,
                    new int[]{EGL14.EGL_NONE}, 0);
            if (windowSurface == EGL14.EGL_NO_SURFACE) {
                Log.w(TAG, "window surface failed");
                return false;
            }
            return EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context);
        } catch (Throwable th) {
            Log.w(TAG, "attachSurface failed: " + th);
            return false;
        }
    }

    /** Render one video frame straight into the attached encoder surface. */
    public boolean renderToSurface(Bitmap source, long presentationTimeNs,
                                   int width, int height, boolean bicubic, float sharpen) {
        try {
            if (windowSurface == EGL14.EGL_NO_SURFACE) {
                return false;
            }
            if (!EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context)) {
                return false;
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            drawQuad(source, 0, 0, width, height, width, height, bicubic, sharpen);
            EGLExt.eglPresentationTimeANDROID(display, windowSurface, presentationTimeNs);
            return EGL14.eglSwapBuffers(display, windowSurface);
        } catch (Throwable th) {
            Log.w(TAG, "renderToSurface failed: " + th);
            return false;
        }
    }

    private void drawQuad(Bitmap source, int x0, int y0, int width, int height,
                          int outWidth, int outHeight, boolean bicubic, float sharpen) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
        if (source.getWidth() != inputW || source.getHeight() != inputH) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, source, 0);
            inputW = source.getWidth();
            inputH = source.getHeight();
        } else {
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, source);
        }
        GLES20.glUniform1i(uTex, 0);
        GLES20.glUniform2f(uTexSize, source.getWidth(), source.getHeight());
        GLES20.glUniform1f(uSharpen, sharpen);
        GLES20.glUniform1f(uBicubic, bicubic ? 1.0f : 0.0f);
        GLES20.glUniform2f(uTileOffset, x0, y0);
        GLES20.glUniform2f(uTileSize, width, height);
        GLES20.glUniform2f(uOutSize, outWidth, outHeight);
        quad.position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quad);
        quad.position(2);
        GLES20.glEnableVertexAttribArray(aTex);
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 16, quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aTex);
    }

    private static int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.w(TAG, "shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void releaseOutput() {
        if (fbo != 0) {
            GLES20.glDeleteFramebuffers(1, new int[]{fbo}, 0);
            fbo = 0;
        }
        if (outTexture != 0) {
            GLES20.glDeleteTextures(1, new int[]{outTexture}, 0);
            outTexture = 0;
        }
        tileSize = 0;
        readBuffer = null;
    }

    @Override
    public void close() {
        try {
            EGL14.eglMakeCurrent(display, surface, surface, context);
            releaseOutput();
            GLES20.glDeleteTextures(1, new int[]{inputTexture}, 0);
            GLES20.glDeleteProgram(program);
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (windowSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, windowSurface);
                windowSurface = EGL14.EGL_NO_SURFACE;
            }
            EGL14.eglDestroySurface(display, surface);
            EGL14.eglDestroyContext(display, context);
            EGL14.eglTerminate(display);
        } catch (Throwable ignored) {
        }
    }
}
