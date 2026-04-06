package bench

import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val DOWNSCALE_WIDTH = 640
private const val DOWNSCALE_HEIGHT = 448
private const val JPEG_COMPRESSION_QUALITY = 0.75f

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg")

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

fun main() {
    val projectRoot = findProjectRoot()
    val benchDir = File(projectRoot, "imgs")
    val referenceDir = File(benchDir, "reference")
    val downscaledDir = File(benchDir, "downscaled")
    val resultsDir = File(benchDir, "results")

    referenceDir.mkdirs()
    downscaledDir.mkdirs()
    resultsDir.mkdirs()

    val referenceFiles = referenceDir.listFiles()
        ?.filter { it.extension.lowercase() in IMAGE_EXTENSIONS }
        ?.sortedBy { it.name }
        ?: emptyList()

    if (referenceFiles.isEmpty()) {
        println("No reference images found in: ${referenceDir.absolutePath}")
        println()
        println("To use this benchmark:")
        println("  1. Place high-resolution reference images (PNG or JPG) in:")
        println("     ${referenceDir.absolutePath}")
        println("  2. Run the benchmark again.")
        println()
        println("The tool will automatically downscale them to ${DOWNSCALE_WIDTH}x${DOWNSCALE_HEIGHT},")
        println("run all upscalers, and compare against the originals.")
        return
    }

    println("Upscale Benchmark")
    println("=================")
    println("Reference images: ${referenceFiles.size}")
    println("Upscalers:        ${ALL_UPSCALERS.size}")
    println()

    data class Result(
        val imageName: String,
        val upscalerName: String,
        val psnr: Double,
        val ssim: Double,
        val edgeScore: Double,
        val lumaRmse: Double,
        val timeMs: Long,
    )

    val allResults = mutableListOf<Result>()

    for (refFile in referenceFiles) {
        val baseName = refFile.nameWithoutExtension
        println("Processing: $baseName")

        val reference = try {
            loadImage(refFile)
        } catch (e: Exception) {
            println("  WARNING: Skipping (cannot load): ${e.message}")
            continue
        }

        // Ensure we have a downscaled version
        val downscaledFile = File(downscaledDir, "${baseName}.png")
        if (!downscaledFile.exists()) {
            print("  Creating downscaled version (${DOWNSCALE_WIDTH}x${DOWNSCALE_HEIGHT})...")
            val downscaled = downscale(reference, DOWNSCALE_WIDTH, DOWNSCALE_HEIGHT)
            val withCompression = simulateJpegCompression(downscaled, JPEG_COMPRESSION_QUALITY)
            saveImage(withCompression, downscaledFile)
            println(" done")
        }

        val downscaled = try {
            loadImage(downscaledFile)
        } catch (e: Exception) {
            println("  WARNING: Skipping (cannot load downscaled): ${e.message}")
            continue
        }

        val targetW = reference.width
        val targetH = reference.height

        for ((upscalerName, upscaleFn) in ALL_UPSCALERS) {
            print("  $upscalerName...")
            try {
                val startNs = System.nanoTime()
                val upscaled = upscaleFn(downscaled, targetW, targetH)
                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

                val metrics = computeAllMetrics(reference, upscaled)

                allResults += Result(
                    baseName, upscalerName,
                    metrics.psnr, metrics.ssim, metrics.edgeScore, metrics.lumaRmse,
                    elapsedMs,
                )

                val resultFile = File(resultsDir, "${baseName}_${sanitize(upscalerName)}.png")
                saveImage(upscaled, resultFile)

                println(" PSNR=%.1fdB SSIM=%.3f Edge=%.3f RMSE=%.4f (%dms)".format(
                    metrics.psnr, metrics.ssim, metrics.edgeScore, metrics.lumaRmse, elapsedMs
                ))
            } catch (e: Exception) {
                println(" FAILED: ${e.message}")
            }
        }
        println()
    }

    if (allResults.isEmpty()) {
        println("No results collected.")
        return
    }

    // Print results table
    val table = buildResultsTable(allResults)
    println(table)

    // Save markdown version
    val mdTable = buildMarkdownResults(allResults)
    val mdFile = File(resultsDir, "benchmark_results.md")
    mdFile.writeText(mdTable)
    println("Results saved to: ${mdFile.absolutePath}")
}

// ---------------------------------------------------------------------------
// Project root finder
// ---------------------------------------------------------------------------

