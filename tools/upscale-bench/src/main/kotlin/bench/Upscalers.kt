package bench

import java.awt.image.BufferedImage
import kotlin.math.*

// ---------------------------------------------------------------------------
// Helper utilities
// ---------------------------------------------------------------------------

fun luma(r: Float, g: Float, b: Float): Float = 0.299f * r + 0.587f * g + 0.114f * b

fun clampCoord(v: Int, max: Int): Int = v.coerceIn(0, max - 1)

/** Read a pixel as [r, g, b] in 0..1 range, with coordinate clamping. */
fun getPixelF(img: BufferedImage, x: Int, y: Int): FloatArray {
    val cx = clampCoord(x, img.width)
    val cy = clampCoord(y, img.height)
    val rgb = img.getRGB(cx, cy)
    return floatArrayOf(
        ((rgb shr 16) and 0xFF) / 255f,
        ((rgb shr 8) and 0xFF) / 255f,
        (rgb and 0xFF) / 255f,
    )
}

/** Catmull-Rom basis weight for distance |t|. */
fun catmullRomWeight(t: Float): Float {
    val at = abs(t)
    return when {
        at < 1f -> 1.5f * at * at * at - 2.5f * at * at + 1f
        at < 2f -> -0.5f * at * at * at + 2.5f * at * at - 4f * at + 2f
        else -> 0f
    }
}

/** Bilinear sample at sub-pixel coordinates. Returns [r, g, b] in 0..1. */
fun sampleBilinear(img: BufferedImage, x: Float, y: Float): FloatArray {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val fx = x - x0
    val fy = y - y0

    val p00 = getPixelF(img, x0, y0)
    val p10 = getPixelF(img, x0 + 1, y0)
    val p01 = getPixelF(img, x0, y0 + 1)
    val p11 = getPixelF(img, x0 + 1, y0 + 1)

    return FloatArray(3) { c ->
        val top = p00[c] * (1f - fx) + p10[c] * fx
        val bot = p01[c] * (1f - fx) + p11[c] * fx
        top * (1f - fy) + bot * fy
    }
}

