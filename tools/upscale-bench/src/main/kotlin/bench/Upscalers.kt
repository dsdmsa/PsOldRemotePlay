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
// 9. Optimal (Adaptive Bicubic + Block Boundary Artifact Suppression)
// ---------------------------------------------------------------------------

/**
 * Bicubic Catmull-Rom with selective bilateral smoothing at H.264 block boundaries.
 *
 * Most pixels get pure bicubic (the proven winner). Near 8px/4px block boundaries,
 * if a luma discontinuity looks like a compression artifact (abrupt step without
 * surrounding gradient support), a lightweight bilateral correction is blended in.
 *
 * This can only improve on bicubic (smooths artifact seams) and never hurt it
 * (real edges near grid lines are protected by bilateral range weighting).
 */
fun upscaleOptimal(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH
    val bilateralSigma = 15f

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // --- Standard Catmull-Rom bicubic ---
            val bicubic = FloatArray(3)
            var bw = 0f
            for (dy in -1..2) {
                val wy = catmullRomWeight(fy - dy)
                for (dx in -1..2) {
                    val wx = catmullRomWeight(fx - dx)
                    val w = wx * wy
                    bw += w
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    for (c in 0..2) bicubic[c] += p[c] * w
                }
            }
            if (bw > 0f) for (c in 0..2) bicubic[c] /= bw

            // --- Block boundary detection ---
            val inputPxX = ix0 + fx
            val inputPxY = iy0 + fy

            // Distance to nearest 8px and 4px grid lines (0 = on boundary)
            val dist8x = abs((inputPxX % 8f + 8f) % 8f - 4f)  // 0..4, min at boundary
            val dist8y = abs((inputPxY % 8f + 8f) % 8f - 4f)
            val dist4x = abs((inputPxX % 4f + 4f) % 4f - 2f)  // 0..2, min at boundary
            val dist4y = abs((inputPxY % 4f + 4f) % 4f - 2f)

            val nearBound8 = min(dist8x, dist8y)
            val nearBound4 = min(dist4x, dist4y)

            // Boundary proximity weights
            fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
                val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }

            val boundWeight8 = 1f - smoothstep(0f, 1.5f, nearBound8)
            val boundWeight4 = 1f - smoothstep(0f, 1.0f, nearBound4)
            val boundWeight = max(boundWeight8, boundWeight4 * 0.5f)

            if (boundWeight > 0.01f) {
                // Sample 4 cardinal neighbors
                val tN = getPixelF(input, ix0, iy0 - 1)
                val tS = getPixelF(input, ix0, iy0 + 1)
                val tW = getPixelF(input, ix0 - 1, iy0)
                val tE = getPixelF(input, ix0 + 1, iy0)

                val lC = luma(bicubic[0], bicubic[1], bicubic[2])
                val lN = luma(tN[0], tN[1], tN[2])
                val lS = luma(tS[0], tS[1], tS[2])
                val lW = luma(tW[0], tW[1], tW[2])
                val lE = luma(tE[0], tE[1], tE[2])

                // Max cross-boundary step
                val maxStep = max(max(abs(lC - lN), abs(lC - lS)),
                                  max(abs(lC - lW), abs(lC - lE)))

                // Surrounding variance (do neighbors agree with each other?)
                val neighborVar = (abs(lN - lS) + abs(lW - lE)) * 0.5f

                // Artifact signature: high step + low variance = compression artifact
                val artifactness = maxStep * (1f - smoothstep(0f, maxStep * 0.8f + 0.01f, neighborVar))
                val suppressStrength = smoothstep(0.02f, 0.12f, artifactness) * boundWeight * 0.6f

                if (suppressStrength > 0.01f) {
                    // Bilateral weighted average of 3x3 neighborhood
                    val tNW = getPixelF(input, ix0 - 1, iy0 - 1)
                    val tNE = getPixelF(input, ix0 + 1, iy0 - 1)
                    val tSW = getPixelF(input, ix0 - 1, iy0 + 1)
                    val tSE = getPixelF(input, ix0 + 1, iy0 + 1)

                    val lNW = luma(tNW[0], tNW[1], tNW[2])
                    val lNE = luma(tNE[0], tNE[1], tNE[2])
                    val lSW = luma(tSW[0], tSW[1], tSW[2])
                    val lSE = luma(tSE[0], tSE[1], tSE[2])

                    val wC = 1f
                    val wN = exp(-(lN - lC) * (lN - lC) * bilateralSigma)
                    val wS = exp(-(lS - lC) * (lS - lC) * bilateralSigma)
                    val wW = exp(-(lW - lC) * (lW - lC) * bilateralSigma)
                    val wE = exp(-(lE - lC) * (lE - lC) * bilateralSigma)
                    val wNW = exp(-(lNW - lC) * (lNW - lC) * bilateralSigma) * 0.707f
                    val wNE = exp(-(lNE - lC) * (lNE - lC) * bilateralSigma) * 0.707f
                    val wSW = exp(-(lSW - lC) * (lSW - lC) * bilateralSigma) * 0.707f
                    val wSE = exp(-(lSE - lC) * (lSE - lC) * bilateralSigma) * 0.707f

                    val wTotal = wC + wN + wS + wW + wE + wNW + wNE + wSW + wSE
                    val smoothed = FloatArray(3) { c ->
                        (bicubic[c] * wC + tN[c] * wN + tS[c] * wS + tW[c] * wW + tE[c] * wE
                         + tNW[c] * wNW + tNE[c] * wNE + tSW[c] * wSW + tSE[c] * wSE) / wTotal
                    }

                    // Blend: mostly bicubic, with controlled artifact smoothing
                    for (c in 0..2) {
                        bicubic[c] = bicubic[c] * (1f - suppressStrength) + smoothed[c] * suppressStrength
                    }
                }
            }

            out.setRGB(ox, oy, packRgb(bicubic))
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
    "Optimal" to ::upscaleOptimal,
    "Fixed3x" to ::upscaleFixed3x,
    "BicubicBilateral" to ::upscaleBicubicBilateral,
    "BicubicClamp" to ::upscaleBicubicClamp,
    "LumaGuided" to ::upscaleLumaGuided,
    "DualKernel" to ::upscaleDualKernel,
    "SSIMOptimized" to ::upscaleSSIMOptimized,
    "TinyNN" to ::upscaleTinyNN,
    "TinyNNv2" to ::upscaleTinyNNv2,
)

