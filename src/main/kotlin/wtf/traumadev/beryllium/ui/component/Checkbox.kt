package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class Checkbox(initialChecked: Boolean = false) : View() {
	private val checkSpring = SpringFloat(0f)

	var checked: Boolean = initialChecked
		set(value) {
			field = value
			checkSpring.setTarget(if (value) 1f else 0f)
		}

	var label: String = ""
		set(value) {
			field = value
			requestLayout()
		}
	var textSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var font: FontRenderer = Fonts.regular
	var boxSize: Float = 16f
	var labelColor: Int = Colors.WHITE
	var boxIdleColor: Int = Colors.hex("#2E2E38")
	var boxCheckedColor: Int = Colors.hex("#5B7CFF")
	var boxBorderColor: Int = Colors.hex("#454550")
	var checkColor: Int = Colors.WHITE

	var onToggled: ((Boolean) -> Unit)? = null

	init {
		checked = initialChecked
		checkSpring.snapTo(if (checked) 1f else 0f)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		var contentWidth = boxSize
		if (label.isNotEmpty()) contentWidth += LABEL_GAP + font.textWidth(label, textSize)
		val contentHeight = maxOf(boxSize, font.lineHeight(textSize))
		width = resolveSize(widthSpec, availableWidth, contentWidth)
		height = resolveSize(heightSpec, availableHeight, contentHeight)
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
		val progress = checkSpring.value.coerceIn(0f, 1f)
		val fill = Colors.lerp(boxIdleColor, boxCheckedColor, progress)
		val boxY = (height - boxSize) / 2f
		canvas.panel(0f, boxY, boxSize, boxSize, 4f, fill, borderColor = boxBorderColor, borderWidth = 1f)
		if (progress > 0.5f) {
			val inset = boxSize * 0.24f
			val thickness = (boxSize * 0.14f).coerceAtLeast(1.5f)
			canvas.line(inset, boxY + boxSize * 0.52f, boxSize * 0.42f, boxY + boxSize - inset, thickness, checkColor)
			canvas.line(boxSize * 0.42f, boxY + boxSize - inset, boxSize - inset, boxY + inset, thickness, checkColor)
		}
		if (label.isNotEmpty()) {
			val textY = (height - font.lineHeight(textSize)) / 2f
			canvas.text(label, boxSize + LABEL_GAP, textY, textSize, labelColor, font)
		}
	}

	companion object {
		private const val LABEL_GAP = 7f
	}
}
