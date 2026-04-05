package com.my.psremoteplay.feature.ps2.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * SurfaceView wrapped for Compose via AndroidView.
 *
 * The Surface is passed to MediaCodec for zero-copy video rendering.
 * The SurfaceView renders on a separate hardware layer below the Compose overlay,
 * so controller buttons and status text render on top without touching the video path.
 */
@Composable
fun GameStreamSurface(
    modifier: Modifier = Modifier,
    onSurfaceAvailable: (android.view.Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onSurfaceAvailable(holder.surface)
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        onSurfaceDestroyed()
                    }
                })
            }
        },
        modifier = modifier
    )
}
