package com.my.psremoteplay.feature.ps2.ui

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import com.my.psremoteplay.feature.ps2.ui.upscale.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.0 renderer with independent upscaler + sharpener pipeline.
 *
 * Pipeline: MediaCodec → SurfaceTexture → blit to sampler2D → [upscale] → [sharpen] → display.
 *
 * Upscaler and sharpener can be mixed independently for any combination.
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

    private var inputFboId = 0
    private var inputTexId = 0
    private var intermediateFboId = 0
    private var intermediateTexId = 0

    private var blitProgram = 0
    private var upscaleProgram = 0
    private var sharpenProgram = 0
    private var blitTexLoc = -1
    private var blitStMatrixLoc = -1

    private var quadVao = 0
    private var quadVbo = 0
    private var outputWidth = 0
    private var outputHeight = 0
    private val stMatrix = FloatArray(16)
    private var frameCount = 0L
    private var lastFpsTime = 0L
    private var framesInInterval = 0
    @Volatile var currentFps = 0
        private set

    // Current pipeline config
    @Volatile var sharpness: Float = 0.2f
    @Volatile private var pendingUpscaleMethod: UpscaleMethod = UpscaleMethod.BILINEAR
    @Volatile private var pendingSharpenMethod: SharpenMethod = SharpenMethod.NONE
    private var activeUpscaleMethod: UpscaleMethod? = null
    private var activeSharpenMethod: SharpenMethod? = null
    private var activeUpscaleStrategy: UpscaleStrategy? = null
    private var activeSharpenStrategy: UpscaleStrategy? = null

    fun setUpscaleMethod(method: UpscaleMethod) { pendingUpscaleMethod = method }
    fun setSharpenMethod(method: SharpenMethod) { pendingSharpenMethod = method }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        log("GL: ${GLES30.glGetString(GLES30.GL_RENDERER)} — ${GLES30.glGetString(GLES30.GL_VERSION)}")
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        oesTextureId = createOesTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).also { st ->
            st.setDefaultBufferSize(inputWidth, inputHeight)
            surface = Surface(st).also { onSurfaceReady(it) }
        }

        blitProgram = compileProgram(VERTEX_SRC, BLIT_OES_FRAG)
        if (blitProgram != 0) {
            blitTexLoc = GLES30.glGetUniformLocation(blitProgram, "uTex")
            blitStMatrixLoc = GLES30.glGetUniformLocation(blitProgram, "uSTMatrix")
        }

        val fbo = createFbo(inputWidth, inputHeight)
        inputFboId = fbo.first; inputTexId = fbo.second
        quadVao = createFullscreenQuad()
        lastFpsTime = System.currentTimeMillis()
        applyPipeline()
    }

    // Letterbox viewport (maintains aspect ratio)
    private var vpX = 0
    private var vpY = 0
    private var vpW = 0
    private var vpH = 0

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        outputWidth = width; outputHeight = height

        // Calculate letterboxed viewport to maintain input aspect ratio
        val srcAspect = inputWidth.toFloat() / inputHeight
        val dstAspect = width.toFloat() / height
        if (srcAspect > dstAspect) {
            // Pillarbox (input wider than screen) — unlikely for 4:3 on wide phone
            vpW = width; vpH = (width / srcAspect).toInt()
            vpX = 0; vpY = (height - vpH) / 2
        } else {
            // Letterbox (input taller than screen) — 4:3 on 19.5:9 phone
            vpH = height; vpW = (height * srcAspect).toInt()
            vpX = (width - vpW) / 2; vpY = 0
        }
        log("Output: ${outputWidth}x${outputHeight}, viewport: ${vpW}x${vpH}+${vpX}+${vpY}")
        recreateFbo(vpW, vpH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val st = surfaceTexture ?: return
        if (outputWidth <= 0 || outputHeight <= 0 || blitProgram == 0) return

        if (pendingUpscaleMethod != activeUpscaleMethod || pendingSharpenMethod != activeSharpenMethod)
            applyPipeline()

        val upStrategy = activeUpscaleStrategy
        val shStrategy = activeSharpenStrategy
        val sharpVal = sharpness

        for (i in 0..9) { if (GLES30.glGetError() == GLES30.GL_NO_ERROR) break }
        try { st.updateTexImage() } catch (_: Exception) { return }
        st.getTransformMatrix(stMatrix)
        frameCount++
        updateFps()

        val hasUpscale = upStrategy != null && upscaleProgram != 0
        val hasSharpen = shStrategy != null && sharpenProgram != 0

        if (!hasUpscale && !hasSharpen) {
            blitOesToScreen(); return
        }

        // Blit OES → input FBO
        blitOesToFbo(inputFboId, inputWidth, inputHeight)

        if (hasUpscale && hasSharpen) {
            // Upscale → intermediate FBO, Sharpen → screen
            renderPass(upscaleProgram, inputTexId, intermediateFboId, vpW, vpH) {
                upStrategy!!.setUpscaleUniforms(upscaleProgram, inputWidth, inputHeight, vpW, vpH)
            }
            renderPass(sharpenProgram, intermediateTexId, 0, vpW, vpH) {
                shStrategy!!.setSharpenUniforms(sharpenProgram, vpW, vpH, sharpVal)
            }
        } else if (hasUpscale) {
            // Upscale → screen
            renderPass(upscaleProgram, inputTexId, 0, vpW, vpH) {
                upStrategy!!.setUpscaleUniforms(upscaleProgram, inputWidth, inputHeight, vpW, vpH)
            }
        } else {
            // Sharpen only: blit input to intermediate at viewport res, Sharpen → screen
            blitInputToFbo(intermediateFboId, vpW, vpH)
            renderPass(sharpenProgram, intermediateTexId, 0, vpW, vpH) {
                shStrategy!!.setSharpenUniforms(sharpenProgram, vpW, vpH, sharpVal)
            }
        }
    }

    private fun renderPass(program: Int, texId: Int, fboId: Int, w: Int, h: Int, setUniforms: () -> Unit) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        if (fboId == 0) {
            // Screen output: clear full screen, then render to letterboxed viewport
            GLES30.glViewport(0, 0, outputWidth, outputHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glViewport(vpX, vpY, vpW, vpH)
        } else {
            GLES30.glViewport(0, 0, w, h)
        }
        GLES30.glUseProgram(program)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        setUniforms()
        drawQuad()
    }

    private var passthroughProgram = 0

    private fun blitInputToFbo(fboId: Int, w: Int, h: Int) {
        if (passthroughProgram == 0) {
            passthroughProgram = compileProgram(VERTEX_SRC, PASSTHROUGH_FRAG)
        }
        if (passthroughProgram == 0) return
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, w, h)
        GLES30.glUseProgram(passthroughProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexId)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(passthroughProgram, "uInputTex"), 0)
        drawQuad()
    }

    private fun updateFps() {
        framesInInterval++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsTime
        if (elapsed >= 1000) {
            currentFps = (framesInInterval * 1000 / elapsed).toInt()
            framesInInterval = 0
            lastFpsTime = now
        }
    }

    // --- Pipeline management ---

    private fun applyPipeline() {
        val upMethod = pendingUpscaleMethod
        val shMethod = pendingSharpenMethod

        if (upscaleProgram != 0) { GLES30.glDeleteProgram(upscaleProgram); upscaleProgram = 0 }
        if (sharpenProgram != 0) { GLES30.glDeleteProgram(sharpenProgram); sharpenProgram = 0 }
        activeUpscaleStrategy = null
        activeSharpenStrategy = null

        // Compile upscaler
        if (upMethod != UpscaleMethod.BILINEAR) {
            val strategy = createUpscaleStrategy(upMethod)
            if (strategy != null) {
                upscaleProgram = compileProgram(VERTEX_SRC, strategy.upscaleFragShader)
                if (upscaleProgram != 0) activeUpscaleStrategy = strategy
                else log("WARN: ${upMethod.displayName} shader failed")
            }
        }

        // Compile sharpener
        if (shMethod != SharpenMethod.NONE) {
            val strategy = createSharpenStrategy(shMethod)
            val shaderSrc = strategy?.sharpenFragShader
            if (shaderSrc != null) {
                sharpenProgram = compileProgram(VERTEX_SRC, shaderSrc)
                if (sharpenProgram != 0) activeSharpenStrategy = strategy
                else log("WARN: ${shMethod.displayName} shader failed")
            }
        }

        activeUpscaleMethod = upMethod
        activeSharpenMethod = shMethod
        log("Pipeline: ${upMethod.displayName} → ${shMethod.displayName} " +
            "(upscale=$upscaleProgram sharpen=$sharpenProgram)")
    }

    private fun createUpscaleStrategy(method: UpscaleMethod): UpscaleStrategy? = when (method) {
        UpscaleMethod.CATMULL_ROM -> CatmullRomCasStrategy()
        UpscaleMethod.FSR_EASU -> FsrStrategy()
        UpscaleMethod.CUSTOM -> CustomUpscaleStrategy()
        UpscaleMethod.MATRIX_FILTER -> MatrixFilterBankStrategy()
        UpscaleMethod.FEATURE_GUIDED -> FeatureGuidedStrategy()
        UpscaleMethod.RAISR -> RaisrStrategy()
        UpscaleMethod.LUT_UPSCALE -> LutUpscaleStrategy()
        UpscaleMethod.DEBLOCK -> DeblockUpscaleStrategy()
        else -> null
    }

    private fun createSharpenStrategy(method: SharpenMethod): UpscaleStrategy? = when (method) {
        SharpenMethod.CAS -> CatmullRomCasStrategy()
        SharpenMethod.FSR_RCAS -> FsrStrategy()
        SharpenMethod.CUSTOM_USM -> CustomUpscaleStrategy()
        SharpenMethod.FEATURE_SHARPEN -> FeatureGuidedStrategy()
        SharpenMethod.DEBLOCK_SHARPEN -> DeblockUpscaleStrategy()
        else -> null
    }

    private fun recreateFbo(w: Int, h: Int) {
        if (intermediateFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(intermediateFboId), 0)
            GLES30.glDeleteTextures(1, intArrayOf(intermediateTexId), 0)
        }
        val fbo = createFbo(w, h)
        intermediateFboId = fbo.first; intermediateTexId = fbo.second
    }

    // --- Blit helpers ---

    private fun blitOesToScreen() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glViewport(vpX, vpY, vpW, vpH)
        GLES30.glUseProgram(blitProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(blitTexLoc, 0)
        GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
        drawQuad()
    }

    private fun blitOesToFbo(fboId: Int, w: Int, h: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, w, h)
        GLES30.glUseProgram(blitProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(blitTexLoc, 0)
        GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
        drawQuad()
    }

    fun release() {
        onSurfaceDestroyed()
        surface?.release(); surface = null
        surfaceTexture?.release(); surfaceTexture = null
        deleteGl(GLES30::glDeleteTextures, oesTextureId, inputTexId, intermediateTexId)
        deleteGl(GLES30::glDeleteFramebuffers, inputFboId, intermediateFboId)
        deleteGl(GLES30::glDeleteVertexArrays, quadVao)
        deleteGl(GLES30::glDeleteBuffers, quadVbo)
        for (p in intArrayOf(blitProgram, upscaleProgram, sharpenProgram, passthroughProgram))
            if (p != 0) GLES30.glDeleteProgram(p)
    }

    private fun deleteGl(fn: (Int, IntArray, Int) -> Unit, vararg ids: Int) {
        for (id in ids) if (id != 0) fn(1, intArrayOf(id), 0)
    }

    private fun log(msg: String) = android.util.Log.d("UPSCALE", msg)
    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVao); GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4); GLES30.glBindVertexArray(0)
    }

    // --- GL resource creation ---

    private fun createOesTexture(): Int {
        val ids = IntArray(1); GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        for (p in intArrayOf(GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_TEXTURE_MAG_FILTER))
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, p, GLES30.GL_LINEAR)
        for (p in intArrayOf(GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_TEXTURE_WRAP_T))
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, p, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun createFbo(w: Int, h: Int): Pair<Int, Int> {
        val tex = IntArray(1); GLES30.glGenTextures(1, tex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        val fbo = IntArray(1); GLES30.glGenFramebuffers(1, fbo, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex[0], 0)
        val st = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        if (st != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            GLES30.glDeleteFramebuffers(1, fbo, 0); GLES30.glDeleteTextures(1, tex, 0); return Pair(0, 0)
        }
        return Pair(fbo[0], tex[0])
    }

    private fun createFullscreenQuad(): Int {
        val v = floatArrayOf(-1f,-1f,0f,0f, 1f,-1f,1f,0f, -1f,1f,0f,1f, 1f,1f,1f,1f)
        val buf = ByteBuffer.allocateDirect(v.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(v).apply { position(0) }
        val vao = IntArray(1); GLES30.glGenVertexArrays(1, vao, 0); GLES30.glBindVertexArray(vao[0])
        val vbo = IntArray(1); GLES30.glGenBuffers(1, vbo, 0); quadVbo = vbo[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, v.size*4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0); GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8); GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindVertexArray(0); return vao[0]
    }

    private fun compileProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        if (vs == 0 || fs == 0) return 0
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, vs); GLES30.glAttachShader(p, fs)
        GLES30.glBindAttribLocation(p, 0, "aPosition"); GLES30.glBindAttribLocation(p, 1, "aTexCoord")
        GLES30.glLinkProgram(p)
        val s = IntArray(1); GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, s, 0)
        GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs)
        if (s[0] == 0) { log("Link failed: ${GLES30.glGetProgramInfoLog(p)}"); GLES30.glDeleteProgram(p); return 0 }
        return p
    }

    private fun compileShader(type: Int, src: String): Int {
        val sh = GLES30.glCreateShader(type)
        GLES30.glShaderSource(sh, src.trimIndent().trim()); GLES30.glCompileShader(sh)
        val s = IntArray(1); GLES30.glGetShaderiv(sh, GLES30.GL_COMPILE_STATUS, s, 0)
        if (s[0] == 0) { log("${if (type==GLES30.GL_VERTEX_SHADER) "vert" else "frag"} compile failed: ${GLES30.glGetShaderInfoLog(sh)}"); GLES30.glDeleteShader(sh); return 0 }
        return sh
    }

    companion object {
        private const val VERTEX_SRC = """
#version 300 es
layout(location=0) in vec2 aPosition;
layout(location=1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main() { gl_Position = vec4(aPosition, 0.0, 1.0); vTexCoord = aTexCoord; }
"""
        private const val PASSTHROUGH_FRAG = """
#version 300 es
precision mediump float;
uniform sampler2D uInputTex;
in vec2 vTexCoord;
out vec4 fragColor;
void main() { fragColor = texture(uInputTex, vTexCoord); }
"""
        private const val BLIT_OES_FRAG = """
#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTex;
uniform mat4 uSTMatrix;
in vec2 vTexCoord;
out vec4 fragColor;
void main() { fragColor = texture(uTex, (uSTMatrix * vec4(vTexCoord, 0.0, 1.0)).xy); }
"""
    }
}
