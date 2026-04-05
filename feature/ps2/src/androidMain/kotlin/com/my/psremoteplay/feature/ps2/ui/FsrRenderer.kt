package com.my.psremoteplay.feature.ps2.ui

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.0 renderer implementing AMD FidelityFX Super Resolution (FSR) 1.0.
 *
 * Pipeline: MediaCodec → SurfaceTexture (OES) → EASU upscale → RCAS sharpen → display.
 *
 * EASU = Edge-Adaptive Spatial Upscaling (12-tap directional Lanczos)
 * RCAS = Robust Contrast-Adaptive Sharpening (5-tap cross filter)
 */
class FsrRenderer(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val onSurfaceReady: (Surface) -> Unit,
    private val onSurfaceDestroyed: () -> Unit
) : GLSurfaceView.Renderer {

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var oesTextureId = 0

    // Input blit FBO (OES → sampler2D at input resolution)
    private var inputFboId = 0
    private var inputTexId = 0

    // EASU intermediate FBO (at output resolution)
    private var easuFboId = 0
    private var easuTexId = 0

    // Shader programs
    private var blitProgram = 0
    private var easuProgram = 0
    private var rcasProgram = 0

    // Fullscreen quad
    private var quadVao = 0
    private var quadVbo = 0

    private var outputWidth = 0
    private var outputHeight = 0
    private val stMatrix = FloatArray(16)
    private var frameCount = 0L

    // Cached uniform locations (avoid per-frame glGetUniformLocation calls)
    private var blitTexLoc = -1
    private var blitStMatrixLoc = -1
    private var easuInputTexLoc = -1
    private var easuInputSizeLoc = -1
    private var easuOutputSizeLoc = -1
    private var rcasInputTexLoc = -1
    private var rcasTexelSizeLoc = -1
    private var rcasSharpnessLoc = -1

    @Volatile
    var sharpness: Float = 0.2f

    @Volatile
    var fsrEnabled: Boolean = true

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION)
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER)
        val glVendor = GLES30.glGetString(GLES30.GL_VENDOR)
        log("GL init: $glRenderer ($glVendor) — $glVersion")
        log("FSR input: ${inputWidth}x${inputHeight}")

        GLES30.glClearColor(0f, 0f, 0f, 1f)

        // Create OES texture for MediaCodec output
        oesTextureId = createOesTexture()
        log("OES texture created: id=$oesTextureId")

        // Create SurfaceTexture and Surface for MediaCodec
        surfaceTexture = SurfaceTexture(oesTextureId).also { st ->
            st.setDefaultBufferSize(inputWidth, inputHeight)
            surface = Surface(st).also { onSurfaceReady(it) }
        }
        log("SurfaceTexture + Surface ready for MediaCodec")

        // Compile shaders (FSR shaders may fail on some GPUs — fall back to blit)
        blitProgram = compileProgram("blit", VERTEX_SRC, BLIT_OES_FRAG)
        easuProgram = compileProgram("easu", VERTEX_SRC, EASU_FRAG)
        rcasProgram = compileProgram("rcas", VERTEX_SRC, RCAS_FRAG)
        log("Shaders: blit=$blitProgram easu=$easuProgram rcas=$rcasProgram")

        if (blitProgram == 0) {
            log("ERROR: Blit shader failed — rendering will be broken")
        }
        if (easuProgram == 0 || rcasProgram == 0) {
            log("WARN: FSR shaders failed, falling back to bilinear blit")
            fsrEnabled = false
        } else {
            log("FSR enabled: EASU + RCAS pipeline active")
        }

        // Cache uniform locations
        if (blitProgram != 0) {
            blitTexLoc = GLES30.glGetUniformLocation(blitProgram, "uTex")
            blitStMatrixLoc = GLES30.glGetUniformLocation(blitProgram, "uSTMatrix")
        }
        if (easuProgram != 0) {
            easuInputTexLoc = GLES30.glGetUniformLocation(easuProgram, "uInputTex")
            easuInputSizeLoc = GLES30.glGetUniformLocation(easuProgram, "uInputSize")
            easuOutputSizeLoc = GLES30.glGetUniformLocation(easuProgram, "uOutputSize")
        }
        if (rcasProgram != 0) {
            rcasInputTexLoc = GLES30.glGetUniformLocation(rcasProgram, "uInputTex")
            rcasTexelSizeLoc = GLES30.glGetUniformLocation(rcasProgram, "uTexelSize")
            rcasSharpnessLoc = GLES30.glGetUniformLocation(rcasProgram, "uSharpness")
        }

        // Create input FBO (OES → sampler2D at input resolution, created once)
        val inputFbo = createFbo(inputWidth, inputHeight)
        if (inputFbo.first == 0) {
            log("ERROR: Input FBO creation failed, disabling FSR")
            fsrEnabled = false
        }
        inputFboId = inputFbo.first
        inputTexId = inputFbo.second
        log("Input FBO: fbo=$inputFboId tex=$inputTexId (${inputWidth}x${inputHeight})")

        // Create fullscreen quad VAO
        quadVao = createFullscreenQuad()
        log("Fullscreen quad VAO: $quadVao")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
        log("Surface changed: ${outputWidth}x${outputHeight} (scale: ${outputWidth.toFloat()/inputWidth}x)")

        // Recreate EASU FBO at output resolution
        if (easuFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(easuFboId), 0)
            GLES30.glDeleteTextures(1, intArrayOf(easuTexId), 0)
        }
        val fbo = createFbo(outputWidth, outputHeight)
        if (fbo.first == 0) {
            log("ERROR: EASU FBO creation failed, disabling FSR")
            fsrEnabled = false
        }
        easuFboId = fbo.first
        easuTexId = fbo.second
        log("EASU FBO: fbo=${easuFboId} tex=${easuTexId} (${outputWidth}x${outputHeight})")
    }

    override fun onDrawFrame(gl: GL10?) {
        val st = surfaceTexture ?: return
        if (outputWidth <= 0 || outputHeight <= 0) return

        // Snapshot volatile fields for thread safety
        val useFsr = fsrEnabled
        val sharpnessVal = sharpness

        // Clear stale GL errors before updateTexImage (max 10 to avoid infinite loop)
        for (i in 0..9) { if (GLES30.glGetError() == GLES30.GL_NO_ERROR) break }

        try {
            st.updateTexImage()
        } catch (_: Exception) {
            return
        }
        st.getTransformMatrix(stMatrix)
        frameCount++

        if (frameCount == 1L) {
            log("First frame rendered, fsrEnabled=$useFsr")
        }
        if (frameCount % 300 == 0L) {
            log("Frame $frameCount, mode=${if (useFsr) "FSR" else "blit"}, output=${outputWidth}x${outputHeight}")
        }

        if (!useFsr || blitProgram == 0) {
            // Simple blit without FSR
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, outputWidth, outputHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            if (blitProgram == 0) return
            GLES30.glUseProgram(blitProgram)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
            GLES30.glUniform1i(blitTexLoc, 0)
            GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
            drawQuad(quadVao)
            return
        }

        // === Pass 0: Blit OES → regular texture at input resolution ===
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, inputFboId)
        GLES30.glViewport(0, 0, inputWidth, inputHeight)
        GLES30.glUseProgram(blitProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(blitTexLoc, 0)
        GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
        drawQuad(quadVao)

        // === Pass 1: EASU (upscale input → output resolution) ===
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, easuFboId)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glUseProgram(easuProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexId)
        GLES30.glUniform1i(easuInputTexLoc, 0)
        GLES30.glUniform2f(easuInputSizeLoc, inputWidth.toFloat(), inputHeight.toFloat())
        GLES30.glUniform2f(easuOutputSizeLoc, outputWidth.toFloat(), outputHeight.toFloat())
        drawQuad(quadVao)

        // === Pass 2: RCAS (sharpen at output resolution) → screen ===
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(rcasProgram)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, easuTexId)
        GLES30.glUniform1i(rcasInputTexLoc, 0)
        GLES30.glUniform2f(rcasTexelSizeLoc, 1f / outputWidth, 1f / outputHeight)
        GLES30.glUniform1f(rcasSharpnessLoc, sharpnessVal)
        drawQuad(quadVao)
    }

    private fun log(msg: String) {
        android.util.Log.d("FSR", msg)
    }

    fun release() {
        onSurfaceDestroyed()
        surface?.release()
        surface = null
        surfaceTexture?.release()
        surfaceTexture = null

        // Clean up GL resources
        deleteGlResource(GLES30::glDeleteTextures, oesTextureId)
        deleteGlResource(GLES30::glDeleteTextures, inputTexId)
        deleteGlResource(GLES30::glDeleteTextures, easuTexId)
        deleteGlResource(GLES30::glDeleteFramebuffers, inputFboId)
        deleteGlResource(GLES30::glDeleteFramebuffers, easuFboId)
        deleteGlResource(GLES30::glDeleteVertexArrays, quadVao)
        deleteGlResource(GLES30::glDeleteBuffers, quadVbo)
        if (blitProgram != 0) GLES30.glDeleteProgram(blitProgram)
        if (easuProgram != 0) GLES30.glDeleteProgram(easuProgram)
        if (rcasProgram != 0) GLES30.glDeleteProgram(rcasProgram)
        log("GL resources released")
    }

    private fun deleteGlResource(deleter: (Int, IntArray, Int) -> Unit, id: Int) {
        if (id != 0) deleter(1, intArrayOf(id), 0)
    }

    // --- GL helpers ---

    private fun createOesTexture(): Int {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texIds[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return texIds[0]
    }

    private fun createFbo(width: Int, height: Int): Pair<Int, Int> {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texIds[0])
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val fboIds = IntArray(1)
        GLES30.glGenFramebuffers(1, fboIds, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboIds[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, texIds[0], 0
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            log("FBO incomplete! status=0x${status.toString(16)} size=${width}x${height}")
            GLES30.glDeleteFramebuffers(1, fboIds, 0)
            GLES30.glDeleteTextures(1, texIds, 0)
            return Pair(0, 0)
        }
        return Pair(fboIds[0], texIds[0])
    }

    private fun createFullscreenQuad(): Int {
        val vertices = floatArrayOf(
            // x, y, u, v
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )
        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply { position(0) }

        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        GLES30.glBindVertexArray(vaoIds[0])

        val vboIds = IntArray(1)
        GLES30.glGenBuffers(1, vboIds, 0)
        quadVbo = vboIds[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_STATIC_DRAW)

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindVertexArray(0)
        return vaoIds[0]
    }

    private fun drawQuad(vao: Int) {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun compileProgram(name: String, vertSrc: String, fragSrc: String): Int {
        val vert = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vert)
        GLES30.glAttachShader(prog, frag)
        GLES30.glBindAttribLocation(prog, 0, "aPosition")
        GLES30.glBindAttribLocation(prog, 1, "aTexCoord")
        GLES30.glLinkProgram(prog)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            android.util.Log.e("FsrRenderer", "Program link failed: $log")
            GLES30.glDeleteProgram(prog)
            return 0
        }

        GLES30.glDeleteShader(vert)
        GLES30.glDeleteShader(frag)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src.trimIndent().trim())
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            val typeName = if (type == GLES30.GL_VERTEX_SHADER) "vertex" else "fragment"
            android.util.Log.e("FsrRenderer", "$typeName shader compile failed: $log")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val VERTEX_SRC = """
#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main() {
    gl_Position = vec4(aPosition, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
"""

        /** Simple OES → screen blit with SurfaceTexture transform */
        private const val BLIT_OES_FRAG = """
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTex;
uniform mat4 uSTMatrix;
in vec2 vTexCoord;
out vec4 fragColor;
void main() {
    vec2 tc = (uSTMatrix * vec4(vTexCoord, 0.0, 1.0)).xy;
    fragColor = texture(uTex, tc);
}
"""

        /**
         * FSR EASU — Edge-Adaptive Spatial Upscaling.
         *
         * 12-tap directional Lanczos filter that detects edge orientation
         * and elongates the filter kernel along edges to preserve sharpness.
         */
        private const val EASU_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

float Luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Lanczos2 approximation: (25/16)(1/4 d^2 - 1)^2 - (25/16 - 1)
float EasuWeight(float dist2) {
    float x = 0.25 * dist2 - 1.0;
    return clamp(x * x * 1.5625 - 0.5625, 0.0, 1.0);
}

void main() {
    vec2 ps = 1.0 / uInputSize;
    vec2 srcPos = vTexCoord * uInputSize;
    vec2 srcCenter = floor(srcPos - 0.5) + 0.5;
    vec2 f = srcPos - srcCenter;

    vec2 tc = srcCenter * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // 12-tap sampling pattern:
    //   b c
    //  e f g h
    //  i j k l
    //   n o
    vec3 b = texture(uInputTex, tc - dy).rgb;
    vec3 c = texture(uInputTex, tc - dy + dx).rgb;
    vec3 e = texture(uInputTex, tc - dx).rgb;
    vec3 fc = texture(uInputTex, tc).rgb;
    vec3 g = texture(uInputTex, tc + dx).rgb;
    vec3 h = texture(uInputTex, tc + 2.0 * dx).rgb;
    vec3 i = texture(uInputTex, tc + dy - dx).rgb;
    vec3 j = texture(uInputTex, tc + dy).rgb;
    vec3 k = texture(uInputTex, tc + dy + dx).rgb;
    vec3 l = texture(uInputTex, tc + dy + 2.0 * dx).rgb;
    vec3 n = texture(uInputTex, tc + 2.0 * dy).rgb;
    vec3 o = texture(uInputTex, tc + 2.0 * dy + dx).rgb;

    // Luminance for edge detection
    float bL=Luma(b), cL=Luma(c), eL=Luma(e), fL=Luma(fc);
    float gL=Luma(g), hL=Luma(h), iL=Luma(i), jL=Luma(j);
    float kL=Luma(k), lL=Luma(l), nL=Luma(n), oL=Luma(o);

    // Directional gradient estimation
    float dirH = abs(bL-fL) + abs(cL-gL) + abs(eL-iL) + abs(fL-jL) +
                 abs(gL-kL) + abs(hL-lL) + abs(jL-nL) + abs(kL-oL);
    float dirV = abs(eL-fL) + abs(fL-gL) + abs(gL-hL) + abs(iL-jL) +
                 abs(jL-kL) + abs(kL-lL) + abs(bL-cL) + abs(nL-oL);

    // Edge direction and stretch
    float dirWeight = dirV / (dirH + dirV + 1e-5);
    float edgeStr = clamp(max(dirH,dirV) / (min(dirH,dirV) + 1e-5), 1.0, 4.0);
    float stretchX = mix(1.0, 1.0/edgeStr, dirWeight);
    float stretchY = mix(1.0/edgeStr, 1.0, dirWeight);

    // Weighted sum with directional kernel
    vec3 colors[12];
    colors[0]=b;  colors[1]=c;  colors[2]=e;  colors[3]=fc;
    colors[4]=g;  colors[5]=h;  colors[6]=i;  colors[7]=j;
    colors[8]=k;  colors[9]=l;  colors[10]=n; colors[11]=o;

    // Tap offsets relative to fractional position
    float ox[12], oy[12];
    ox[0]=-f.x;        oy[0]=-1.0-f.y;     // b
    ox[1]=1.0-f.x;     oy[1]=-1.0-f.y;     // c
    ox[2]=-1.0-f.x;    oy[2]=-f.y;          // e
    ox[3]=-f.x;         oy[3]=-f.y;          // f
    ox[4]=1.0-f.x;     oy[4]=-f.y;          // g
    ox[5]=2.0-f.x;     oy[5]=-f.y;          // h
    ox[6]=-1.0-f.x;    oy[6]=1.0-f.y;      // i
    ox[7]=-f.x;         oy[7]=1.0-f.y;      // j
    ox[8]=1.0-f.x;     oy[8]=1.0-f.y;      // k
    ox[9]=2.0-f.x;     oy[9]=1.0-f.y;      // l
    ox[10]=-f.x;        oy[10]=2.0-f.y;     // n
    ox[11]=1.0-f.x;    oy[11]=2.0-f.y;     // o

    vec3 result = vec3(0.0);
    float totalW = 0.0;
    for (int t = 0; t < 12; t++) {
        float d2 = (ox[t]*stretchX)*(ox[t]*stretchX) + (oy[t]*stretchY)*(oy[t]*stretchY);
        float w = EasuWeight(d2);
        result += colors[t] * w;
        totalW += w;
    }
    fragColor = vec4(result / max(totalW, 1e-5), 1.0);
}
"""

        /**
         * FSR RCAS — Robust Contrast-Adaptive Sharpening.
         *
         * 5-tap cross filter with adaptive strength: sharpens flat areas,
         * avoids amplifying noise in high-contrast regions.
         */
        private const val RCAS_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uTexelSize;
uniform float uSharpness; // 0.0 = max sharpen, 1.0 = none

in vec2 vTexCoord;
out vec4 fragColor;

float Luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 center = texture(uInputTex, vTexCoord).rgb;
    vec3 north  = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 south  = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;
    vec3 west   = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 east   = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;

    float mn = min(min(Luma(north), Luma(south)), min(Luma(west), Luma(east)));
    float mx = max(max(Luma(north), Luma(south)), max(Luma(west), Luma(east)));
    mn = min(mn, Luma(center));
    mx = max(mx, Luma(center));

    float peakC = sqrt(1.0 - min(mn, 1.0 - mx));
    float w = peakC * mix(-0.125, -0.2, 1.0 - uSharpness);
    w = max(w, -0.25);

    float rcpW = 1.0 / (1.0 + 4.0 * w);
    vec3 result = clamp((center + w * (north + south + west + east)) * rcpW, 0.0, 1.0);
    fragColor = vec4(result, 1.0);
}
"""
    }
}
