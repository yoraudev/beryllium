package wtf.traumadev.beryllium.render.font

import com.mojang.renderpearl.api.commands.CommandEncoder
import wtf.traumadev.beryllium.util.Logger

object Fonts {
	internal val atlas = GlyphAtlas()

	private val fonts = LinkedHashMap<String, FontRenderer>()
	private val ordered = ArrayList<FontRenderer>()
	private var nextFontId = 0
	private var prewarmFontIndex = 0
	private var prewarmCharIndex = 0

	internal val prewarmComplete: Boolean
		get() = prewarmFontIndex >= ordered.size

	val regular: FontRenderer
		get() = fonts["inter"] ?: fallback

	val bold: FontRenderer
		get() = fonts["inter-bold"] ?: fallback

	val names: Set<String>
		get() = fonts.keys

	val nativeAvailable: Boolean
		get() = NativeFont.available

	private val fallback: FontRenderer by lazy {
		FontRenderer(NullFontFace, FALLBACK_ID, atlas)
	}

	fun find(name: String): FontRenderer? = fonts[name.lowercase()]

	fun register(name: String, data: ByteArray): FontRenderer {
		val key = name.lowercase()
		require(key.isNotBlank()) { "Font name must not be blank" }
		require(!fonts.containsKey(key)) { "Font already registered: $name" }
		check(NativeFont.available) { "Native font library unavailable; custom fonts cannot be registered" }
		val handle = NativeFont.openFont(data)
		require(handle != 0L) { "Font data could not be parsed: $name" }
		return registerLoaded(key, NativeFontFace(handle))
	}

	fun unregister(name: String): Boolean {
		val key = name.lowercase()
		val renderer = fonts.remove(key) ?: return false
		val index = ordered.indexOf(renderer)
		if (index >= 0) {
			ordered.removeAt(index)
			if (index < prewarmFontIndex) prewarmFontIndex-- else if (index == prewarmFontIndex) prewarmCharIndex = 0
		}
		renderer.face.close()
		return true
	}

	internal fun init() {
		if (!NativeFont.available) {
			Logger.error("Native library unavailable")
			return
		}
		if (!fonts.containsKey("inter")) register("inter", readBundled("Inter_18pt-Regular.ttf"))
		if (!fonts.containsKey("inter-bold")) register("inter-bold", readBundled("Inter_18pt-Bold.ttf"))
	}

	private fun registerLoaded(name: String, face: NativeFontFace): FontRenderer {
		val renderer = FontRenderer(face, nextFontId++, atlas)
		fonts[name] = renderer
		ordered.add(renderer)
		return renderer
	}

	private fun readBundled(fileName: String): ByteArray {
		val stream = Fonts::class.java.getResourceAsStream("/assets/beryllium/fonts/$fileName")
		requireNotNull(stream) { "Bundled font missing: $fileName" }
		return stream.use { it.readBytes() }
	}

	internal fun prewarmStep(budgetNanos: Long) {
		if (prewarmComplete) return
		if (!NativeFont.available) {
			prewarmFontIndex = Int.MAX_VALUE
			return
		}
		val deadline = System.nanoTime() + budgetNanos
		while (prewarmFontIndex < ordered.size) {
			val renderer = ordered[prewarmFontIndex]
			while (prewarmCharIndex < prewarmCodepoints.size) {
				val codePoint = prewarmCodepoints[prewarmCharIndex]
				atlas.getMsdfGlyph(renderer.face, renderer.fontId, renderer.glyphIdFor(codePoint))
				prewarmCharIndex++
				if (System.nanoTime() >= deadline) return
			}
			prewarmFontIndex++
			prewarmCharIndex = 0
		}
	}

	private val prewarmCodepoints: IntArray by lazy {
		val chars = StringBuilder()
		for (codePoint in ' '.code..'~'.code) chars.appendCodePoint(codePoint)
		for (extra in "ĞğİıŞşÇçÖöüÜ€°•…–—‘’“”") chars.append(extra)
		chars.toString().codePoints().toArray()
	}

	internal fun uploadPending(encoder: CommandEncoder) {
		atlas.uploadPending(encoder)
	}

	internal fun close() {
		atlas.close()
		fonts.clear()
		ordered.clear()
		prewarmFontIndex = 0
		prewarmCharIndex = 0
	}

	private const val FALLBACK_ID = -1
}