// v2 trained weights: 17 -> 32 (PReLU) -> 1
private val NN2_W1 = arrayOf(
    floatArrayOf(-0.07594684f, 0.07626595f, 0.08301702f, 0.16279778f, 0.55041552f, -0.08384617f, -1.02428377f, -0.21316607f, -0.30617255f, -0.87117511f, 0.32489854f, -0.42195767f, -0.32253233f, 0.29536030f, 1.51803434f, -0.15211511f, 0.31028756f),
    floatArrayOf(-0.08214081f, 0.16868961f, -0.42958802f, -0.52204376f, -0.36085600f, 0.11910978f, -0.40368447f, -0.41609293f, 0.27750540f, -0.10265231f, 0.45739093f, -0.25095797f, 0.10055041f, 0.81493360f, -0.47352800f, 0.11844184f, -0.03896111f),
    floatArrayOf(-0.12931222f, 0.48202926f, 0.22977844f, 0.02002582f, 0.32356173f, -0.17825772f, -0.68179369f, 0.45013696f, -0.37680051f, -0.23868492f, 0.00717530f, -0.34524551f, 0.12403469f, 0.44156015f, -0.01291365f, -0.21183106f, 0.00243893f),
    floatArrayOf(0.53241110f, -0.02095328f, 0.17765921f, -0.20551305f, -0.12670898f, 0.69296837f, -0.63354599f, -0.35665825f, -0.02832796f, -0.64472598f, -0.08558945f, 0.23781049f, 0.37471113f, 0.12407303f, 0.67950225f, -0.33367723f, 0.05525667f),
    floatArrayOf(-0.49473226f, 0.63763160f, 0.11450190f, 0.42052352f, 0.19992445f, 0.01084714f, -0.38181606f, -0.45626256f, -0.16702473f, -0.28644288f, 0.09854432f, 0.10543602f, 0.21600090f, 0.14397420f, -0.74749386f, 0.00313472f, -0.90437615f),
    floatArrayOf(-0.10721792f, 0.21007808f, 0.11179554f, -0.08274584f, -0.19908999f, -0.23251699f, 0.62377077f, 0.33350790f, -0.05291760f, 0.07849650f, -0.01561016f, -0.36230353f, -0.32137063f, 0.02894279f, -0.56182379f, -0.38304490f, 0.17859802f),
    floatArrayOf(0.24960600f, -0.20140159f, 0.20551172f, -0.26364797f, 0.51930964f, -0.02855723f, 0.06135403f, 0.02009043f, -0.69277173f, -0.08288021f, 0.32085237f, -0.45160243f, -0.21862917f, 0.00771240f, -0.75131941f, -0.15998116f, -0.11768488f),
    floatArrayOf(-0.23866457f, 0.55545890f, -0.09012435f, -0.12411235f, 0.30673894f, -0.16127771f, -0.15761770f, 0.93019211f, -0.01695478f, 0.02821859f, -0.05724021f, -0.20124483f, -0.28720233f, -0.54274964f, -0.61912066f, 0.12299103f, 0.07157333f),
    floatArrayOf(0.04568081f, -0.10157495f, 0.43141934f, 0.03278437f, -0.76393503f, 0.52096552f, -0.26181322f, -0.30754155f, 0.63782775f, -0.21537335f, -0.41035047f, 0.54017425f, -0.16056333f, 0.07820616f, -0.02929139f, 0.07867445f, 0.07921333f),
    floatArrayOf(-0.26970384f, 0.54455370f, 0.00561322f, 0.07116960f, 0.35469809f, -0.09423842f, -0.40156570f, -0.32487547f, 0.32463381f, -0.43229163f, 0.18148383f, 0.07143535f, -0.03387812f, -0.42290068f, 0.71731466f, 0.00000818f, -0.09626732f),
    floatArrayOf(0.39508089f, 0.14590140f, -0.18160103f, 0.17013469f, -0.55643415f, 0.34424508f, -0.00649961f, -0.46116111f, -0.25484717f, -0.37074333f, -0.09997012f, -0.83336413f, 0.07416948f, -0.21970412f, -0.13877159f, -0.12407248f, 0.33845985f),
    floatArrayOf(-1.07370269f, -0.19328758f, -0.48478305f, 0.57973915f, 0.34132570f, 0.33224830f, 0.13578647f, 0.24106784f, 0.10053262f, -0.13247952f, -0.63626981f, 0.21591215f, -0.03239843f, -0.11914501f, 2.53496337f, -0.32165235f, 0.12317625f),
    floatArrayOf(0.02644250f, -0.64615721f, -0.19638970f, -0.30656394f, -0.51499683f, -0.22366799f, 0.36003932f, 0.32641843f, 0.19413868f, -0.33516428f, -0.05464863f, -0.23544182f, 0.33732948f, -0.73739707f, -0.10006277f, -0.15922740f, 0.13674870f),
    floatArrayOf(-0.12126449f, 0.79409242f, 0.20389511f, -0.29837665f, -0.02097108f, -0.38074297f, 0.16445868f, -0.60476488f, -0.01412795f, 0.62114179f, 0.23120834f, -0.16953845f, -0.39018577f, -0.18643406f, 1.08465445f, -0.00534683f, -0.08034737f),
    floatArrayOf(0.24220023f, -0.21056907f, 0.12796326f, -0.06531160f, -0.10945502f, 0.00407417f, 0.49446046f, 0.09921362f, -1.02836204f, 0.15399878f, -0.41484231f, 0.13725962f, 0.29966232f, -0.84585226f, -0.23581685f, -0.33138332f, -0.29485580f),
    floatArrayOf(-0.53596920f, 0.32517350f, -0.78466928f, 0.27014703f, 1.04836357f, 0.45409119f, -0.59825963f, 0.43685511f, -0.57279915f, 0.20828126f, -0.08276001f, 0.00224097f, -0.16822986f, 0.16667904f, -0.33164212f, 0.00377953f, 0.01126296f),
    floatArrayOf(-0.06415099f, 0.59264922f, -0.10888223f, 0.11529579f, 0.08928192f, 0.06920827f, -0.37967175f, -0.29015702f, -0.25079024f, -0.55222321f, 0.84496844f, -0.01552753f, -0.16062924f, 0.18775040f, -0.18017367f, -0.01400502f, -0.63860554f),
    floatArrayOf(-0.27060258f, 0.20130700f, -0.09284683f, -0.57875121f, -0.15919289f, -0.05777871f, 0.26950005f, -0.02959779f, 0.35973987f, 0.27135891f, 0.19143113f, -0.01334481f, -0.11502211f, 0.10786016f, 0.27248153f, 0.00503650f, -0.37868023f),
    floatArrayOf(0.57227707f, 0.08815625f, -0.03275141f, -0.65544498f, -0.29841170f, 0.06651454f, -0.19791722f, -0.03146725f, -0.69230258f, 0.55468857f, -0.45750645f, -0.02985728f, -0.05442322f, -0.18416028f, 0.73028260f, 0.39092594f, -0.04658653f),
    floatArrayOf(0.00366380f, 0.16509686f, -0.10487658f, -0.20018885f, 0.63572013f, 0.34003782f, -0.34702066f, -0.54578286f, -0.18536307f, 0.09687588f, -0.65255857f, 0.77579492f, -0.16828959f, 0.00423148f, -0.19491971f, 0.33852148f, 0.19595464f),
    floatArrayOf(-0.22438356f, -0.10630741f, 0.01776039f, -0.17499647f, 0.19986665f, -0.29489195f, 0.00698258f, 0.33267915f, 0.13693523f, 0.29786712f, -0.56545043f, 0.18580642f, 0.18713868f, 0.13486445f, -0.74619865f, -0.00839109f, -0.62703782f),
    floatArrayOf(0.38518292f, -0.02706418f, -0.55003953f, 0.31549078f, 0.12410324f, -0.09475128f, 0.09271191f, -0.34585449f, -0.34536058f, 0.15360418f, -0.10382079f, 0.63015532f, -0.29795578f, -0.33295658f, -1.50438547f, -0.18871082f, 0.04728692f),
    floatArrayOf(0.08822595f, -0.74647343f, -0.21771646f, -0.20886166f, 1.00873804f, 0.13494429f, -0.50973582f, 0.46871433f, 0.14896742f, 0.39813417f, -0.10601351f, -0.03209623f, -0.08304449f, -0.02009753f, 0.13771202f, -0.11814111f, 0.00022508f),
    floatArrayOf(-0.23157877f, -0.03974226f, -0.05526459f, -0.07356630f, 0.03798184f, 0.31954721f, -0.27429372f, -0.13694881f, 0.16734783f, 0.01441543f, 0.28789654f, 0.17042811f, -0.47189933f, 0.27210954f, 0.69003958f, 0.24016751f, -0.18313047f),
    floatArrayOf(0.21889944f, -0.02923098f, 0.38865551f, -0.37630135f, -0.31767282f, -0.57086772f, 0.30987537f, -0.61890817f, 0.06817079f, -0.34918293f, 0.04590745f, 0.71523935f, 0.49992681f, 0.45516655f, 0.00862371f, -0.03927956f, -0.00228864f),
    floatArrayOf(-0.22646227f, -0.18095762f, 0.48275447f, -0.46008483f, -0.16007960f, 0.40993443f, -0.16090178f, 0.43532223f, -0.38929859f, 0.52234864f, -0.20837623f, -0.30351910f, 0.24873337f, -0.42407268f, 0.48857254f, -0.26680711f, -0.08308366f),
    floatArrayOf(0.29820698f, -0.44365686f, -0.09447069f, -0.25883010f, 1.11261284f, -0.03171086f, -0.18980256f, 0.10229345f, -0.15864135f, -0.64916223f, 0.26648092f, -0.45775279f, 0.11312359f, -0.31279156f, -0.14106531f, -0.17103209f, 0.04472712f),
    floatArrayOf(0.39617187f, -0.28479573f, -0.62477773f, -0.06703609f, 0.13655373f, 0.49119610f, 0.27038950f, -0.45226526f, 0.22805153f, -0.20691898f, 0.10240629f, -0.22700751f, 0.00121667f, 0.03383791f, -0.20340976f, -0.11255612f, -0.23406039f),
    floatArrayOf(-0.32266143f, -0.52888644f, 0.73044598f, -0.09332733f, 0.00017107f, 0.47496381f, -0.27623209f, 0.05512840f, -0.23444746f, -0.65816504f, 0.04491862f, -0.47821224f, 0.23619515f, 0.22517762f, -0.03550949f, -0.16105735f, 0.04201148f),
    floatArrayOf(-0.12974620f, 0.13464460f, 0.01654150f, -0.28220856f, 0.53017527f, -0.71085572f, -0.31870896f, -0.43967083f, 0.38919595f, -0.18553741f, 0.29048488f, -0.11404966f, 0.75158411f, -0.45012143f, -0.30753058f, -0.32230937f, -0.18115662f),
    floatArrayOf(-0.35456085f, 0.38396266f, 0.35585371f, 0.02029514f, -0.09887882f, -0.16568063f, 0.46522135f, 0.23399240f, -0.51302981f, 0.34427419f, -0.53866041f, -0.48134473f, -0.32995701f, -0.68039143f, 0.31515485f, 0.41079488f, -0.26232210f),
    floatArrayOf(-0.71521091f, 0.64106762f, 0.32397407f, 0.30643982f, 0.29504097f, 0.10691492f, -0.16568801f, -0.18944663f, -0.17397982f, -0.34959644f, -0.03247731f, 0.01893195f, -0.27564809f, -0.32994375f, 0.92333919f, 0.13365540f, 0.25771463f),
)
private val NN2_B1 = floatArrayOf(-0.11364847f, -0.01573631f, 0.01641350f, -0.01287453f, 0.09960442f, 0.00761941f, 0.07500811f, -0.07317840f, -0.06325179f, 0.01602321f, -0.14993112f, -0.04699005f, -0.01250415f, 0.02267874f, 0.15281086f, -0.04790859f, 0.09774239f, 0.05515026f, -0.17217533f, -0.23647989f, 0.05733934f, 0.00559070f, 0.05902180f, -0.15955852f, -0.05008085f, 0.05798851f, 0.01717622f, 0.05776561f, 0.01980421f, 0.08390762f, -0.19470580f, -0.15112719f)
private val NN2_PA = floatArrayOf(0.23420137f, 0.10356650f, 0.01813022f, 0.04343412f, -0.00842485f, 0.14378558f, 0.04428518f, -0.17898785f, 0.86052847f, 0.14417818f, 0.07366382f, 0.01961853f, 0.25615248f, 0.05273965f, 0.08485956f, -0.27613065f, 0.09496025f, 0.09567812f, 0.16722204f, 0.79283416f, 0.04885687f, -0.02369863f, 0.25642443f, -0.01707325f, 0.05068994f, 0.16812097f, 0.33123791f, 0.00953902f, 0.03627227f, 0.16421342f, 0.11603388f, 1.44778299f)
private val NN2_W2 = floatArrayOf(-0.06222763f, -0.01343619f, 0.04505785f, -0.02973955f, 0.13665591f, 0.00380822f, -0.01606090f, -0.00959853f, 0.06419960f, -0.05098522f, 0.03232338f, 0.09108628f, 0.06044649f, -0.03988641f, 0.01531986f, 0.10681760f, 0.06147851f, -0.08270272f, 0.01571030f, -0.02321668f, -0.19449897f, -0.05111585f, -0.04145851f, 0.02045488f, -0.05000873f, 0.02974380f, -0.04413324f, -0.00372997f, -0.01051416f, -0.01933203f, -0.00034071f, -0.01721230f)
private const val NN2_B2 = -0.00349952f

