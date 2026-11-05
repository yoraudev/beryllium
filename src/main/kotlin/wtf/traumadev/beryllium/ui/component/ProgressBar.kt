package wtf.traumadev.beryllium.ui.component

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class ProgressBar : View() {
	private val fillSpring = SpringFloat(0f)

	var progress: Float = 0f
		set(value) {
			field = value.coerceIn(0f, 1f)
			fillSpring.setTarget(field)
		}

	var trackColor: Int = Colors.hex("#2E2E38")
	var fillColor: Int = Colors.hex("#5B7CFF")
	var cornerRadius: Float = -1f

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 160f)
		height = resolveSize(heightSpec, availableHeight, 8f)
	}

	override fun onDraw(canvas: UiCanvas) {
		val radius = if (cornerRadius >= 0f) cornerRadius else height / 2f
		canvas.panel(0f, 0f, width, height, radius, trackColor)
		val fillWidth = width * fillSpring.value.coerceIn(0f, 1f)
		if (fillWidth > 0.5f) {
			canvas.panel(0f, 0f, fillWidth.coerceAtLeast(height), height, radius, fillColor)
		}
	}
}
