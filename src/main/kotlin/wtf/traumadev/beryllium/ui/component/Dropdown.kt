package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Column
import wtf.traumadev.beryllium.ui.layout.Size
import wtf.traumadev.beryllium.ui.layout.ScrollView

class Dropdown : View() {
	var options: List<String> = emptyList()
		set(value) {
			field = value
			requestLayout()
		}
	var selected: Int = -1
	var onSelected: ((Int, String) -> Unit)? = null

	var hint: String = "Select"
	var textSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var font: FontRenderer = Fonts.regular
	var textColor: Int = Colors.WHITE
	var hintColor: Int = Colors.withAlpha(Colors.WHITE, 110)
	var backgroundColor: Int = Colors.hex("#1A1A22")
	var borderColor: Int = Colors.hex("#3C3C46")
	var optionHoverColor: Int = Colors.hex("#2E2E3C")
	var cornerRadius: Float = 6f
	var borderWidth: Float = 1f
	var optionHeight: Float = 26f
	var maxVisible: Int = 8

	private var popup: Panel? = null
	private var barrier: View? = null

	val isOpen: Boolean
		get() = popup != null

	val selectedOption: String?
		get() = options.getOrNull(selected)

	init {
		heightSpec = Size.Fixed(28f)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		var contentWidth = font.textWidth(selectedOption ?: hint, textSize)
		for (option in options) contentWidth = maxOf(contentWidth, font.textWidth(option, textSize))
		width = resolveSize(widthSpec, availableWidth, contentWidth + LABEL_PADDING * 2f + CHEVRON_SPACE)
		height = resolveSize(heightSpec, availableHeight, font.lineHeight(textSize) + 12f)
	}

	override fun onPress(button: Int): Boolean = true

	override fun onClick(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
		if (isOpen) close() else open()
		return true
	}

	private fun open() {
		val owner = host ?: return
		close()
		barrier = object : View() {
			init {
				widthSpec = Size.Fill()
				heightSpec = Size.Fill()
			}

			override fun onPress(button: Int): Boolean {
				close()
				return true
			}

			override fun onEscape(): Boolean {
				close()
				return true
			}
		}
		val popupWidth = maxOf(width, options.maxOfOrNull { font.textWidth(it, textSize) } ?: 0f) + LABEL_PADDING * 2f + CHEVRON_SPACE
		val visibleCount = options.size.coerceAtMost(maxVisible)
		val popupHeight = visibleCount * optionHeight + POPUP_PADDING * 2f
		val panel = Panel().apply {
			widthSpec = Size.Fixed(popupWidth)
			heightSpec = Size.Fixed(popupHeight)
			fill = backgroundColor
			borderColor = this@Dropdown.borderColor
			borderWidth = this@Dropdown.borderWidth
			cornerRadius = this@Dropdown.cornerRadius
			shadowColor = Colors.withAlpha(Colors.BLACK, 120)
			shadowOffsetY = 4f
			shadowSpread = 10f
			manuallyPositioned = true
		}
		if (options.size > maxVisible) {
			val scroll = ScrollView().apply {
				widthSpec = Size.Fixed(popupWidth - POPUP_PADDING * 2f)
				heightSpec = Size.Fixed(popupHeight - POPUP_PADDING * 2f)
				scrollSpeed = this@Dropdown.optionHeight
				manuallyPositioned = true
				x = POPUP_PADDING
				y = POPUP_PADDING
			}
			for (index in options.indices) scroll.addChild(optionRow(index))
			panel.addChild(scroll)
		} else {
			val column = Column().apply {
				widthSpec = Size.Fill()
				heightSpec = Size.Wrap
			}
			for (index in options.indices) column.addChild(optionRow(index))
			panel.addChild(column)
		}
		barrier?.let { owner.addOverlay(it) }
		owner.addOverlay(panel)
		panel.x = absoluteX.coerceIn(0f, (Renderer.gui.guiWidth - popupWidth).coerceAtLeast(0f))
		panel.y = absoluteY + height + 4f
		if (panel.y + popupHeight > Renderer.gui.guiHeight) panel.y = (absoluteY - popupHeight - 4f).coerceAtLeast(0f)
		popup = panel
	}

	private fun optionRow(index: Int): DropdownOption =
		DropdownOption(
			options[index],
			selected == index,
			optionHeight,
			textSize,
			font,
			optionHoverColor,
			textColor
		) {
			selected = index
			onSelected?.invoke(index, options[index])
			close()
		}

	private fun close() {
		val owner = host
		barrier?.let { owner?.removeOverlay(it) }
		popup?.let { owner?.removeOverlay(it) }
		barrier = null
		popup = null
	}

	override fun onDraw(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, cornerRadius, backgroundColor, borderColor = borderColor, borderWidth = borderWidth)
		val label = selectedOption ?: hint
		val labelColor = if (selectedOption != null) textColor else hintColor
		val textY = (height - font.lineHeight(textSize)) / 2f
		canvas.text(label, LABEL_PADDING, textY, textSize, labelColor, font)
		val chevronX = width - CHEVRON_SPACE / 2f - 4f
		val chevronY = height / 2f
		if (isOpen) {
			canvas.triangle(chevronX - 4f, chevronY + 1.5f, chevronX + 4f, chevronY + 1.5f, chevronX, chevronY - 2.5f, labelColor)
		} else {
			canvas.triangle(chevronX - 4f, chevronY - 1.5f, chevronX + 4f, chevronY - 1.5f, chevronX, chevronY + 2.5f, labelColor)
		}
	}

	companion object {
		private const val LABEL_PADDING = 10f
		private const val CHEVRON_SPACE = 14f
		private const val POPUP_PADDING = 6f
	}
}

private class DropdownOption(
	private val optionLabel: String,
	private val isSelected: Boolean,
	private val rowHeight: Float,
	private val rowTextSize: Float,
	private val rowFont: FontRenderer,
	private val hoverColor: Int,
	private val textColor: Int,
	private val onPick: () -> Unit
) : View() {
	private var hovered = false

	init {
		widthSpec = Size.Fill()
		heightSpec = Size.Fixed(rowHeight)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, rowFont.textWidth(optionLabel, rowTextSize) + 20f)
		height = resolveSize(heightSpec, availableHeight, rowHeight)
	}

	override fun onMouseEnter() {
		hovered = true
	}

	override fun onMouseExit() {
		hovered = false
	}

	override fun onPress(button: Int): Boolean = true

	override fun onClick(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
		onPick()
		return true
	}

	override fun onDraw(canvas: UiCanvas) {
		if (hovered || isSelected) {
			canvas.panel(4f, 2f, width - 8f, height - 4f, 4f, hoverColor)
		}
		canvas.text(optionLabel, LABEL_PADDING, (height - rowFont.lineHeight(rowTextSize)) / 2f, rowTextSize, textColor, rowFont)
	}

	companion object {
		private const val LABEL_PADDING = 10f
	}
}