fun upscaleTinyNNv2(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val inW = input.width; val inH = input.height
    val result = createImage(outW, outH)
    val sx = inW.toFloat() / outW; val sy = inH.toFloat() / outH

    for (oy in 0 until outH) {
        val srcY = (oy + 0.5f) * sy - 0.5f
        val iy = srcY.toInt().coerceIn(0, inH - 1)
        val fy = srcY - iy

        for (ox in 0 until outW) {
            val srcX = (ox + 0.5f) * sx - 0.5f
            val ix = srcX.toInt().coerceIn(0, inW - 1)
            val fx = srcX - ix

            // 3x3 luma
            val L = FloatArray(9)
            var k = 0
            for (dy in -1..1) for (dx in -1..1) {
                val p = getPixelF(input, clampCoord(ix+dx, inW), clampCoord(iy+dy, inH))
                L[k++] = luma(p[0], p[1], p[2])
            }

            // Dilated cross ±2
            val pUp = getPixelF(input, ix, clampCoord(iy-2, inH)); val dUp = luma(pUp[0], pUp[1], pUp[2])
            val pDn = getPixelF(input, ix, clampCoord(iy+2, inH)); val dDn = luma(pDn[0], pDn[1], pDn[2])
            val pLt = getPixelF(input, clampCoord(ix-2, inW), iy); val dLt = luma(pLt[0], pLt[1], pLt[2])
            val pRt = getPixelF(input, clampCoord(ix+2, inW), iy); val dRt = luma(pRt[0], pRt[1], pRt[2])

            // Gradient magnitude
            val gx = -L[0]+L[2]-2*L[3]+2*L[5]-L[6]+L[8]
            val gy = -L[0]-2*L[1]-L[2]+L[6]+2*L[7]+L[8]
            val gradMag = sqrt(gx*gx + gy*gy)

            // Local variance
            val mean = L.average().toFloat()
            val variance = L.map { (it-mean)*(it-mean) }.average().toFloat()

            // Bilinear
            val p11 = getPixelF(input, ix, iy)
            val p21 = getPixelF(input, clampCoord(ix+1, inW), iy)
            val p12 = getPixelF(input, ix, clampCoord(iy+1, inH))
            val p22 = getPixelF(input, clampCoord(ix+1, inW), clampCoord(iy+1, inH))
            val bil = FloatArray(3) { c -> (1-fx)*(1-fy)*p11[c] + fx*(1-fy)*p21[c] + (1-fx)*fy*p12[c] + fx*fy*p22[c] }
            val bicLuma = luma(bil[0], bil[1], bil[2])

            // NN input: 17 values
            val inp = floatArrayOf(*L, dUp, dDn, dLt, dRt, gradMag, variance, fx, fy)

            // Hidden: PReLU(W1*inp + B1)
            val h = FloatArray(32)
            for (j in 0..31) {
                var s = NN2_B1[j]
                for (i in 0..16) s += NN2_W1[j][i] * inp[i]
                h[j] = if (s >= 0) s else NN2_PA[j] * s
            }

            // Output
            var res = NN2_B2
            for (j in 0..31) res += NN2_W2[j] * h[j]

            val corrLuma = bicLuma + res
            val ratio = if (bicLuma > 0.001f) (corrLuma / bicLuma).coerceIn(0.8f, 1.2f) else 1f
            setPixel(result, ox, oy, floatArrayOf(
                (bil[0]*ratio).coerceIn(0f,1f), (bil[1]*ratio).coerceIn(0f,1f), (bil[2]*ratio).coerceIn(0f,1f)
            ))
        }
    }
    return result
}

