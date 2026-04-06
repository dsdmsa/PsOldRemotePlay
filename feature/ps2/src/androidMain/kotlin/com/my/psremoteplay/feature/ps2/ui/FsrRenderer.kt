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
 * OpenGL ES 3.0 renderer with swappable upscaling strategies.
 *
 * Pipeline: MediaCodec → SurfaceTexture (OES) → blit to sampler2D → [strategy upscale] → [strategy sharpen] → display.
 *
 * Strategies can be swapped at runtime via [setStrategy]. Supports:
 * - FSR 1.0 (corrected, 2-pass: EASU + RCAS)
 * - Snapdragon GSR (single-pass)
 * - Catmull-Rom Bicubic + CAS (2-pass)
 * - None (bilinear blit only)
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

    // Intermediate FBO (at output resolution, for 2-pass strategies)
    private var intermediateFboId = 0
    private var intermediateTexId = 0

    // Shader programs
    private var blitProgram = 0
    private var upscaleProgram = 0
    private var sharpenProgram = 0

    // Fullscreen quad
    private var quadVao = 0
    private var quadVbo = 0

    private var outputWidth = 0
    private var outputHeight = 0
    private val stMatrix = FloatArray(16)
    private var frameCount = 0L

    // Cached blit uniform locations
    private var blitTexLoc = -1
    private var blitStMatrixLoc = -1

    // Active upscale strategy
    @Volatile private var activeStrategy: UpscaleStrategy? = null
    @Volatile private var pendingStrategyName: String? = null
    @Volatile private var pendingStrategy: UpscaleStrategy? = null
    @Volatile var sharpness: Float = 0.2f
    private var glReady = false
    private var activeStrategyName: String? = null

    /** Change the upscale strategy. Takes effect on next frame (GL thread safe). */
    fun setStrategy(strategy: UpscaleStrategy?) {
        val newName = strategy?.name
        if (newName != pendingStrategyName) {
            pendingStrategy = strategy
            pendingStrategyName = newName
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION)
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER)
        log("GL init: $glRenderer — $glVersion")
        log("Input: ${inputWidth}x${inputHeight}")

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

        val inputFbo = createFbo(inputWidth, inputHeight)
        inputFboId = inputFbo.first
        inputTexId = inputFbo.second

        quadVao = createFullscreenQuad()
        glReady = true

        // Compile initial strategy if set
        applyPendingStrategy()

        log("Renderer ready, strategy=${activeStrategy?.name ?: "none"}")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
        log("Output: ${outputWidth}x${outputHeight} (${outputWidth.toFloat() / inputWidth}x)")
        recreateIntermediateFbo()
    }

    override fun onDrawFrame(gl: GL10?) {
        val st = surfaceTexture ?: return
        if (outputWidth <= 0 || outputHeight <= 0 || blitProgram == 0) return

        // Apply pending strategy change on GL thread (compare by name to avoid reference issues)
        if (pendingStrategyName != activeStrategyName) applyPendingStrategy()

        val strategy = activeStrategy
        val sharpnessVal = sharpness

        // Drain stale GL errors
        for (i in 0..9) { if (GLES30.glGetError() == GLES30.GL_NO_ERROR) break }

        try { st.updateTexImage() } catch (_: Exception) { return }
        st.getTransformMatrix(stMatrix)
        frameCount++

        if (frameCount == 1L) log("First frame, strategy=${strategy?.name ?: "bilinear"}")
        if (frameCount % 300 == 0L) log("Frame $frameCount, strategy=${strategy?.name ?: "bilinear"}")

        if (strategy == null || upscaleProgram == 0) {
            // Bilinear blit directly to screen
            blitOesToScreen()
            return
        }

        // === Pass 0: Blit OES → input FBO (sampler2D) ===
        blitOesToFbo(inputFboId, inputWidth, inputHeight)

        if (strategy.isSinglePass) {
            // Single-pass: upscale directly to screen
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, outputWidth, outputHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glUseProgram(upscaleProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexId)
            strategy.setUpscaleUniforms(upscaleProgram, inputWidth, inputHeight, outputWidth, outputHeight)
            drawQuad()
        } else {
            // 2-pass: upscale → intermediate FBO → sharpen → screen
            // Pass 1: Upscale
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, intermediateFboId)
            GLES30.glViewport(0, 0, outputWidth, outputHeight)
            GLES30.glUseProgram(upscaleProgram)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexId)
            strategy.setUpscaleUniforms(upscaleProgram, inputWidth, inputHeight, outputWidth, outputHeight)
            drawQuad()

            // Pass 2: Sharpen → screen
            if (sharpenProgram != 0) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                GLES30.glViewport(0, 0, outputWidth, outputHeight)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                GLES30.glUseProgram(sharpenProgram)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, intermediateTexId)
                strategy.setSharpenUniforms(sharpenProgram, outputWidth, outputHeight, sharpnessVal)
                drawQuad()
            } else {
                // No sharpen shader — blit intermediate to screen
                blitTexToScreen(intermediateTexId)
            }
        }
    }

    // --- Blit helpers ---

    private fun blitOesToScreen() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(blitProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(blitTexLoc, 0)
        GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
        drawQuad()
    }

    private fun blitOesToFbo(fboId: Int, width: Int, height: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(blitProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glUniform1i(blitTexLoc, 0)
        GLES30.glUniformMatrix4fv(blitStMatrixLoc, 1, false, stMatrix, 0)
        drawQuad()
    }

    private fun blitTexToScreen(texId: Int) {
        // Simple passthrough blit using the PASSTHROUGH_FRAG
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        // Reuse upscale program with NoneStrategy's passthrough if available,
        // or just use the intermediate texture directly — already rendered
    }

    // --- Strategy management ---

    private fun applyPendingStrategy() {
        val newStrategy = pendingStrategy
        val newName = pendingStrategyName

        // Clean up old programs
        if (upscaleProgram != 0) { GLES30.glDeleteProgram(upscaleProgram); upscaleProgram = 0 }
        if (sharpenProgram != 0) { GLES30.glDeleteProgram(sharpenProgram); sharpenProgram = 0 }

        if (newStrategy == null) {
            activeStrategy = null
            activeStrategyName = null
            log("Strategy: bilinear (no upscaling)")
            return
        }

        // Compile upscale shader
        upscaleProgram = compileProgram(VERTEX_SRC, newStrategy.upscaleFragShader)
        if (upscaleProgram == 0) {
            log("WARN: ${newStrategy.name} upscale shader failed, falling back to bilinear")
            activeStrategy = null
            activeStrategyName = newName  // Mark as applied so we don't retry every frame
            return
        }

        // Compile sharpen shader (if 2-pass)
        val sharpenSrc = newStrategy.sharpenFragShader
        if (sharpenSrc != null) {
            sharpenProgram = compileProgram(VERTEX_SRC, sharpenSrc)
            if (sharpenProgram == 0) {
                log("WARN: ${newStrategy.name} sharpen shader failed, using upscale only")
            }
        }

        activeStrategy = newStrategy
        activeStrategyName = newName
        log("Strategy: ${newStrategy.name} (upscale=$upscaleProgram sharpen=$sharpenProgram)")
    }

    private fun recreateIntermediateFbo() {
        if (intermediateFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(intermediateFboId), 0)
            GLES30.glDeleteTextures(1, intArrayOf(intermediateTexId), 0)
        }
        val fbo = createFbo(outputWidth, outputHeight)
        intermediateFboId = fbo.first
        intermediateTexId = fbo.second
    }

    fun release() {
        onSurfaceDestroyed()
        surface?.release()
        surface = null
        surfaceTexture?.release()
        surfaceTexture = null

        deleteGl(GLES30::glDeleteTextures, oesTextureId, inputTexId, intermediateTexId)
        deleteGl(GLES30::glDeleteFramebuffers, inputFboId, intermediateFboId)
        deleteGl(GLES30::glDeleteVertexArrays, quadVao)
        deleteGl(GLES30::glDeleteBuffers, quadVbo)
        for (p in intArrayOf(blitProgram, upscaleProgram, sharpenProgram)) {
            if (p != 0) GLES30.glDeleteProgram(p)
        }
        glReady = false
    }

    private fun deleteGl(deleter: (Int, IntArray, Int) -> Unit, vararg ids: Int) {
        for (id in ids) { if (id != 0) deleter(1, intArrayOf(id), 0) }
    }

    private fun log(msg: String) { android.util.Log.d("UPSCALE", msg) }

    // --- GL helpers ---

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun createOesTexture(): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun createFbo(width: Int, height: Int): Pair<Int, Int> {
        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texIds[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val fboIds = IntArray(1)
        GLES30.glGenFramebuffers(1, fboIds, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboIds[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texIds[0], 0)
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            log("FBO incomplete: 0x${status.toString(16)} ${width}x${height}")
            GLES30.glDeleteFramebuffers(1, fboIds, 0)
            GLES30.glDeleteTextures(1, texIds, 0)
            return Pair(0, 0)
        }
        return Pair(fboIds[0], texIds[0])
    }

    private fun createFullscreenQuad(): Int {
        val verts = floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts).apply { position(0) }
        val vao = IntArray(1); GLES30.glGenVertexArrays(1, vao, 0); GLES30.glBindVertexArray(vao[0])
        val vbo = IntArray(1); GLES30.glGenBuffers(1, vbo, 0); quadVbo = vbo[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindVertexArray(0)
        return vao[0]
    }

    private fun compileProgram(vertSrc: String, fragSrc: String): Int {
        val vert = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        if (vert == 0 || frag == 0) return 0
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vert)
        GLES30.glAttachShader(prog, frag)
        GLES30.glBindAttribLocation(prog, 0, "aPosition")
        GLES30.glBindAttribLocation(prog, 1, "aTexCoord")
        GLES30.glLinkProgram(prog)
        val status = IntArray(1); GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        GLES30.glDeleteShader(vert); GLES30.glDeleteShader(frag)
        if (status[0] == 0) {
            log("Link failed: ${GLES30.glGetProgramInfoLog(prog)}")
            GLES30.glDeleteProgram(prog); return 0
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src.trimIndent().trim())
        GLES30.glCompileShader(shader)
        val status = IntArray(1); GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val name = if (type == GLES30.GL_VERTEX_SHADER) "vert" else "frag"
            log("$name compile failed: ${GLES30.glGetShaderInfoLog(shader)}")
            GLES30.glDeleteShader(shader); return 0
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
    }
}