private fun findProjectRoot(): File {
    var dir = File(System.getProperty("user.dir"))
    while (dir.parentFile != null) {
        if (File(dir, "settings.gradle.kts").exists()) return dir
        dir = dir.parentFile
    }
    return File(System.getProperty("user.dir"))
}

// ---------------------------------------------------------------------------
// JPEG compression simulation
// ---------------------------------------------------------------------------

private fun simulateJpegCompression(img: java.awt.image.BufferedImage, quality: Float): java.awt.image.BufferedImage {
    val writers = ImageIO.getImageWritersByFormatName("jpeg")
    if (!writers.hasNext()) return img
    val writer = writers.next()
    val param = writer.defaultWriteParam
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = quality

    val baos = java.io.ByteArrayOutputStream()
    val ios = ImageIO.createImageOutputStream(baos)
    writer.output = ios
    writer.write(null, IIOImage(img, null, null), param)
    ios.close()
    writer.dispose()

    val bais = java.io.ByteArrayInputStream(baos.toByteArray())
    return ImageIO.read(bais) ?: img
}

// ---------------------------------------------------------------------------
// Table formatting
// ---------------------------------------------------------------------------

private fun sanitize(name: String): String =
    name.replace(Regex("[^a-zA-Z0-9_-]"), "_")

private data class Row(
    val imageName: String,
    val upscalerName: String,
    val psnr: Double,
    val ssim: Double,
    val edgeScore: Double,
    val lumaRmse: Double,
)

private fun toRows(results: List<Any>): List<Row> = results.map { r ->
    val cls = r::class.java
    Row(
        cls.getDeclaredField("imageName").also { it.isAccessible = true }.get(r) as String,
        cls.getDeclaredField("upscalerName").also { it.isAccessible = true }.get(r) as String,
        cls.getDeclaredField("psnr").also { it.isAccessible = true }.get(r) as Double,
        cls.getDeclaredField("ssim").also { it.isAccessible = true }.get(r) as Double,
        cls.getDeclaredField("edgeScore").also { it.isAccessible = true }.get(r) as Double,
        cls.getDeclaredField("lumaRmse").also { it.isAccessible = true }.get(r) as Double,
    )
}

private fun buildResultsTable(results: List<Any>): String {
    val rows = toRows(results)
    val imgW = maxOf(20, rows.maxOf { it.imageName.length } + 2)
    val upW = maxOf(15, rows.maxOf { it.upscalerName.length } + 2)

    val sb = StringBuilder()

    fun hline(left: Char, mid: Char, right: Char, fill: Char = '\u2550') {
        sb.append(left)
        sb.append(fill.toString().repeat(imgW))
        sb.append(mid)
        sb.append(fill.toString().repeat(upW))
        sb.append(mid)
        sb.append(fill.toString().repeat(8))
        sb.append(mid)
        sb.append(fill.toString().repeat(6))
        sb.append(mid)
        sb.append(fill.toString().repeat(12))
        sb.append(mid)
        sb.append(fill.toString().repeat(11))
        sb.appendLine(right)
    }

    fun row(img: String, up: String, psnr: String, ssim: String, edge: String, rmse: String) {
        sb.append("\u2551 ")
        sb.append(img.padEnd(imgW - 2))
        sb.append(" \u2551 ")
        sb.append(up.padEnd(upW - 2))
        sb.append(" \u2551 ")
        sb.append(psnr.padStart(6))
        sb.append(" \u2551 ")
        sb.append(ssim.padStart(4))
        sb.append(" \u2551 ")
        sb.append(edge.padStart(10))
        sb.append(" \u2551 ")
        sb.append(rmse.padStart(9))
        sb.appendLine(" \u2551")
    }

    hline('\u2554', '\u2566', '\u2557')
    row("Image", "Upscaler", "PSNR", "SSIM", "Edge Score", "Luma RMSE")
    hline('\u2560', '\u256C', '\u2563')

    for (r in rows) {
        row(
            r.imageName,
            r.upscalerName,
            "%.1fdB".format(r.psnr),
            "%.2f".format(r.ssim),
            "%.3f".format(r.edgeScore),
            "%.4f".format(r.lumaRmse),
        )
    }
    hline('\u255A', '\u2569', '\u255D')

    sb.appendLine()
    appendBestAndRanking(sb, rows)
    return sb.toString()
}