// Trained weights: 11 -> 16 (ReLU) -> 1
private val NN_W1 = arrayOf(
    floatArrayOf(-0.16855514f, -0.92925972f, -0.98442978f, 0.34329975f, 0.41149089f, 0.22072943f, -0.04198847f, 0.37030515f, -0.06592251f, 0.04220565f, -0.51154804f),
    floatArrayOf(-0.07172213f, 0.18855977f, -0.23843971f, -0.23946273f, -0.43159166f, -0.47970226f, 0.37244648f, 0.36405545f, 0.56479651f, 0.00686237f, -0.55412102f),
    floatArrayOf(-0.78766543f, 0.27380177f, 0.06921509f, -1.17025030f, 0.21329223f, 0.10231556f, -0.51428944f, 0.62975210f, -0.17388032f, -1.24921358f, -0.00312707f),
    floatArrayOf(0.63439149f, 0.11995757f, -0.08222373f, 0.47934717f, -0.45203638f, -1.10146153f, 0.36486289f, -0.31616867f, -0.20023936f, -0.01690165f, -0.04872619f),
    floatArrayOf(-0.01767764f, -0.00788193f, -0.35041004f, -0.09448076f, 0.96657574f, 0.03870355f, -1.34436381f, -0.04397606f, -0.16506277f, 0.08820263f, -0.69907105f),
    floatArrayOf(-0.30537009f, -0.79603571f, -0.81097186f, 0.22148766f, 0.87891078f, 0.31425083f, -0.01122448f, 0.27848166f, -0.26841092f, -0.01401283f, 0.71582770f),
    floatArrayOf(-0.83545327f, 0.50713247f, -0.16818498f, -0.38301194f, 0.20186435f, 0.47672352f, -0.23967513f, 0.44186223f, -0.37370232f, 0.20269307f, 0.03770854f),
    floatArrayOf(0.59170586f, -0.03963555f, -0.16520661f, -0.53812784f, -0.86841118f, 0.12053194f, 0.35098445f, -0.05003193f, 0.51457310f, 0.00418267f, 0.28231433f),
    floatArrayOf(0.15099388f, -0.21561404f, 0.27309108f, 0.04560194f, 0.14129822f, 0.68960351f, 0.67553627f, -0.19050325f, -0.41252032f, 0.75457972f, -0.44232202f),
    floatArrayOf(0.02645458f, 0.07660850f, 0.02288682f, 0.92380655f, -0.50648451f, 0.48970509f, -0.46826959f, -1.28756344f, -0.40403846f, -0.00188941f, 0.35511214f),
    floatArrayOf(-0.27587792f, 0.43114656f, 0.44837236f, 0.66333401f, 0.67023766f, -0.62157011f, -0.26402062f, -0.54503191f, -1.01385558f, -0.06441662f, -0.73462152f),
    floatArrayOf(0.14532357f, -0.68342513f, -0.21842854f, 0.04673654f, 0.06423801f, 0.48259154f, 0.34043232f, -0.41981754f, 0.13767043f, 0.00414749f, 0.80593520f),
    floatArrayOf(0.89484823f, -0.62952465f, -0.29825518f, -1.46706676f, -0.01741776f, -0.13710219f, -0.03300381f, 0.17092586f, 0.50379497f, 0.00292463f, 0.52349991f),
    floatArrayOf(-0.08584571f, 0.34847462f, 0.75314569f, -0.18189706f, -1.06106544f, 0.02816024f, 0.30322102f, -0.26242957f, 0.18261577f, -0.01308800f, 0.13431822f),
    floatArrayOf(0.42106748f, 0.31728929f, -0.73259002f, 0.47889698f, 0.29152438f, -0.73200393f, -0.28528440f, 0.52131164f, -0.58537388f, -0.91888881f, 0.04546573f),
    floatArrayOf(-0.69341570f, -0.00391920f, 0.07384969f, 0.95475972f, 0.55622774f, 0.27579132f, -0.29096568f, -0.17103378f, -0.45623872f, 0.13464345f, 0.22846797f),
)
private val NN_B1 = floatArrayOf(-0.03807065f, 0.06361828f, 0.20749816f, 0.01309223f, 0.03799388f, -0.61370653f, -0.20911601f, -0.10336015f, 0.16259143f, -0.29892609f, 0.06629603f, -0.21222802f, -0.43988922f, -0.12412796f, 0.05428797f, -0.00457107f)
private val NN_W2 = floatArrayOf(-0.13392687f, -0.11094674f, -0.45129395f, -0.07866702f, 0.39380315f, 0.22080863f, 0.19551767f, 0.08251771f, -0.01240126f, -0.16858803f, 0.13422555f, -0.05422284f, -0.25943333f, -0.10629159f, 0.15783808f, 0.06817199f)
private const val NN_B2 = -0.00125137f

