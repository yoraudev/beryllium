package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class Slider : View() {
	var minValue: Float = 0f
	var maxValue: Float = 1f

	var value: Float = 0f
		set(value) {
			field = value.coerceIn(minValue, maxValue)
			onValueChanged?.invoke(field)
		}

	var onValueChanged: ((Float) -> Unit)? = null

	var trackHeight: Float = 4f
	var thumbRadius: Float = 6f
	var trackColor: Int = Colors.hex("#3A3A46")
	var fillColor: Int = Colors.hex("#5B7CFF")
	var thumbColor: Int = Colors.WHITE

	private var dragging = false

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 120f)
		height = resolveSize(heightSpec, availableHeight, thumbRadius * 2f)
	}

	override fun onPress(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		dragging = true
		updateFromMouse()
		return true
	}

	override fun onDrag(button: Int, deltaX: Float, deltaY: Float): Boolean {
		if (!dragging || button != InputConstants.MOUSE_BUTTON_LEFT) return true
		updateFromMouse()
		return true
	}

	override fun onRelease(button: Int, inside: Boolean): Boolean {
		dragging = false
		return true
	}

	override fun onDraw(canvas: UiCanvas) {
		val trackY = (height - trackHeight) / 2f
		val ratio = if (maxValue > minValue) (value - minValue) / (maxValue - minValue) else 0f
		val fillWidth = width * ratio
		canvas.panel(0f, trackY, width, trackHeight, trackHeight / 2f, trackColor)
		if (fillWidth > 0.5f) {
			canvas.panel(0f, trackY, fillWidth.coerceAtLeast(trackHeight), trackHeight, trackHeight / 2f, fillColor)
		}
		val thumbX = (fillWidth).coerceIn(thumbRadius, (width - thumbRadius).coerceAtLeast(thumbRadius))
		canvas.circle(thumbX, height / 2f, thumbRadius, thumbColor)
	}

	private fun updateFromMouse() {
		val owner = host ?: return
		val localX = (owner.mouseX - absoluteX).coerceIn(0f, width)
		if (width <= 0f) return
		value = minValue + (maxValue - minValue) * (localX / width)
	}
}
