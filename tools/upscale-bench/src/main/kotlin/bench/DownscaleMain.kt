package bench

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/** Default downscale target dimensions (PS2 native output). */
private const val TARGET_WIDTH = 640
private const val TARGET_HEIGHT = 448

/** JPEG quality level used to simulate H.264-like compression artifacts. */
private const val JPEG_ARTIFACT_QUALITY = 85

/**
 * Standalone tool that reads high-resolution reference images and produces
 * downscaled versions suitable for upscaler benchmarking.
 *
 * Usage:
 *   ./gradlew :tools:upscale-bench:run -PmainClass=bench.DownscaleMainKt
 *
 * Reads from `imgs/reference/` and writes to `imgs/downscaled/` relative
 * to the project root.
 */
fun main() {
    val projectRoot = findDownscaleProjectRoot()
    val referenceDir = projectRoot.resolve("tools/upscale-bench/imgs/reference")
    val downscaledDir = projectRoot.resolve("tools/upscale-bench/imgs/downscaled")

    if (!referenceDir.isDirectory) {
        referenceDir.mkdirs()
        println("Reference directory created: ${referenceDir.absolutePath}")
        println("Place PNG/JPG images inside, then re-run.")
        return
    }

    downscaledDir.mkdirs()

    val imageFiles = referenceDir.listFiles { f ->
        f.isFile && f.extension.lowercase() in setOf("png", "jpg", "jpeg")
    }?.sortedBy { it.name } ?: emptyList()

    if (imageFiles.isEmpty()) {
        println("No PNG/JPG images found in ${referenceDir.absolutePath}")
        return
    }

    println("Downscale Tool")
    println("==============")
    println("Target size:      ${TARGET_WIDTH}x${TARGET_HEIGHT}")
    println("JPEG quality:     $JPEG_ARTIFACT_QUALITY")
    println("Reference images: ${imageFiles.size}")
    println()

    var processed = 0

    for (file in imageFiles) {
        try {
            val original = loadImage(file)
            val baseName = file.nameWithoutExtension

            // Pure bicubic downscale
            val downscaled = downscale(original, TARGET_WIDTH, TARGET_HEIGHT)
            val outFile = downscaledDir.resolve("${baseName}_${TARGET_WIDTH}x${TARGET_HEIGHT}.png")
            saveImage(downscaled, outFile)

            // Version with simulated compression artifacts (JPEG round-trip)
            val degraded = simulateCompressionArtifacts(downscaled, JPEG_ARTIFACT_QUALITY)
            val degradedFile = downscaledDir.resolve("${baseName}_${TARGET_WIDTH}x${TARGET_HEIGHT}_q${JPEG_ARTIFACT_QUALITY}.png")
            saveImage(degraded, degradedFile)

            println("  [OK] ${file.name} -> ${outFile.name}, ${degradedFile.name}")
            processed++
        } catch (e: Exception) {
            println("  [FAIL] ${file.name}: ${e.message}")
        }
    }

    println()
    println("Processed $processed image(s).")
    println("Output: ${downscaledDir.absolutePath}")
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Simulates H.264-like quality degradation by JPEG-compressing the image
 * at the given quality (0-100) and decoding it back.
 */
private fun simulateCompressionArtifacts(img: java.awt.image.BufferedImage, quality: Int): java.awt.image.BufferedImage {
    val writers = ImageIO.getImageWritersByFormatName("JPEG")
    if (!writers.hasNext()) return img
    val writer = writers.next()
    val param = writer.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = quality / 100f
    }

    val baos = ByteArrayOutputStream()
    ImageIO.createImageOutputStream(baos).use { ios ->
        writer.output = ios
        writer.write(null, IIOImage(img, null, null), param)
    }
    writer.dispose()

    return ImageIO.read(ByteArrayInputStream(baos.toByteArray()))
        ?: throw IllegalStateException("Failed to decode JPEG round-trip image")
}

/**
 * Walks up from the current working directory until a `settings.gradle.kts`
 * file is found.
 */
private fun findDownscaleProjectRoot(): File {
    var dir: File? = File(System.getProperty("user.dir"))
    while (dir != null) {
        if (dir.resolve("settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    throw IllegalStateException(
        "Could not find project root (no settings.gradle.kts in any parent of ${System.getProperty("user.dir")})"
    )
}