fun upscaleTinyNN(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val inW = input.width; val inH = input.height
    val result = createImage(outW, outH)
    val sx = inW.toFloat() / outW; val sy = inH.toFloat() / outH

    for (oy in 0 until outH) {
        val srcY = (oy + 0.5f) * sy - 0.5f
        val iy = srcY.toInt().coerceIn(0, inH - 1)
        val fy = srcY - iy

        for (ox in 0 until outW) {
            val srcX = (ox + 0.5f) * sx - 0.5f
            val ix = srcX.toInt().coerceIn(0, inW - 1)
            val fx = srcX - ix

            // 3x3 luma neighborhood
            val inp = FloatArray(11)
            var k = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val p = getPixelF(input, clampCoord(ix + dx, inW), clampCoord(iy + dy, inH))
                    inp[k++] = luma(p[0], p[1], p[2])
                }
            }
            inp[9] = fx; inp[10] = fy

            // Bilinear baseline
            val p11 = getPixelF(input, ix, iy)
            val p21 = getPixelF(input, clampCoord(ix + 1, inW), iy)
            val p12 = getPixelF(input, ix, clampCoord(iy + 1, inH))
            val p22 = getPixelF(input, clampCoord(ix + 1, inW), clampCoord(iy + 1, inH))
            val bilinear = FloatArray(3)
            for (c in 0..2) bilinear[c] = (1-fx)*(1-fy)*p11[c] + fx*(1-fy)*p21[c] + (1-fx)*fy*p12[c] + fx*fy*p22[c]
            val bicLuma = luma(bilinear[0], bilinear[1], bilinear[2])

            // Hidden layer
            val h = FloatArray(16)
            for (j in 0..15) {
                var s = NN_B1[j]
                for (i in 0..10) s += NN_W1[j][i] * inp[i]
                h[j] = maxOf(0f, s)
            }

            // Output
            var residual = NN_B2
            for (j in 0..15) residual += NN_W2[j] * h[j]

            val correctedLuma = bicLuma + residual
            val ratio = if (bicLuma > 0.001f) (correctedLuma / bicLuma).coerceIn(0.8f, 1.2f) else 1f
            val rgb = floatArrayOf(
                (bilinear[0] * ratio).coerceIn(0f, 1f),
                (bilinear[1] * ratio).coerceIn(0f, 1f),
                (bilinear[2] * ratio).coerceIn(0f, 1f)
            )
            setPixel(result, ox, oy, rgb)
        }
    }
    return result
}

