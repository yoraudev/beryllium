package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.texture.ImageSource
import wtf.traumadev.beryllium.ui.ViewGroup
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Padding

class Button : ViewGroup() {
	private val hoverSpring = SpringFloat(0f)
	private val pressSpring = SpringFloat(0f, stiffness = 400f, damping = 30f)
	private val selectionSpring = SpringFloat(0f)
	private val iconChanged: () -> Unit = { requestLayout() }

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
	var textColor: Int = Colors.WHITE
	var selectedTextColor: Int = textColor
	var idleColor: Int = Colors.hex("#2E2E38")
	var hoverColor: Int = Colors.hex("#3C3C4A")
	var pressedColor: Int = Colors.hex("#26262E")
	var borderColor: Int = Colors.hex("#454550")
	var pressedBorderColor: Int = Colors.hex("#5B7CFF")
	var selected: Boolean = false
		set(value) {
			field = value
			selectionSpring.setTarget(if (value) 1f else 0f)
		}
	var selectedColor: Int = Colors.hex("#5B7CFF")
	var selectedBorderColor: Int = Colors.hex("#5B7CFF")
	var borderWidth: Float = 1f
	var cornerRadius: Float = 6f
	var padding: Padding = Padding(14f, 9f, 14f, 9f)
		set(value) {
			field = value
			requestLayout()
		}
	var icon: ImageSource? = null
		set(value) {
			val previous = field
			field = value
			if (previous !== value) {
				previous?.removeChangeListener(iconChanged)
				value?.addChangeListener(iconChanged)
			}
			requestLayout()
		}
	var iconSize: Float = 0f
		set(value) {
			field = value
			requestLayout()
		}
	var iconSpacing: Float = 6f
		set(value) {
			field = value
			requestLayout()
		}
	var iconTint: Int = Colors.WHITE
	var selectedIconTint: Int = iconTint
	var contentAlignment: TextAlign = TextAlign.CENTER

	var onClicked: ((Button) -> Unit)? = null

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val contentWidth = padding.horizontal + iconBoxWidth + textGap + font.textWidth(label, textSize)
		val contentHeight = padding.vertical + maxOf(iconBoxHeight, font.lineHeight(textSize))
		width = resolveSize(widthSpec, availableWidth, contentWidth)
		height = resolveSize(heightSpec, availableHeight, contentHeight)
	}

	override fun onMouseEnter() {
		hoverSpring.setTarget(1f)
	}

	override fun onMouseExit() {
		hoverSpring.setTarget(0f)
	}

	override fun onPress(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		pressSpring.setTarget(1f)
		return true
	}

	override fun onRelease(button: Int, inside: Boolean): Boolean {
		pressSpring.setTarget(0f)
		return true
	}

	override fun onClick(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
		onClicked?.invoke(this)
		return true
	}

	override fun onDraw(canvas: UiCanvas) {
		val hover = hoverSpring.value.coerceIn(0f, 1f)
		val press = pressSpring.value.coerceIn(0f, 1f)
		val selection = selectionSpring.value.coerceIn(0f, 1f)
		val base = if (selected) selectedColor else idleColor
		val baseBorder = if (selected) selectedBorderColor else borderColor
		val fill = Colors.lerp(Colors.lerp(base, hoverColor, hover), pressedColor, press * 0.6f)
		val border = Colors.lerp(baseBorder, pressedBorderColor, press)
		canvas.panel(0f, 0f, width, height, cornerRadius, fill, borderColor = border, borderWidth = borderWidth)
		val contentHeight = maxOf(iconBoxHeight, font.lineHeight(textSize))
		var cursor = when (contentAlignment) {
			TextAlign.LEFT -> padding.left
			TextAlign.CENTER -> (width - (iconBoxWidth + textGap + font.textWidth(label, textSize))) / 2f
			TextAlign.RIGHT -> width - padding.right - iconBoxWidth - textGap - font.textWidth(label, textSize)
		}
		val contentY = (height - contentHeight) / 2f
		val tint = Colors.lerp(iconTint, selectedIconTint, selection)
		val image = icon?.image
		if (image != null && iconBoxWidth > 0f && iconBoxHeight > 0f) {
			val drawY = contentY + (contentHeight - iconBoxHeight) / 2f
			canvas.image(image, cursor, drawY, iconBoxWidth, iconBoxHeight, tint)
			cursor += iconBoxWidth + textGap
		}
		val textY = contentY + (contentHeight - font.lineHeight(textSize)) / 2f
		canvas.text(label, cursor, textY, textSize, Colors.lerp(textColor, selectedTextColor, selection), font)
	}

	private val textGap: Float
		get() = if (icon != null && label.isNotEmpty()) iconSpacing else 0f

	private val iconScale: Float
		get() {
			val image = icon?.image ?: return 0f
			if (image.width <= 0 || image.height <= 0) return 0f
			return if (iconSize > 0f) iconSize / image.height else 1f
		}

	private val iconBoxWidth: Float
		get() = (icon?.image?.width ?: 0) * iconScale

	private val iconBoxHeight: Float
		get() = (icon?.image?.height ?: 0) * iconScale
}
