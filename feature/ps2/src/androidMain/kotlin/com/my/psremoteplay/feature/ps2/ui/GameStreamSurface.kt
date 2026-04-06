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
import com.my.psremoteplay.feature.ps2.ui.upscale.UpscaleStrategy

/**
 * GLSurfaceView with swappable upscaling strategies, wrapped for Compose.
 *
 * Pipeline: MediaCodec → SurfaceTexture (OES) → [strategy upscale] → [strategy sharpen] → display.
 */
@Composable
fun GameStreamSurface(
    modifier: Modifier = Modifier,
    inputWidth: Int = 640,
    inputHeight: Int = 448,
    upscaleStrategy: UpscaleStrategy? = null,
    sharpness: Float = 0.2f,
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

    // Update strategy and sharpness on recomposition (thread-safe via volatile)
    renderer.setStrategy(upscaleStrategy)
    renderer.sharpness = sharpness

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