/** Pack [r,g,b] (0..1) into an ARGB int. */
private fun packRgb(rgb: FloatArray): Int {
    val r = (rgb[0].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val g = (rgb[1].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val b = (rgb[2].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

/** Create an output BufferedImage of the given size. */
private fun createOutput(w: Int, h: Int): BufferedImage =
    BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

/** Lanczos2 weight: sinc(t)*sinc(t/2) for |t|<2, else 0. */
private fun lanczos2(t: Float): Float {
    val at = abs(t)
    if (at < 1e-6f) return 1f
    if (at >= 2f) return 0f
    val pi_t = PI.toFloat() * at
    val sinc1 = sin(pi_t) / pi_t
    val sinc2 = sin(pi_t / 2f) / (pi_t / 2f)
    return sinc1 * sinc2
}

// ---------------------------------------------------------------------------
// 1. Bilinear
// ---------------------------------------------------------------------------

fun upscaleBilinear(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            out.setRGB(ox, oy, packRgb(sampleBilinear(input, ix, iy)))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 2. Bicubic (Catmull-Rom)
// ---------------------------------------------------------------------------

fun upscaleBicubic(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            val result = FloatArray(3)
            var wTotal = 0f

            for (dy in -1..2) {
                val wy = catmullRomWeight(fy - dy)
                for (dx in -1..2) {
                    val wx = catmullRomWeight(fx - dx)
                    val w = wx * wy
                    wTotal += w
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    for (c in 0..2) result[c] += p[c] * w
                }
            }
            if (wTotal > 0f) for (c in 0..2) result[c] /= wTotal
            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 3. FSR EASU (simplified)
// ---------------------------------------------------------------------------

fun upscaleFsrEasu(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH

    // 12-tap offsets (cross pattern used by EASU)
    val taps = arrayOf(
        -1 to -1, 0 to -1, 1 to -1,
        -1 to  0, 0 to  0, 1 to  0,
        -1 to  1, 0 to  1, 1 to  1,
        0 to -2, -2 to 0, 2 to 0,
    )

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Gather 12 samples
            val samples = Array(taps.size) { i ->
                getPixelF(input, ix0 + taps[i].first, iy0 + taps[i].second)
            }
            val lumas = FloatArray(taps.size) { i -> luma(samples[i][0], samples[i][1], samples[i][2]) }

            // Sobel gradients on center 3x3 (indices 0..8)
            val gx = (-lumas[0] + lumas[2] - 2f * lumas[3] + 2f * lumas[5] - lumas[6] + lumas[8])
            val gy = (-lumas[0] - 2f * lumas[1] - lumas[2] + lumas[6] + 2f * lumas[7] + lumas[8])

            // Structure tensor components
            val sxx = gx * gx
            val syy = gy * gy
            val sxy = gx * gy

            // Eigenvalue decomposition for dominant direction
            val trace = sxx + syy
            val det = sxx * syy - sxy * sxy
            val disc = sqrt(max(0f, trace * trace * 0.25f - det))
            val lambda1 = trace * 0.5f + disc
            val lambda2 = trace * 0.5f - disc

            // Edge direction angle
            val angle = if (abs(sxy) > 1e-6f) atan2(lambda1 - sxx, sxy) else 0f
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Coherence: how directional the edge is
            val coherence = if (lambda1 + lambda2 > 1e-6f) (lambda1 - lambda2) / (lambda1 + lambda2) else 0f

            // Compute directional Lanczos2 weights for each tap
            val weights = FloatArray(taps.size)
            var wSum = 0f
            for (i in taps.indices) {
                val dx = taps[i].first.toFloat() - fx
                val dy = taps[i].second.toFloat() - fy

                // Rotate into edge-aligned space
                val rx = dx * cosA + dy * sinA
                val ry = -dx * sinA + dy * cosA

                // Stretch perpendicular to edge based on coherence
                val stretch = 1f + coherence * 2f
                val dist = sqrt(rx * rx + (ry * stretch) * (ry * stretch))

                weights[i] = lanczos2(dist)
                wSum += weights[i]
            }

            // Weighted sum
            val result = FloatArray(3)
            if (wSum > 0f) {
                for (i in taps.indices) {
                    val w = weights[i] / wSum
                    for (c in 0..2) result[c] += samples[i][c] * w
                }
            } else {
                // Fallback to center pixel
                for (c in 0..2) result[c] = samples[4][c]
            }

            // Deringing: clamp to 2x2 min/max around center
            val minC = FloatArray(3) { Float.MAX_VALUE }
            val maxC = FloatArray(3) { Float.MIN_VALUE }
            for (i in intArrayOf(0, 1, 3, 4)) {
                for (c in 0..2) {
                    minC[c] = min(minC[c], samples[i][c])
                    maxC[c] = max(maxC[c], samples[i][c])
                }
            }
            for (c in 0..2) result[c] = result[c].coerceIn(minC[c], maxC[c])

            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 4. Matrix Filter
// ---------------------------------------------------------------------------

fun upscaleMatrixFilter(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Gather 3x3 luma neighborhood
            val lumaGrid = Array(3) { dy ->
                FloatArray(3) { dx ->
                    val p = getPixelF(input, ix0 + dx - 1, iy0 + dy - 1)
                    luma(p[0], p[1], p[2])
                }
            }

            // Sobel edge detection
            val gx = (-lumaGrid[0][0] + lumaGrid[0][2]
                    - 2f * lumaGrid[1][0] + 2f * lumaGrid[1][2]
                    - lumaGrid[2][0] + lumaGrid[2][2])
            val gy = (-lumaGrid[0][0] - 2f * lumaGrid[0][1] - lumaGrid[0][2]
                    + lumaGrid[2][0] + 2f * lumaGrid[2][1] + lumaGrid[2][2])

            val edgeMagnitude = sqrt(gx * gx + gy * gy)
            val edgeAngle = atan2(gy, gx)

            // Blend factor: smoothstep on edge magnitude
            val t = ((edgeMagnitude - 0.05f) / (0.3f - 0.05f)).coerceIn(0f, 1f)
            val blend = t * t * (3f - 2f * t)

            // Flat areas: standard bicubic
            val bicubicResult = FloatArray(3)
            var bw = 0f
            for (dy in -1..2) {
                val wy = catmullRomWeight(fy - dy)
                for (dx in -1..2) {
                    val wx = catmullRomWeight(fx - dx)
                    val w = wx * wy
                    bw += w
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    for (c in 0..2) bicubicResult[c] += p[c] * w
                }
            }
            if (bw > 0f) for (c in 0..2) bicubicResult[c] /= bw

            // Edges: 1D cubic interpolation along edge direction
            val edgeDirX = -sin(edgeAngle)
            val edgeDirY = cos(edgeAngle)
            val edgeResult = FloatArray(3)
            var ew = 0f
            for (i in -1..2) {
                val sampleX = ix0.toFloat() + fx + edgeDirX * i
                val sampleY = iy0.toFloat() + fy + edgeDirY * i
                val dist = abs(i.toFloat())
                val w = catmullRomWeight(dist - if (i < 0) fx * edgeDirX + fy * edgeDirY
                else -(fx * edgeDirX + fy * edgeDirY))
                val absW = abs(w) + 1e-6f
                ew += absW
                val p = sampleBilinear(input, sampleX, sampleY)
                for (c in 0..2) edgeResult[c] += p[c] * absW
            }
            if (ew > 0f) for (c in 0..2) edgeResult[c] /= ew

            // Blend
            val result = FloatArray(3) { c ->
                bicubicResult[c] * (1f - blend) + edgeResult[c] * blend
            }

            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 5. Feature-Guided
// ---------------------------------------------------------------------------

fun upscaleFeatureGuided(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH
    val bilateralSigma = 10f

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Structure tensor from 3x3 neighborhood
            var stxx = 0f; var styy = 0f; var stxy = 0f
            val centerP = getPixelF(input, ix0, iy0)
            val centerLuma = luma(centerP[0], centerP[1], centerP[2])

            for (dy in -1..1) {
                for (dx in -1..1) {
                    val pL = getPixelF(input, ix0 + dx - 1, iy0 + dy)
                    val lL = luma(pL[0], pL[1], pL[2])
                    val pR = getPixelF(input, ix0 + dx + 1, iy0 + dy)
                    val lR = luma(pR[0], pR[1], pR[2])
                    val pU = getPixelF(input, ix0 + dx, iy0 + dy - 1)
                    val lU = luma(pU[0], pU[1], pU[2])
                    val pD = getPixelF(input, ix0 + dx, iy0 + dy + 1)
                    val lD = luma(pD[0], pD[1], pD[2])
                    val gx = (lR - lL) * 0.5f
                    val gy = (lD - lU) * 0.5f
                    stxx += gx * gx
                    styy += gy * gy
                    stxy += gx * gy
                }
            }
            stxx /= 9f; styy /= 9f; stxy /= 9f

            // Eigenvalue decomposition
            val trace = stxx + styy
            val det = stxx * styy - stxy * stxy
            val disc = sqrt(max(0f, trace * trace * 0.25f - det))
            val lambda1 = trace * 0.5f + disc
            val edgeMag = sqrt(lambda1)

            // Bilateral-weighted Catmull-Rom
            val result = FloatArray(3)
            var wTotal = 0f
            for (dy in -1..2) {
                val wy = catmullRomWeight(fy - dy)
                for (dx in -1..2) {
                    val wx = catmullRomWeight(fx - dx)
                    val spatialW = wx * wy
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    val pLuma = luma(p[0], p[1], p[2])
                    val lumaDiff = pLuma - centerLuma
                    val rangeW = exp(-lumaDiff * lumaDiff * bilateralSigma)
                    val w = spatialW * rangeW
                    wTotal += w
                    for (c in 0..2) result[c] += p[c] * w
                }
            }
            if (wTotal > 0f) for (c in 0..2) result[c] /= wTotal

            // Detail extraction
            val lowPass = sampleBilinear(input, ix0 + fx, iy0 + fy)
            val detailWeight = (edgeMag * 0.5f).coerceIn(0f, 0.3f)
            for (c in 0..2) {
                val detail = centerP[c] - lowPass[c]
                result[c] += detail * detailWeight
            }

            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 6. RAISR-style
// ---------------------------------------------------------------------------

fun upscaleRaisr(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Compute gradient via central differences
            val pLL = getPixelF(input, ix0 - 1, iy0)
            val lL = luma(pLL[0], pLL[1], pLL[2])
            val pRR = getPixelF(input, ix0 + 1, iy0)
            val lR = luma(pRR[0], pRR[1], pRR[2])
            val pUU = getPixelF(input, ix0, iy0 - 1)
            val lU = luma(pUU[0], pUU[1], pUU[2])
            val pDD = getPixelF(input, ix0, iy0 + 1)
            val lD = luma(pDD[0], pDD[1], pDD[2])
            val gx = (lR - lL) * 0.5f
            val gy = (lD - lU) * 0.5f

            // Gradient angle quantized to 4 directions
            val angle = ((atan2(gy, gx) + PI.toFloat()) / PI.toFloat() * 4f).toInt() % 4

            // Gradient strength quantized to 3 levels
            val strength = sqrt(gx * gx + gy * gy)
            val strengthLevel = when {
                strength < 0.05f -> 0
                strength < 0.15f -> 1
                else -> 2
            }

            val edgeAngle = angle * PI.toFloat() / 4f
            val edgeDirX = cos(edgeAngle)
            val edgeDirY = sin(edgeAngle)

            val result = when (strengthLevel) {
                0 -> {
                    // Weak: isotropic bicubic
                    val r = FloatArray(3)
                    var w = 0f
                    for (dy in -1..2) {
                        val wy = catmullRomWeight(fy - dy)
                        for (dx in -1..2) {
                            val wx = catmullRomWeight(fx - dx)
                            val wt = wx * wy
                            w += wt
                            val p = getPixelF(input, ix0 + dx, iy0 + dy)
                            for (c in 0..2) r[c] += p[c] * wt
                        }
                    }
                    if (w > 0f) for (c in 0..2) r[c] /= w
                    r
                }
                1 -> {
                    // Medium: stretched bicubic along edge
                    val r = FloatArray(3)
                    var w = 0f
                    val stretch = 1.5f
                    for (dy in -1..2) {
                        for (dx in -1..2) {
                            val ddx = dx.toFloat() - fx
                            val ddy = dy.toFloat() - fy
                            val along = ddx * edgeDirX + ddy * edgeDirY
                            val perp = -ddx * edgeDirY + ddy * edgeDirX
                            val wx = catmullRomWeight(along)
                            val wy = catmullRomWeight(perp * stretch)
                            val wt = wx * wy
                            w += wt
                            val p = getPixelF(input, ix0 + dx, iy0 + dy)
                            for (c in 0..2) r[c] += p[c] * wt
                        }
                    }
                    if (w > 0f) for (c in 0..2) r[c] /= w
                    r
                }
                else -> {
                    // Strong: 1D cubic along edge direction
                    val r = FloatArray(3)
                    var w = 0f
                    for (i in -1..2) {
                        val sampleX = ix0.toFloat() + fx + edgeDirX * i
                        val sampleY = iy0.toFloat() + fy + edgeDirY * i
                        val proj = fx * edgeDirX + fy * edgeDirY
                        val wt = catmullRomWeight(i.toFloat() - proj)
                        val absW = abs(wt) + 1e-6f
                        w += absW
                        val p = sampleBilinear(input, sampleX, sampleY)
                        for (c in 0..2) r[c] += p[c] * absW
                    }
                    if (w > 0f) for (c in 0..2) r[c] /= w
                    r
                }
            }

            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 7. Deblock
// ---------------------------------------------------------------------------

fun upscaleDeblock(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH
    val bilateralSigma = 8f
    val bilateralRadius = 2

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Detect proximity to 4px or 8px block boundaries in input space
            val nearBoundary4 = (ix0 % 4 == 0 && fx < 0.5f) || ((ix0 + 1) % 4 == 0 && fx > 0.5f) ||
                    (iy0 % 4 == 0 && fy < 0.5f) || ((iy0 + 1) % 4 == 0 && fy > 0.5f)
            val nearBoundary8 = (ix0 % 8 == 0 && fx < 0.5f) || ((ix0 + 1) % 8 == 0 && fx > 0.5f) ||
                    (iy0 % 8 == 0 && fy < 0.5f) || ((iy0 + 1) % 8 == 0 && fy > 0.5f)

            if (nearBoundary4 || nearBoundary8) {
                // Bilateral smoothing around boundary
                val centerP = getPixelF(input, ix0, iy0)
                val centerLuma = luma(centerP[0], centerP[1], centerP[2])
                val result = FloatArray(3)
                var wTotal = 0f

                for (dy in -bilateralRadius..bilateralRadius) {
                    for (dx in -bilateralRadius..bilateralRadius) {
                        val p = getPixelF(input, ix0 + dx, iy0 + dy)
                        val pLuma = luma(p[0], p[1], p[2])
                        val lumaDiff = pLuma - centerLuma
                        val spatialDist = sqrt((dx * dx + dy * dy).toFloat())
                        val spatialW = exp(-spatialDist * spatialDist / 4f)
                        val rangeW = exp(-lumaDiff * lumaDiff * bilateralSigma)
                        val w = spatialW * rangeW
                        wTotal += w
                        for (c in 0..2) result[c] += p[c] * w
                    }
                }
                if (wTotal > 0f) for (c in 0..2) result[c] /= wTotal
                out.setRGB(ox, oy, packRgb(result))
            } else {
                // Standard Catmull-Rom bicubic
                val result = FloatArray(3)
                var wTotal = 0f
                for (dy in -1..2) {
                    val wy = catmullRomWeight(fy - dy)
                    for (dx in -1..2) {
                        val wx = catmullRomWeight(fx - dx)
                        val w = wx * wy
                        wTotal += w
                        val p = getPixelF(input, ix0 + dx, iy0 + dy)
                        for (c in 0..2) result[c] += p[c] * w
                    }
                }
                if (wTotal > 0f) for (c in 0..2) result[c] /= wTotal
                out.setRGB(ox, oy, packRgb(result))
            }
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// 8. Custom Bilateral Bicubic
// ---------------------------------------------------------------------------

fun upscaleCustomBilateral(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH
    val sigma = 12f

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            val centerP = getPixelF(input, ix0, iy0)
            val centerLuma = luma(centerP[0], centerP[1], centerP[2])

            val result = FloatArray(3)
            var wTotal = 0f

            for (dy in -1..2) {
                val wy = catmullRomWeight(fy - dy)
                for (dx in -1..2) {
                    val wx = catmullRomWeight(fx - dx)
                    val spatialW = wx * wy
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    val pLuma = luma(p[0], p[1], p[2])
                    val lumaDiff = pLuma - centerLuma
                    val rangeW = exp(-lumaDiff * lumaDiff * sigma)
                    val w = spatialW * rangeW
                    wTotal += w
                    for (c in 0..2) result[c] += p[c] * w
                }
            }
            if (wTotal > 0f) for (c in 0..2) result[c] /= wTotal

            out.setRGB(ox, oy, packRgb(result))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Registry
// ---------------------------------------------------------------------------

val ALL_UPSCALERS: Map<String, (BufferedImage, Int, Int) -> BufferedImage> = mapOf(
    "Bilinear" to ::upscaleBilinear,
    "Bicubic" to ::upscaleBicubic,
    "FSR-EASU" to ::upscaleFsrEasu,
    "MatrixFilter" to ::upscaleMatrixFilter,
    "FeatureGuided" to ::upscaleFeatureGuided,
    "RAISR" to ::upscaleRaisr,
    "Deblock" to ::upscaleDeblock,
    "CustomBilateral" to ::upscaleCustomBilateral,
)
