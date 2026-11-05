package wtf.traumadev.beryllium.render

object Colors {
	const val WHITE = 0xFFFFFFFF.toInt()
	const val BLACK = 0xFF000000.toInt()
	const val TRANSPARENT = 0x00000000

	fun rgba(r: Int, g: Int, b: Int, a: Int): Int = (a and 0xFF) shl 24 or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

	fun rgb(r: Int, g: Int, b: Int): Int = rgba(r, g, b, 255)

	fun alpha(color: Int): Int = color ushr 24 and 0xFF

	fun red(color: Int): Int = color shr 16 and 0xFF

	fun green(color: Int): Int = color shr 8 and 0xFF

	fun blue(color: Int): Int = color and 0xFF

	fun withAlpha(color: Int, alpha: Int): Int = alpha and 0xFF shl 24 or (color and 0x00FFFFFF)

	fun multiplyAlpha(color: Int, factor: Float): Int = withAlpha(color, (alpha(color) * factor).toInt().coerceIn(0, 255))

	fun lerp(from: Int, to: Int, t: Float): Int {
		val clamped = t.coerceIn(0f, 1f)
		return rgba(
			(red(from) + (red(to) - red(from)) * clamped).toInt(),
			(green(from) + (green(to) - green(from)) * clamped).toInt(),
			(blue(from) + (blue(to) - blue(from)) * clamped).toInt(),
			(alpha(from) + (alpha(to) - alpha(from)) * clamped).toInt()
		)
	}

	fun hex(hex: String): Int {
		val cleaned = hex.removePrefix("#")
		val value = cleaned.toLong(16)
		return when (cleaned.length) {
			6 -> (value or 0xFF000000L).toInt()
			8 -> value.toInt()
			else -> throw IllegalArgumentException("Hex color must be #RRGGBB or #AARRGGBB: $hex")
		}
	}
}
