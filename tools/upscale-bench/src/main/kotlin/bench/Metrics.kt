package bench

import java.awt.image.BufferedImage
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Collection of image quality metrics used to evaluate upscaler output
 * against a ground-truth reference image.
 */

data class ImageMetrics(
    /** Peak Signal-to-Noise Ratio in dB. Higher is better. */
    val psnr: Double,
    /** Structural Similarity Index, 0.0-1.0. Higher is better. */
    val ssim: Double,
    /** Edge preservation score (Sobel correlation), 0.0-1.0. Higher is better. */
    val edgeScore: Double,
    /** Root Mean Squared Error on luminance channel. Lower is better. */
    val lumaRmse: Double,
)

/**
 * Computes all quality metrics in one call. If [test] has different dimensions
 * than [reference], it is resized to match before comparison.
 */
fun computeAllMetrics(reference: BufferedImage, test: BufferedImage): ImageMetrics {
    val aligned = alignToReference(reference, test)
    return ImageMetrics(
        psnr = computePSNR(reference, aligned),
        ssim = computeSSIM(reference, aligned),
        edgeScore = computeEdgeScore(reference, aligned),
        lumaRmse = computeLumaRMSE(reference, aligned),
    )
}

// ---------------------------------------------------------------------------
// PSNR
// ---------------------------------------------------------------------------

/**
 * Peak Signal-to-Noise Ratio in dB.
 *
 * Formula: `10 * log10(MAX^2 / MSE)` where MAX = 1.0 and MSE is the mean
 * squared error across all pixels and all three RGB channels.
 *
 * Returns [Double.POSITIVE_INFINITY] when images are identical (MSE = 0).
 */
fun computePSNR(reference: BufferedImage, test: BufferedImage): Double {
    val t = alignToReference(reference, test)
    val w = reference.width
    val h = reference.height
    var sumSqErr = 0.0

    for (y in 0 until h) {
        for (x in 0 until w) {
            val refPx = getPixel(reference, x, y)
            val tstPx = getPixel(t, x, y)
            for (c in 0..2) {
                val diff = (refPx[c] - tstPx[c]).toDouble()
                sumSqErr += diff * diff
            }
        }
    }

    val mse = sumSqErr / (w * h * 3)
    if (mse == 0.0) return Double.POSITIVE_INFINITY
    return 10.0 * log10(1.0 / mse)
}

// ---------------------------------------------------------------------------
// SSIM
// ---------------------------------------------------------------------------

/**
 * Structural Similarity Index using an 11x11 Gaussian window with sigma = 1.5.
 *
 * Luminance is computed as `0.299R + 0.587G + 0.114B` (BT.601).
 * Constants: C1 = (0.01)^2 = 1e-4, C2 = (0.03)^2 = 9e-4.
 *
 * The per-window SSIM values are averaged to produce the final score.
 */
fun computeSSIM(reference: BufferedImage, test: BufferedImage): Double {
    val t = alignToReference(reference, test)
    val w = reference.width
    val h = reference.height

    val refLuma = lumaMap(reference)
    val tstLuma = lumaMap(t)

    val kernel = gaussianKernel(size = 11, sigma = 1.5)
    val halfK = 11 / 2

    val c1 = 1e-4   // (0.01 * L)^2, L = 1.0
    val c2 = 9e-4   // (0.03 * L)^2

    var ssimSum = 0.0
    var windowCount = 0

    for (y in halfK until h - halfK) {
        for (x in halfK until w - halfK) {
            var muX = 0.0
            var muY = 0.0
            var sigmaXX = 0.0
            var sigmaYY = 0.0
            var sigmaXY = 0.0

            for (ky in -halfK..halfK) {
                for (kx in -halfK..halfK) {
                    val weight = kernel[ky + halfK][kx + halfK]
                    val xVal = refLuma[y + ky][x + kx]
                    val yVal = tstLuma[y + ky][x + kx]
                    muX += weight * xVal
                    muY += weight * yVal
                }
            }

            for (ky in -halfK..halfK) {
                for (kx in -halfK..halfK) {
                    val weight = kernel[ky + halfK][kx + halfK]
                    val xVal = refLuma[y + ky][x + kx]
                    val yVal = tstLuma[y + ky][x + kx]
                    sigmaXX += weight * (xVal - muX) * (xVal - muX)
                    sigmaYY += weight * (yVal - muY) * (yVal - muY)
                    sigmaXY += weight * (xVal - muX) * (yVal - muY)
                }
            }

            val numerator = (2.0 * muX * muY + c1) * (2.0 * sigmaXY + c2)
            val denominator = (muX * muX + muY * muY + c1) * (sigmaXX + sigmaYY + c2)
            ssimSum += numerator / denominator
            windowCount++
        }
    }

    return if (windowCount == 0) 1.0 else ssimSum / windowCount
}

// ---------------------------------------------------------------------------
// Edge preservation score
// ---------------------------------------------------------------------------

