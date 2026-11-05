package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.texture.ImageSource
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.ViewGroup
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Padding

class TabBar : ViewGroup() {
	private val indicatorX = SpringFloat(0f, INDICATOR_STIFFNESS, INDICATOR_DAMPING)
	private val indicatorWidth = SpringFloat(0f, INDICATOR_STIFFNESS, INDICATOR_DAMPING)
	private val tabs = mutableListOf<TabView>()
	private var indicatorInitialized = false

	var textSize: Float = 13.5f
		set(value) {
			field = value
			requestLayout()
		}
	var font: FontRenderer = Fonts.regular
	var textColor: Int = Colors.hex("#8B8F9B")
	var hoverTextColor: Int = Colors.hex("#B9BEC9")
	var selectedTextColor: Int = Colors.WHITE
	var iconTint: Int = Colors.hex("#8B8F9B")
	var hoverIconTint: Int = Colors.hex("#B9BEC9")
	var selectedIconTint: Int = Colors.WHITE
	var iconSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var iconSpacing: Float = 7f
		set(value) {
			field = value
			requestLayout()
		}
	var tabPadding: Float = 2f
		set(value) {
			field = value
			requestLayout()
		}
	var gap: Float = 18f
		set(value) {
			field = value
			requestLayout()
		}
	var padding: Padding = Padding.ZERO
		set(value) {
			field = value
			requestLayout()
		}
	var indicatorColor: Int = Colors.WHITE
	var indicatorHeight: Float = 2f
		set(value) {
			field = value
			requestLayout()
		}
	var indicatorRadius: Float = indicatorHeight / 2f
	var selected: Int = -1
		set(value) {
			field = value
			updateTabStates()
			updateIndicator(animate = true)
		}
	var onSelected: ((Int) -> Unit)? = null

	val tabCount: Int
		get() = tabs.size

	fun addTab(label: String, icon: ImageSource? = null): Int {
		val tab = TabView(tabs.size, label, icon)
		tabs.add(tab)
		addChild(tab)
		requestLayout()
		return tabs.size - 1
	}

	fun clearTabs() {
		for (tab in tabs) removeChild(tab)
		tabs.clear()
		selected = -1
		indicatorInitialized = false
		requestLayout()
	}

	private fun select(index: Int) {
		selected = index
		onSelected?.invoke(index)
	}

	private fun updateTabStates() {
		for (tab in tabs) tab.selectedFlag = tab.index == selected
	}

	private fun updateIndicator(animate: Boolean) {
		val tab = tabs.getOrNull(selected)
		if (tab == null || tab.width <= 0f) return
		val targetX = tab.x + tabPadding
		val targetWidth = (tab.width - tabPadding * 2f).coerceAtLeast(0f)
		if (!animate || !indicatorInitialized) {
			indicatorX.snapTo(targetX)
			indicatorWidth.snapTo(targetWidth)
			indicatorInitialized = true
		} else {
			indicatorX.setTarget(targetX)
			indicatorWidth.setTarget(targetWidth)
		}
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		var contentWidth = padding.horizontal
		var contentHeight = 0f
		var count = 0
		for (tab in tabs) {
			if (!tab.visible) continue
			tab.measure(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
			contentWidth += tab.width
			contentHeight = maxOf(contentHeight, tab.height)
			count++
		}
		if (count > 1) contentWidth += gap * (count - 1)
		width = resolveSize(widthSpec, availableWidth, contentWidth)
		height = resolveSize(heightSpec, availableHeight, padding.vertical + contentHeight + indicatorHeight)
	}

	override fun onArrange() {
		var cursor = padding.left
		for (tab in tabs) {
			if (!tab.visible) continue
			tab.arrange(cursor, padding.top)
			cursor += tab.width + gap
		}
		updateIndicator(animate = indicatorInitialized)
	}

	override fun onDraw(canvas: UiCanvas) {
		if (indicatorWidth.value > 0f) {
			val radius = minOf(indicatorRadius, indicatorHeight / 2f)
			canvas.panel(indicatorX.value, height - indicatorHeight, indicatorWidth.value, indicatorHeight, radius, indicatorColor)
		}
		super.onDraw(canvas)
	}

	private inner class TabView(
		val index: Int,
		private val label: String,
		private val icon: ImageSource?,
	) : View() {
		private val hoverSpring = SpringFloat(0f)
		private val pressSpring = SpringFloat(0f, stiffness = 400f, damping = 30f)
		private var selectionSpring = SpringFloat(0f)
		var selectedFlag: Boolean = false
			set(value) {
				field = value
				selectionSpring.setTarget(if (value) 1f else 0f)
			}

		override fun measure(availableWidth: Float, availableHeight: Float) {
			val image = icon?.image
			val iconWidth = when {
				image == null -> 0f
				iconSize <= 0f -> image.width.toFloat()
				image.height > 0 -> image.width * (iconSize / image.height)
				else -> 0f
			}
			val textWidth = if (label.isEmpty()) 0f else font.textWidth(label, textSize)
			width = iconWidth + (if (iconWidth > 0f && label.isNotEmpty()) iconSpacing else 0f) + textWidth + tabPadding * 2f
			height = maxOf(if (image != null) (if (iconSize > 0f) iconSize else image.height.toFloat()) else 0f, font.lineHeight(textSize))
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
			select(index)
			return true
		}

		override fun onDraw(canvas: UiCanvas) {
			val hover = hoverSpring.value.coerceIn(0f, 1f)
			val press = pressSpring.value.coerceIn(0f, 1f)
			val selection = selectionSpring.value.coerceIn(0f, 1f)
			val textBase = Colors.lerp(textColor, hoverTextColor, hover * (1f - press * 0.5f))
			val text = Colors.lerp(textBase, selectedTextColor, selection)
			val iconBase = Colors.lerp(iconTint, hoverIconTint, hover * (1f - press * 0.5f))
			val iconColor = Colors.lerp(iconBase, selectedIconTint, selection)
			val image = icon?.image
			var cursor = tabPadding
			val contentY = (height - font.lineHeight(textSize)) / 2f
			if (image != null) {
				val drawWidth = if (iconSize <= 0f) image.width.toFloat() else if (image.height > 0) image.width * (iconSize / image.height) else 0f
				val drawHeight = if (iconSize <= 0f) image.height.toFloat() else iconSize
				if (drawWidth > 0f) {
					canvas.image(image, cursor, (height - drawHeight) / 2f, drawWidth, drawHeight, iconColor)
					cursor += drawWidth + iconSpacing
				}
			}
			if (label.isNotEmpty()) {
				canvas.text(label, cursor, contentY, textSize, text, font)
			}
		}
	}

	companion object {
		private const val INDICATOR_STIFFNESS = 320f
		private const val INDICATOR_DAMPING = 26f
	}
}
