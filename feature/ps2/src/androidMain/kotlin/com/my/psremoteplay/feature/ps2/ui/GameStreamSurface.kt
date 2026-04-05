package com.my.psremoteplay.feature.ps2.ui

import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * GLSurfaceView with FSR upscaling, wrapped for Compose.
 *
 * Pipeline: MediaCodec → SurfaceTexture (OES) → FSR EASU upscale → FSR RCAS sharpen → display.
 *
 * The FsrRenderer creates a Surface from SurfaceTexture and passes it to the caller
 * via [onSurfaceAvailable]. MediaCodec decodes directly to this Surface, then the
 * GL renderer applies FSR upscaling and sharpening before displaying.
 */
@Composable
fun GameStreamSurface(
    modifier: Modifier = Modifier,
    inputWidth: Int = 640,
    inputHeight: Int = 448,
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit
) {
    val context = LocalContext.current
    val currentOnSurfaceAvailable = rememberUpdatedState(onSurfaceAvailable)
    val currentOnSurfaceDestroyed = rememberUpdatedState(onSurfaceDestroyed)

    val renderer = remember(inputWidth, inputHeight) {
        FsrRenderer(
            inputWidth = inputWidth,
            inputHeight = inputHeight,
            onSurfaceReady = { surface -> currentOnSurfaceAvailable.value(surface) },
            onSurfaceDestroyed = { currentOnSurfaceDestroyed.value() }
        )
    }

    DisposableEffect(renderer) {
        onDispose { renderer.release() }
    }

    AndroidView(
        factory = {
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        modifier = modifier
    )
}
