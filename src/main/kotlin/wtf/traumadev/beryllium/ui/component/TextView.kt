package wtf.traumadev.beryllium.ui.component

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.font.TextLayout
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas

enum class TextAlign { LEFT, CENTER, RIGHT }

class TextView : View() {
	var text: String = ""
		set(value) {
			field = value
			requestLayout()
		}
	var textSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var color: Int = Colors.WHITE
	var font: FontRenderer = Fonts.regular
	var shadow: Boolean = false
	var align: TextAlign = TextAlign.LEFT
	var maxWidth: Float = 0f
		set(value) {
			field = value
			requestLayout()
		}
	var letterSpacing: Float = 0f
		set(value) {
			field = value
			requestLayout()
		}

	private var lines: List<String> = emptyList()

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val wrapWidth = when {
			letterSpacing > 0f -> Float.POSITIVE_INFINITY
			maxWidth > 0f -> maxWidth
			availableWidth.isInfinite() -> Float.POSITIVE_INFINITY
			else -> availableWidth
		}
		lines = if (wrapWidth.isInfinite()) text.split('\n') else TextLayout.wrap(text, font, textSize, wrapWidth)
		var contentWidth = 0f
		for (line in lines) contentWidth = maxOf(contentWidth, lineWidth(line))
		width = resolveSize(widthSpec, availableWidth, contentWidth)
		height = resolveSize(heightSpec, availableHeight, lines.size * font.lineHeight(textSize))
	}

	override fun onDraw(canvas: UiCanvas) {
		val lineHeight = font.lineHeight(textSize)
		var y = 0f
		for (line in lines) {
			val x = when (align) {
				TextAlign.LEFT -> 0f
				TextAlign.CENTER -> (width - lineWidth(line)) / 2f
				TextAlign.RIGHT -> width - lineWidth(line)
			}
			if (letterSpacing > 0f) {
				var cursor = x
				for (char in line) {
					canvas.text(char.toString(), cursor, y, textSize, color, font, shadow)
					cursor += font.textWidth(char.toString(), textSize) + letterSpacing
				}
			} else {
				canvas.text(line, x, y, textSize, color, font, shadow)
			}
			y += lineHeight
		}
	}

	private fun lineWidth(line: String): Float {
		if (letterSpacing <= 0f) return font.textWidth(line, textSize)
		var width = 0f
		for (char in line) width += font.textWidth(char.toString(), textSize)
		if (line.isNotEmpty()) width += letterSpacing * (line.length - 1)
		return width
	}
}