/**
 * Fixed 3x integer bicubic with pre-computed Catmull-Rom weights.
 * Always upscales to exactly 3x the input size, then crops/scales to target.
 */
fun upscaleFixed3x(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val w3 = input.width * 3
    val h3 = input.height * 3
    // Pre-computed weights for phases 0, 1, 2
    val weights = arrayOf(
        floatArrayOf(0f, 1f, 0f, 0f),           // phase 0: exact copy
        floatArrayOf(-0.074074f, 0.888889f, 0.222222f, -0.037037f), // phase 1
        floatArrayOf(-0.037037f, 0.222222f, 0.888889f, -0.074074f)  // phase 2
    )

    val result = createImage(w3, h3)
    val inW = input.width; val inH = input.height

    for (oy in 0 until h3) {
        val phaseY = oy % 3
        val iy = oy / 3
        val wy = weights[phaseY]

        for (ox in 0 until w3) {
            val phaseX = ox % 3
            val ix = ox / 3
            val wx = weights[phaseX]

            // Fast path: phase (0,0) = exact copy
            if (phaseX == 0 && phaseY == 0) {
                val p = getPixelF(input, ix, iy)
                setPixel(result, ox, oy, p)
                continue
            }

            val rgb = FloatArray(3)
            for (dy in -1..2) {
                val sy = clampCoord(iy + dy, inH)
                for (dx in -1..2) {
                    val sx = clampCoord(ix + dx, inW)
                    val w = wx[dx + 1] * wy[dy + 1]
                    if (w != 0f) {
                        val p = getPixelF(input, sx, sy)
                        rgb[0] += p[0] * w; rgb[1] += p[1] * w; rgb[2] += p[2] * w
                    }
                }
            }
            setPixel(result, ox, oy, floatArrayOf(
                rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f)
            ))
        }
    }

    // If target size differs from 3x, scale to match
    if (outW != w3 || outH != h3) {
        return downscale(result, outW, outH)
    }
    return result
}

// ---------------------------------------------------------------------------
// Mitchell-Netravali kernel weight (B=1/3, C=1/3)
// ---------------------------------------------------------------------------

/** Mitchell-Netravali (B=1/3, C=1/3) basis weight for distance |t|. */
private fun mitchellWeight(t: Float): Float {
    val at = abs(t)
    return when {
        at < 1f -> (1f / 6f) * (7f * at * at * at - 12f * at * at + 16f / 3f)
        at < 2f -> (1f / 6f) * ((-7f / 3f) * at * at * at + 12f * at * at - 20f * at + 32f / 3f)
        else -> 0f
    }
}

// ---------------------------------------------------------------------------
// Helper: inline bicubic (Catmull-Rom) at fractional position
// ---------------------------------------------------------------------------

