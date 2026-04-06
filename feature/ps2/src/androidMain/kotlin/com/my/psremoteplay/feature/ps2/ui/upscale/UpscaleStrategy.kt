package com.my.psremoteplay.feature.ps2.ui.upscale

/**
 * Strategy for GPU upscaling shaders. Each implementation provides its own
 * GLSL fragment shader(s) and knows how to set up uniforms.
 */
interface UpscaleStrategy {
    val name: String

    /** Whether this strategy needs an intermediate FBO (2-pass) or renders directly (1-pass) */
    val isSinglePass: Boolean get() = false

    /** Fragment shader for the upscale pass (reads from sampler2D at input resolution) */
    val upscaleFragShader: String

    /** Fragment shader for the sharpen pass (reads from sampler2D at output resolution). Null if single-pass. */
    val sharpenFragShader: String? get() = null

    /** Set uniforms for the upscale pass */
    fun setUpscaleUniforms(program: Int, inputWidth: Int, inputHeight: Int, outputWidth: Int, outputHeight: Int)

    /** Set uniforms for the sharpen pass. Only called if sharpenFragShader != null */
    fun setSharpenUniforms(program: Int, outputWidth: Int, outputHeight: Int, sharpness: Float) {}
}
