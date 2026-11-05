package wtf.traumadev.beryllium.ui.component

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Padding

class TooltipView(text: String) : View() {
	var text: String = text
		set(value) {
			field = value
			requestLayout()
		}
	var textSize: Float = 13f
	var font: FontRenderer = Fonts.regular
	var textColor: Int = Colors.withAlpha(Colors.WHITE, 230)
	var backgroundColor: Int = Colors.hex("#EE14141B")
	var cornerRadius: Float = 6f
	var padding: Padding = Padding(8f, 5f, 8f, 5f)

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, font.textWidth(text, textSize) + padding.horizontal)
		height = resolveSize(heightSpec, availableHeight, font.lineHeight(textSize) + padding.vertical)
	}

	override fun onDraw(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, cornerRadius, backgroundColor)
		canvas.text(text, padding.left, padding.top, textSize, textColor, font)
	}
}
