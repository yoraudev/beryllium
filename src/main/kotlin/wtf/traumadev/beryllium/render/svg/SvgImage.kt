package wtf.traumadev.beryllium.render.svg

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.ViewBox
import com.mojang.renderpearl.api.commands.CommandEncoder
import wtf.traumadev.beryllium.render.texture.ImageSource
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureUploader
import wtf.traumadev.beryllium.util.Logger
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.ceil

class SvgImage private constructor(
	val label: String,
	private val rasterScale: Float,
	private val contentLoader: (() -> ByteArray?)?,
	val filePath: Path?,
) : ImageSource, AutoCloseable {
	override var image: Texture2D? = null
		private set
	var rasterWidth: Int = 0
		private set
	var rasterHeight: Int = 0
		private set

	private var document: SVGDocument? = null
	private var pendingUpload: ByteBuffer? = null
	private val changeListeners = mutableListOf<() -> Unit>()
	private var closed = false

	override fun addChangeListener(listener: () -> Unit) {
		changeListeners.add(listener)
	}

	override fun removeChangeListener(listener: () -> Unit) {
		changeListeners.remove(listener)
	}

	fun rasterize(width: Int, height: Int) {
		val document = document ?: return
		val targetWidth = width.coerceIn(1, MAX_RASTER_SIZE)
		val targetHeight = height.coerceIn(1, MAX_RASTER_SIZE)
		val raster = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
		val graphics = raster.createGraphics()
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
		document.render(null, graphics, ViewBox(targetWidth.toFloat(), targetHeight.toFloat()))
		graphics.dispose()
		val pixels = raster.getRGB(0, 0, targetWidth, targetHeight, null, 0, targetWidth)
		val buffer = ByteBuffer.allocateDirect(pixels.size * 4).order(ByteOrder.nativeOrder())
		for (pixel in pixels) {
			buffer.put(((pixel shr 16) and 0xFF).toByte())
			buffer.put(((pixel shr 8) and 0xFF).toByte())
			buffer.put((pixel and 0xFF).toByte())
			buffer.put(((pixel ushr 24) and 0xFF).toByte())
		}
		buffer.position(0)
		rasterWidth = targetWidth
		rasterHeight = targetHeight
		pendingUpload = buffer
	}

	fun reload(): Boolean {
		val loader = contentLoader ?: return false
		val bytes = try {
			loader()
		} catch (e: Exception) {
			Logger.warn("Failed to read SVG '$label': ${e.message}")
			return false
		}
		if (bytes == null) {
			Logger.warn("SVG '$label' source is no longer readable")
			return false
		}
		return load(bytes)
	}

	private fun load(bytes: ByteArray): Boolean {
		val document = LOADER.load(ByteArrayInputStream(bytes), null, LoaderContext.createDefault())
		if (document == null) {
			Logger.warn("Failed to parse SVG '$label'")
			return false
		}
		this.document = document
		val size = document.size()
		val scale = if (rasterScale > 0f) rasterScale else DEFAULT_RASTER_SCALE
		rasterize(ceil(size.width * scale).toInt(), ceil(size.height * scale).toInt())
		return true
	}

	internal fun upload(encoder: CommandEncoder, uploader: TextureUploader) {
		val pending = pendingUpload ?: return
		if (rasterWidth == 0 || rasterHeight == 0) return
		val current = image
		val target = if (current == null || current.width != rasterWidth || current.height != rasterHeight) {
			current?.close()
			Texture2D.create(label, rasterWidth, rasterHeight).also { created ->
				image = created
				for (listener in changeListeners.toList()) listener()
			}
		} else {
			current
		}
		uploader.upload(encoder, target, 0, 0, rasterWidth, rasterHeight, pending, 0, rasterWidth * 4)
		pendingUpload = null
	}

	override fun close() {
		if (closed) return
		closed = true
		Svgs.unregister(this)
		pendingUpload = null
		image?.close()
		image = null
		changeListeners.clear()
	}

	companion object {
		private val LOADER = SVGLoader()
		private const val MAX_RASTER_SIZE = 4096
		const val DEFAULT_RASTER_SCALE = 4f

		fun ofSource(source: String, label: String = "svg", rasterScale: Float = DEFAULT_RASTER_SCALE): SvgImage =
			fromBytes(source.toByteArray(Charsets.UTF_8), label, rasterScale, contentLoader = null, filePath = null)

		fun ofFile(path: Path, label: String? = null, rasterScale: Float = DEFAULT_RASTER_SCALE): SvgImage {
			val resolvedLabel = label ?: path.fileName?.toString() ?: "svg"
			return fromBytes(
				Files.readAllBytes(path),
				resolvedLabel,
				rasterScale,
				contentLoader = { Files.readAllBytes(path) },
				filePath = path,
			)
		}

		fun ofResource(resourcePath: String, label: String = resourcePath.substringAfterLast('/'), rasterScale: Float = DEFAULT_RASTER_SCALE): SvgImage {
			val stream = SvgImage::class.java.getResourceAsStream(resourcePath)
				?: throw IllegalArgumentException("SVG resource not found: $resourcePath")
			val bytes = stream.use { it.readBytes() }
			return fromBytes(bytes, label, rasterScale, contentLoader = null, filePath = null)
		}

		private fun fromBytes(bytes: ByteArray, label: String, rasterScale: Float, contentLoader: (() -> ByteArray?)?, filePath: Path?): SvgImage {
			val image = SvgImage(label, rasterScale, contentLoader, filePath)
			if (!image.load(bytes)) throw IllegalArgumentException("Failed to parse SVG '$label'")
			Svgs.register(image)
			return image
		}
	}
}
