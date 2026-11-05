package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class Toggle(initialChecked: Boolean = false) : View() {
	private val knobSpring = SpringFloat(0f)

	var checked: Boolean = initialChecked
		set(value) {
			field = value
			knobSpring.setTarget(if (value) 1f else 0f)
		}

	var trackWidth: Float = 36f
	var trackHeight: Float = 20f
	var trackOffColor: Int = Colors.hex("#3A3A46")
	var trackOnColor: Int = Colors.hex("#5B7CFF")
	var knobColor: Int = Colors.WHITE
	var knobOffColor: Int = knobColor
	var knobOnColor: Int = knobColor

	var onToggled: ((Boolean) -> Unit)? = null

	init {
		checked = initialChecked
		knobSpring.snapTo(if (checked) 1f else 0f)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, trackWidth)
		height = resolveSize(heightSpec, availableHeight, trackHeight)
	}

	override fun onPress(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		return true
	}

	override fun onClick(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
		checked = !checked
		onToggled?.invoke(checked)
		return true
	}

	override fun onDraw(canvas: UiCanvas) {
		val progress = knobSpring.value.coerceIn(0f, 1f)
		val fill = Colors.lerp(trackOffColor, trackOnColor, progress)
		val knob = Colors.lerp(knobOffColor, knobOnColor, progress)
		canvas.panel(0f, 0f, width, height, height / 2f, fill)
		val knobDiameter = (height - KNOB_INSET * 2f).coerceAtLeast(2f)
		val knobX = KNOB_INSET + progress * (width - KNOB_INSET * 2f - knobDiameter)
		canvas.circle(knobX + knobDiameter / 2f, height / 2f, knobDiameter / 2f, knob)
	}

	companion object {
		private const val KNOB_INSET = 2f
	}
}
