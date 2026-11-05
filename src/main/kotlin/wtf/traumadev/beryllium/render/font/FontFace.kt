package wtf.traumadev.beryllium.render.font

internal class MsdfRaster(
	val width: Int,
	val height: Int,
	val bearingX: Float,
	val bearingY: Float,
	val advance: Float,
	val pixels: ByteArray
)

internal interface FontFace : AutoCloseable {
	val unitsPerEm: Int
	val ascentUnits: Int
	val descentUnits: Int
	val lineGapUnits: Int

	fun glyphForCodePoint(codePoint: Int): Int

	fun advanceUnits(glyphId: Int): Float

	fun kerningUnits(left: Int, right: Int): Float

	fun rasterizeMsdf(glyphId: Int, emPx: Float, rangePx: Float): MsdfRaster?
}

internal class NativeFontFace(private val handle: Long) : FontFace {
	private val metrics: FloatArray = NativeFont.verticalMetrics(handle)
	private val glyphIdCache = HashMap<Int, Int>()
	private val advanceCache = HashMap<Int, Float>()
	private val kerningCache = HashMap<Long, Float>()

	override val unitsPerEm: Int = NativeFont.upEm(handle)
	override val ascentUnits: Int = metrics.getOrElse(0) { 0f }.toInt()
	override val descentUnits: Int = metrics.getOrElse(1) { 0f }.toInt()
	override val lineGapUnits: Int = metrics.getOrElse(2) { 0f }.toInt()

	override fun glyphForCodePoint(codePoint: Int): Int =
		glyphIdCache.getOrPut(codePoint) { NativeFont.glyphId(handle, codePoint) }

	override fun advanceUnits(glyphId: Int): Float =
		advanceCache.getOrPut(glyphId) { NativeFont.advance(handle, glyphId) }

	override fun kerningUnits(left: Int, right: Int): Float =
		kerningCache.getOrPut(kerningKey(left, right)) { NativeFont.kerning(handle, left, right) }

	private fun kerningKey(left: Int, right: Int): Long =
		(left.toLong() shl 32) or (right.toLong() and 0xFFFFFFFFL)

	override fun rasterizeMsdf(glyphId: Int, emPx: Float, rangePx: Float): MsdfRaster? {
		val payload = NativeFont.rasterizeMsdf(handle, glyphId, emPx, rangePx) ?: return null
		if (payload.size < 20) return null
		val width = readInt(payload, 0)
		val height = readInt(payload, 4)
		val bearingX = readFloat(payload, 8)
		val bearingY = readFloat(payload, 12)
		val advance = readFloat(payload, 16)
		val pixelCount = width * height * 3
		if (width <= 0 || height <= 0 || payload.size < 20 + pixelCount) return null
		val pixels = payload.copyOfRange(20, 20 + pixelCount)
		return MsdfRaster(width, height, bearingX, bearingY, advance, pixels)
	}

	override fun close() = NativeFont.freeFont(handle)

	private fun readInt(data: ByteArray, offset: Int): Int =
		(data[offset].toInt() and 0xFF) or
			((data[offset + 1].toInt() and 0xFF) shl 8) or
			((data[offset + 2].toInt() and 0xFF) shl 16) or
			((data[offset + 3].toInt() and 0xFF) shl 24)

	private fun readFloat(data: ByteArray, offset: Int): Float =
		Float.fromBits(readInt(data, offset))
}

internal object NullFontFace : FontFace {
	override val unitsPerEm: Int = 100
	override val ascentUnits: Int = 0
	override val descentUnits: Int = 0
	override val lineGapUnits: Int = 0

	override fun glyphForCodePoint(codePoint: Int): Int = -1

	override fun advanceUnits(glyphId: Int): Float = 0f

	override fun kerningUnits(left: Int, right: Int): Float = 0f

	override fun rasterizeMsdf(glyphId: Int, emPx: Float, rangePx: Float): MsdfRaster? = null

	override fun close() {}
}
