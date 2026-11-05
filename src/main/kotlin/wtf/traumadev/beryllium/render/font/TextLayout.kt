package wtf.traumadev.beryllium.render.font

object TextLayout {
	private data class WrapKey(val fontId: Int, val size: Float, val maxWidth: Float, val text: String)

	private const val CACHE_LIMIT = 256

	private val cache = object : LinkedHashMap<WrapKey, List<String>>(64, 0.75f, true) {
		override fun removeEldestEntry(eldest: MutableMap.MutableEntry<WrapKey, List<String>>): Boolean = size > CACHE_LIMIT
	}

	fun wrap(text: String, font: FontRenderer, size: Float, maxWidth: Float): List<String> {
		val key = WrapKey(font.fontId, size, maxWidth, text)
		cache[key]?.let { return it }
		val lines = wrapUncached(text, font, size, maxWidth)
		cache[key] = lines
		return lines
	}

	private fun wrapUncached(text: String, font: FontRenderer, size: Float, maxWidth: Float): List<String> {
		if (maxWidth <= 0f) return listOf(text)
		val lines = ArrayList<String>()
		for (hardLine in text.split('\n')) {
			if (hardLine.isEmpty()) {
				lines.add("")
				continue
			}
			var current = StringBuilder()
			for (word in hardLine.split(' ')) {
				val candidate = if (current.isEmpty()) word else "$current $word"
				if (font.textWidth(candidate, size) <= maxWidth) {
					current = StringBuilder(candidate)
					continue
				}
				if (current.isNotEmpty()) {
					lines.add(current.toString())
					current = StringBuilder()
				}
				if (font.textWidth(word, size) <= maxWidth) {
					current = StringBuilder(word)
				} else {
					var remainder = word
					while (font.textWidth(remainder, size) > maxWidth && remainder.length > 1) {
						var breakIndex = remainder.length - 1
						while (breakIndex > 1 && font.textWidth(remainder.substring(0, breakIndex), size) > maxWidth) breakIndex--
						lines.add(remainder.substring(0, breakIndex))
						remainder = remainder.substring(breakIndex)
					}
					current = StringBuilder(remainder)
				}
			}
			lines.add(current.toString())
		}
		return lines
	}
}
