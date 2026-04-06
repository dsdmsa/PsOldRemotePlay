package com.my.psremoteplay.feature.ps2.ui

import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.my.psremoteplay.feature.ps2.ui.upscale.SharpenMethod
import com.my.psremoteplay.feature.ps2.ui.upscale.UpscaleMethod

@Composable
fun GameStreamSurface(
    modifier: Modifier = Modifier,
    inputWidth: Int = 640,
    inputHeight: Int = 448,
    upscaleMethod: UpscaleMethod = UpscaleMethod.BILINEAR,
    sharpenMethod: SharpenMethod = SharpenMethod.NONE,
    sharpness: Float = 0.2f,
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onRendererReady: (FsrRenderer) -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnSurface = rememberUpdatedState(onSurfaceAvailable)
    val currentOnDestroyed = rememberUpdatedState(onSurfaceDestroyed)
    val currentOnRenderer = rememberUpdatedState(onRendererReady)

    val renderer = remember(inputWidth, inputHeight) {
        FsrRenderer(inputWidth, inputHeight,
            onSurfaceReady = { currentOnSurface.value(it) },
            onSurfaceDestroyed = { currentOnDestroyed.value() }
        ).also { currentOnRenderer.value(it) }
    }

    renderer.setUpscaleMethod(upscaleMethod)
    renderer.setSharpenMethod(sharpenMethod)
    renderer.sharpness = sharpness

    DisposableEffect(renderer) { onDispose { renderer.release() } }

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
