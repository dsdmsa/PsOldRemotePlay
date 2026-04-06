package bench

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Loads an image from the given file.
 *
 * @throws IllegalArgumentException if the file does not exist or cannot be read as an image.
 */
fun loadImage(file: File): BufferedImage {
    require(file.exists()) { "Image file not found: ${file.absolutePath}" }
    return ImageIO.read(file)
        ?: throw IllegalArgumentException("Failed to decode image (unsupported format?): ${file.absolutePath}")
}

/**
 * Saves an image as PNG to the given file.
 * Parent directories are created automatically if they do not exist.
 */
fun saveImage(img: BufferedImage, file: File) {
    file.parentFile?.mkdirs()
    val success = ImageIO.write(img, "PNG", file)
    if (!success) {
        throw IllegalStateException("No PNG writer available — cannot save image to ${file.absolutePath}")
    }
}

/**
 * Downscales an image to the given dimensions using bicubic interpolation.
 *
 * @return a new [BufferedImage] of size [targetWidth] x [targetHeight].
 */
fun downscale(img: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
    require(targetWidth > 0 && targetHeight > 0) { "Target dimensions must be positive" }
    val output = createImage(targetWidth, targetHeight)
    val g2d = output.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.drawImage(img, 0, 0, targetWidth, targetHeight, null)
    g2d.dispose()
    return output
}

/**
 * Reads the RGB value of a single pixel as floats in the 0.0–1.0 range.
 *
 * @return a [FloatArray] of size 3: [R, G, B].
 */
fun getPixel(img: BufferedImage, x: Int, y: Int): FloatArray {
    val rgb = img.getRGB(x, y)
    val r = ((rgb shr 16) and 0xFF) / 255f
    val g = ((rgb shr 8) and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    return floatArrayOf(r, g, b)
}

/**
 * Writes an RGB pixel (each channel 0.0–1.0) into the image.
 * Values are clamped to the valid range.
 */
fun setPixel(img: BufferedImage, x: Int, y: Int, rgb: FloatArray) {
    val r = (rgb[0].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val g = (rgb[1].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    val b = (rgb[2].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
}

/**
 * Creates a new blank RGB image of the specified size.
 */
fun createImage(width: Int, height: Int): BufferedImage {
    require(width > 0 && height > 0) { "Image dimensions must be positive" }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
}