/** Compute Catmull-Rom bicubic at sub-pixel (ix0+fx, iy0+fy) in input. */
private fun sampleBicubicCR(input: BufferedImage, ix0: Int, iy0: Int, fx: Float, fy: Float): FloatArray {
    val result = FloatArray(3)
    var wTotal = 0f
    for (dy in -1..2) {
        val wy = catmullRomWeight(fy - dy)
        for (dx in -1..2) {
            val wx = catmullRomWeight(fx - dx)
            val w = wx * wy
            wTotal += w
            val p = getPixelF(input, ix0 + dx, iy0 + dy)
            result[0] += p[0] * w; result[1] += p[1] * w; result[2] += p[2] * w
        }
    }
    if (wTotal > 0f) { result[0] /= wTotal; result[1] /= wTotal; result[2] /= wTotal }
    return result
}

// ---------------------------------------------------------------------------
// Combo A: Bicubic + Selective Bilateral Smoothing
// ---------------------------------------------------------------------------

/**
 * Catmull-Rom bicubic, with selective bilateral smoothing near H.264 block
 * boundaries. Works directly at target resolution (no intermediate 3x step).
 */
fun upscaleBicubicBilateral(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
    val out = createOutput(outW, outH)
    val sx = input.width.toFloat() / outW
    val sy = input.height.toFloat() / outH
    val bilateralSigma = 15f

    for (oy in 0 until outH) {
        val iy = oy * sy + sy * 0.5f - 0.5f
        val iy0 = floor(iy).toInt()
        val fy = iy - iy0

        for (ox in 0 until outW) {
            val ix = ox * sx + sx * 0.5f - 0.5f
            val ix0 = floor(ix).toInt()
            val fx = ix - ix0

            // Standard Catmull-Rom bicubic
            val bicubic = sampleBicubicCR(input, ix0, iy0, fx, fy)

            // Block boundary detection in input space
            val dist8x = (ix % 8f).let { min(it, 8f - it) }
            val dist8y = (iy % 8f).let { min(it, 8f - it) }
            val dist4x = (ix % 4f).let { min(it, 4f - it) }
            val dist4y = (iy % 4f).let { min(it, 4f - it) }
            val nearBound = min(min(dist8x, dist8y), min(dist4x, dist4y))

            if (nearBound < 1.5f) {
                val centerP = getPixelF(input, ix0, iy0)
                val cL = luma(centerP[0], centerP[1], centerP[2])
                val bL = luma(bicubic[0], bicubic[1], bicubic[2])

                val pN = getPixelF(input, ix0, iy0 - 1)
                val pS = getPixelF(input, ix0, iy0 + 1)
                val pW = getPixelF(input, ix0 - 1, iy0)
                val pE = getPixelF(input, ix0 + 1, iy0)
                val lN = luma(pN[0], pN[1], pN[2])
                val lS = luma(pS[0], pS[1], pS[2])
                val lW = luma(pW[0], pW[1], pW[2])
                val lE = luma(pE[0], pE[1], pE[2])

                val maxStep = max(max(abs(cL - lN), abs(cL - lS)), max(abs(cL - lW), abs(cL - lE)))
                val neighborVar = (abs(lN - lS) + abs(lW - lE)) * 0.5f
                val artifactness = maxStep * (1f - (neighborVar / (maxStep * 0.8f + 0.01f)).coerceIn(0f, 1f))
                val boundProximity = 1f - (nearBound / 1.5f).coerceIn(0f, 1f)
                val suppressStrength = (artifactness * 3f).coerceIn(0f, 1f) * boundProximity * 0.5f

                if (suppressStrength > 0.01f) {
                    val smoothed = FloatArray(3)
                    var wTotal = 0f
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val p = getPixelF(input, ix0 + dx, iy0 + dy)
                            val pL = luma(p[0], p[1], p[2])
                            val lumaDiff = pL - bL
                            val spatialW = if (dx == 0 && dy == 0) 1f else if (abs(dx) + abs(dy) == 1) 0.7f else 0.5f
                            val rangeW = exp(-lumaDiff * lumaDiff * bilateralSigma)
                            val w = spatialW * rangeW
                            wTotal += w
                            smoothed[0] += p[0] * w; smoothed[1] += p[1] * w; smoothed[2] += p[2] * w
                        }
                    }
                    if (wTotal > 0f) { smoothed[0] /= wTotal; smoothed[1] /= wTotal; smoothed[2] /= wTotal }
                    for (c in 0..2) bicubic[c] = bicubic[c] * (1f - suppressStrength) + smoothed[c] * suppressStrength
                }
            }

            out.setRGB(ox, oy, packRgb(bicubic))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Combo B: Bicubic + Anime4K-style Clamp (local min/max clamping)
// ---------------------------------------------------------------------------

/**
 * Catmull-Rom bicubic with local min/max clamping of the 2x2 nearest input
 * pixels. Prevents ringing from negative lobes, improving SSIM.
 * Inspired by Anime4K's Clamp_Highlights and FSR EASU's deringing.
 */
fun upscaleBicubicClamp(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
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

            // Catmull-Rom bicubic
            val bicubic = sampleBicubicCR(input, ix0, iy0, fx, fy)

            // Local min/max from the 2x2 cell the point falls in
            val p00 = getPixelF(input, ix0, iy0)
            val p10 = getPixelF(input, ix0 + 1, iy0)
            val p01 = getPixelF(input, ix0, iy0 + 1)
            val p11 = getPixelF(input, ix0 + 1, iy0 + 1)

            for (c in 0..2) {
                val lo = min(min(p00[c], p10[c]), min(p01[c], p11[c]))
                val hi = max(max(p00[c], p10[c]), max(p01[c], p11[c]))
                bicubic[c] = bicubic[c].coerceIn(lo, hi)
            }

            out.setRGB(ox, oy, packRgb(bicubic))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Combo C: Luma-Guided (bicubic/bilinear blend based on local contrast)
// ---------------------------------------------------------------------------

/**
 * Blends between bicubic (sharp) and bilinear (smooth) based on local
 * luminance contrast. Low contrast -> bilinear (better SSIM for flat areas),
 * high contrast -> bicubic (sharper edges).
 */
fun upscaleLumaGuided(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
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

            // Bicubic
            val bicubic = sampleBicubicCR(input, ix0, iy0, fx, fy)

            // Bilinear
            val bilinear = sampleBilinear(input, ix, iy)

            // Local contrast from 3x3 neighborhood
            val centerP = getPixelF(input, ix0, iy0)
            val centerL = luma(centerP[0], centerP[1], centerP[2])
            var maxDiff = 0f
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    maxDiff = max(maxDiff, abs(luma(p[0], p[1], p[2]) - centerL))
                }
            }

            // Smoothstep: low contrast -> bilinear, high contrast -> bicubic
            val t = ((maxDiff - 0.03f) / (0.15f - 0.03f)).coerceIn(0f, 1f)
            val blend = t * t * (3f - 2f * t)

            val rgb = FloatArray(3) { c ->
                bilinear[c] * (1f - blend) + bicubic[c] * blend
            }
            out.setRGB(ox, oy, packRgb(rgb))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Combo D: Dual-Kernel Blend (Catmull-Rom + Mitchell-Netravali)
// ---------------------------------------------------------------------------

/**
 * Blends Catmull-Rom and Mitchell-Netravali based on local variance.
 * High variance (edges) -> Catmull-Rom, low variance (flat) -> Mitchell.
 * Mitchell has less ringing, better for smooth regions.
 */
fun upscaleDualKernel(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
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

            val catmullRom = FloatArray(3)
            val mitchell = FloatArray(3)
            var crTotal = 0f
            var mnTotal = 0f

            val centerP = getPixelF(input, ix0, iy0)
            val centerL = luma(centerP[0], centerP[1], centerP[2])
            var localVariance = 0f

            for (dy in -1..2) {
                val crWy = catmullRomWeight(fy - dy)
                val mnWy = mitchellWeight(fy - dy)
                for (dx in -1..2) {
                    val crWx = catmullRomWeight(fx - dx)
                    val mnWx = mitchellWeight(fx - dx)
                    val crW = crWx * crWy
                    val mnW = mnWx * mnWy
                    crTotal += crW
                    mnTotal += mnW
                    val p = getPixelF(input, ix0 + dx, iy0 + dy)
                    catmullRom[0] += p[0] * crW; catmullRom[1] += p[1] * crW; catmullRom[2] += p[2] * crW
                    mitchell[0] += p[0] * mnW; mitchell[1] += p[1] * mnW; mitchell[2] += p[2] * mnW

                    if (dx in 0..1 && dy in 0..1) {
                        val l = luma(p[0], p[1], p[2])
                        val diff = l - centerL
                        localVariance += diff * diff
                    }
                }
            }
            if (crTotal > 0f) { catmullRom[0] /= crTotal; catmullRom[1] /= crTotal; catmullRom[2] /= crTotal }
            if (mnTotal > 0f) { mitchell[0] /= mnTotal; mitchell[1] /= mnTotal; mitchell[2] /= mnTotal }
            localVariance /= 4f

            val t = ((sqrt(localVariance) - 0.02f) / (0.12f - 0.02f)).coerceIn(0f, 1f)
            val blend = t * t * (3f - 2f * t)

            val rgb = FloatArray(3) { c ->
                mitchell[c] * (1f - blend) + catmullRom[c] * blend
            }
            out.setRGB(ox, oy, packRgb(rgb))
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Combo E: SSIM-Optimized Bicubic
// ---------------------------------------------------------------------------

/**
 * Catmull-Rom bicubic with overshoot correction: where bicubic ringing pushes
 * values beyond the local 2x2 min/max, blend toward bilinear. This directly
 * targets SSIM improvement by reducing structural error from negative lobes.
 */
fun upscaleSSIMOptimized(input: BufferedImage, outW: Int, outH: Int): BufferedImage {
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

            // Bicubic
            val bicubic = sampleBicubicCR(input, ix0, iy0, fx, fy)

            // Bilinear
            val bilinear = sampleBilinear(input, ix, iy)

            // Local min/max from 2x2 cell
            val p00 = getPixelF(input, ix0, iy0)
            val p10 = getPixelF(input, ix0 + 1, iy0)
            val p01 = getPixelF(input, ix0, iy0 + 1)
            val p11 = getPixelF(input, ix0 + 1, iy0 + 1)

            var overshoot = 0f
            for (c in 0..2) {
                val lo = min(min(p00[c], p10[c]), min(p01[c], p11[c]))
                val hi = max(max(p00[c], p10[c]), max(p01[c], p11[c]))
                if (bicubic[c] < lo) overshoot += lo - bicubic[c]
                if (bicubic[c] > hi) overshoot += bicubic[c] - hi
            }

            // Blend toward bilinear proportionally to overshoot
            val fallback = (overshoot * 8f).coerceIn(0f, 1f)
            val rgb = FloatArray(3) { c ->
                val blended = bicubic[c] * (1f - fallback) + bilinear[c] * fallback
                // Soft clamp
                val lo = min(min(p00[c], p10[c]), min(p01[c], p11[c]))
                val hi = max(max(p00[c], p10[c]), max(p01[c], p11[c]))
                val margin = (hi - lo) * 0.05f
                blended.coerceIn(lo - margin, hi + margin)
            }

            out.setRGB(ox, oy, packRgb(rgb))
        }
    }
    return out
}