private fun buildMarkdownResults(results: List<Any>): String {
    val rows = toRows(results)
    val sb = StringBuilder()
    sb.appendLine("# Upscale Benchmark Results")
    sb.appendLine()
    sb.appendLine("| Image | Upscaler | PSNR | SSIM | Edge Score | Luma RMSE |")
    sb.appendLine("|-------|----------|------|------|------------|-----------|")

    for (r in rows) {
        sb.appendLine("| ${r.imageName} | ${r.upscalerName} | %.1fdB | %.3f | %.3f | %.4f |".format(
            r.psnr, r.ssim, r.edgeScore, r.lumaRmse
        ))
    }

    sb.appendLine()
    sb.appendLine("## Best per metric")
    sb.appendLine()
    val bestPsnr = rows.maxByOrNull { it.psnr }
    val bestSsim = rows.maxByOrNull { it.ssim }
    val bestEdge = rows.maxByOrNull { it.edgeScore }
    val bestRmse = rows.minByOrNull { it.lumaRmse }
    if (bestPsnr != null) sb.appendLine("- **PSNR:** ${bestPsnr.upscalerName} (%.1f dB)".format(bestPsnr.psnr))
    if (bestSsim != null) sb.appendLine("- **SSIM:** ${bestSsim.upscalerName} (%.3f)".format(bestSsim.ssim))
    if (bestEdge != null) sb.appendLine("- **Edge Score:** ${bestEdge.upscalerName} (%.3f)".format(bestEdge.edgeScore))
    if (bestRmse != null) sb.appendLine("- **Luma RMSE:** ${bestRmse.upscalerName} (%.4f)".format(bestRmse.lumaRmse))

    sb.appendLine()
    sb.appendLine("## Overall ranking")
    sb.appendLine()
    sb.appendLine("Weighted: 40% SSIM + 30% PSNR_norm + 20% Edge + 10% RMSE_inv")
    sb.appendLine()

    val scored = computeRanking(rows)
    for ((i, entry) in scored.withIndex()) {
        sb.appendLine("${i + 1}. **${entry.first}** -- score: %.3f".format(entry.second))
    }

    return sb.toString()
}

private fun appendBestAndRanking(sb: StringBuilder, rows: List<Row>) {
    sb.appendLine("Best per metric:")
    val bestPsnr = rows.maxByOrNull { it.psnr }
    val bestSsim = rows.maxByOrNull { it.ssim }
    val bestEdge = rows.maxByOrNull { it.edgeScore }
    val bestRmse = rows.minByOrNull { it.lumaRmse }

    if (bestPsnr != null) sb.appendLine("  PSNR:       ${bestPsnr.upscalerName} (%.1f dB)".format(bestPsnr.psnr))
    if (bestSsim != null) sb.appendLine("  SSIM:       ${bestSsim.upscalerName} (%.3f)".format(bestSsim.ssim))
    if (bestEdge != null) sb.appendLine("  Edge Score: ${bestEdge.upscalerName} (%.3f)".format(bestEdge.edgeScore))
    if (bestRmse != null) sb.appendLine("  Luma RMSE:  ${bestRmse.upscalerName} (%.4f)".format(bestRmse.lumaRmse))

    sb.appendLine()
    sb.appendLine("Overall ranking (weighted: 40% SSIM + 30% PSNR_norm + 20% Edge + 10% RMSE_inv):")

    val scored = computeRanking(rows)
    for ((i, entry) in scored.withIndex()) {
        sb.appendLine("  ${i + 1}. ${entry.first.padEnd(20)} -- score: %.3f".format(entry.second))
    }
}

private fun computeRanking(rows: List<Row>): List<Pair<String, Double>> {
    val maxPsnr = rows.maxOf { it.psnr }
    val minPsnr = rows.minOf { it.psnr }
    val psnrRange = (maxPsnr - minPsnr).let { if (it < 1e-6) 1.0 else it }
    val maxRmse = rows.maxOf { it.lumaRmse }
    val minRmse = rows.minOf { it.lumaRmse }
    val rmseRange = (maxRmse - minRmse).let { if (it < 1e-6) 1.0 else it }

    val byUpscaler = rows.groupBy { it.upscalerName }
    return byUpscaler.map { (name, entries) ->
        val avgSsim = entries.map { it.ssim }.average()
        val avgPsnr = entries.map { it.psnr }.average()
        val avgEdge = entries.map { it.edgeScore }.average()
        val avgRmse = entries.map { it.lumaRmse }.average()

        val psnrNorm = (avgPsnr - minPsnr) / psnrRange
        val rmseInv = 1.0 - (avgRmse - minRmse) / rmseRange

        val score = 0.40 * avgSsim + 0.30 * psnrNorm + 0.20 * avgEdge + 0.10 * rmseInv
        name to score
    }.sortedByDescending { it.second }
}
