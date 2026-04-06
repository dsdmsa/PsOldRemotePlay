package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * No upscaling — simple bilinear passthrough.
 * Single-pass strategy that just samples the input texture directly.
 */
class NoneStrategy : UpscaleStrategy {

    override val name: String = "None (Bilinear)"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = PASSTHROUGH_FRAG.trimIndent().trim()

    override fun setUpscaleUniforms(
        program: Int,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int
    ) {
        val texLoc = GLES30.glGetUniformLocation(program, "uInputTex")
        GLES30.glUniform1i(texLoc, 0)
    }

    companion object {
        private const val PASSTHROUGH_FRAG = """
#version 300 es
precision mediump float;

uniform sampler2D uInputTex;

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(uInputTex, vTexCoord);
}
"""
    }
}