/**
 * Edge preservation score: applies a Sobel operator to both images (on
 * luminance), then computes the Pearson correlation coefficient between the
 * two edge-magnitude maps.
 *
 * Returns a value in 0.0-1.0 (clamped; negative correlation is mapped to 0).
 */
fun computeEdgeScore(reference: BufferedImage, test: BufferedImage): Double {
    val t = alignToReference(reference, test)
    val refEdge = sobelMagnitudeMap(lumaMap(reference))
    val tstEdge = sobelMagnitudeMap(lumaMap(t))

    val h = refEdge.size
    val w = refEdge[0].size
    val n = h * w
    if (n == 0) return 1.0

    var sumA = 0.0
    var sumB = 0.0
    for (y in 0 until h) {
        for (x in 0 until w) {
            sumA += refEdge[y][x]
            sumB += tstEdge[y][x]
        }
    }
    val meanA = sumA / n
    val meanB = sumB / n

    var cov = 0.0
    var varA = 0.0
    var varB = 0.0
    for (y in 0 until h) {
        for (x in 0 until w) {
            val da = refEdge[y][x] - meanA
            val db = tstEdge[y][x] - meanB
            cov += da * db
            varA += da * da
            varB += db * db
        }
    }

    val denom = sqrt(varA * varB)
    if (denom == 0.0) return 1.0
    return (cov / denom).coerceIn(0.0, 1.0)
}

// ---------------------------------------------------------------------------
// Luma RMSE
// ---------------------------------------------------------------------------

/**
 * Root Mean Squared Error on the luminance channel (BT.601 weights).
 */
fun computeLumaRMSE(reference: BufferedImage, test: BufferedImage): Double {
    val t = alignToReference(reference, test)
    val w = reference.width
    val h = reference.height
    var sumSqErr = 0.0

    for (y in 0 until h) {
        for (x in 0 until w) {
            val refPx = getPixel(reference, x, y)
            val tstPx = getPixel(t, x, y)
            val refY = 0.299 * refPx[0] + 0.587 * refPx[1] + 0.114 * refPx[2]
            val tstY = 0.299 * tstPx[0] + 0.587 * tstPx[1] + 0.114 * tstPx[2]
            val diff = refY - tstY
            sumSqErr += diff * diff
        }
    }

    return sqrt(sumSqErr / (w * h))
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * If [test] dimensions differ from [reference], resize [test] to match.
 * Returns the original [test] when sizes already match.
 */
private fun alignToReference(reference: BufferedImage, test: BufferedImage): BufferedImage {
    return if (test.width == reference.width && test.height == reference.height) {
        test
    } else {
        downscale(test, reference.width, reference.height)
    }
}

/** Converts an image to a 2D luminance map (BT.601) with values in 0.0-1.0. */
private fun lumaMap(img: BufferedImage): Array<DoubleArray> {
    val w = img.width
    val h = img.height
    return Array(h) { y ->
        DoubleArray(w) { x ->
            val px = getPixel(img, x, y)
            0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]
        }
    }
}

/**
 * Creates a normalised 2D Gaussian kernel.
 */
private fun gaussianKernel(size: Int, sigma: Double): Array<DoubleArray> {
    val half = size / 2
    val kernel = Array(size) { DoubleArray(size) }
    var sum = 0.0
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = (x - half).toDouble()
            val dy = (y - half).toDouble()
            val value = exp(-(dx * dx + dy * dy) / (2.0 * sigma * sigma))
            kernel[y][x] = value
            sum += value
        }
    }
    for (y in 0 until size) {
        for (x in 0 until size) {
            kernel[y][x] /= sum
        }
    }
    return kernel
}

/**
 * Computes the Sobel edge-magnitude map from a luminance map.
 * The output is 2 pixels smaller in each dimension (border pixels are skipped).
 */
private fun sobelMagnitudeMap(luma: Array<DoubleArray>): Array<DoubleArray> {
    val h = luma.size
    val w = if (h > 0) luma[0].size else 0
    if (h < 3 || w < 3) return arrayOf()

    val outH = h - 2
    val outW = w - 2
    return Array(outH) { y ->
        DoubleArray(outW) { x ->
            val oy = y + 1
            val ox = x + 1
            val gx = -luma[oy - 1][ox - 1] + luma[oy - 1][ox + 1] +
                -2.0 * luma[oy][ox - 1] + 2.0 * luma[oy][ox + 1] +
                -luma[oy + 1][ox - 1] + luma[oy + 1][ox + 1]
            val gy = -luma[oy - 1][ox - 1] - 2.0 * luma[oy - 1][ox] - luma[oy - 1][ox + 1] +
                luma[oy + 1][ox - 1] + 2.0 * luma[oy + 1][ox] + luma[oy + 1][ox + 1]
            sqrt(gx * gx + gy * gy)
        }
    }
}
