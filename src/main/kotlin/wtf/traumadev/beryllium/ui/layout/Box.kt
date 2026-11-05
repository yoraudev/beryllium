package wtf.traumadev.beryllium.ui.layout

import wtf.traumadev.beryllium.ui.ViewGroup

open class Box : ViewGroup() {
	var alignment: Alignment = Alignment.TOP_LEFT
	var padding: Padding = Padding.ZERO
		set(value) {
			field = value
			requestLayout()
		}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val innerAvailableWidth = if (availableWidth.isInfinite()) availableWidth else (availableWidth - padding.horizontal).coerceAtLeast(0f)
		val innerAvailableHeight = if (availableHeight.isInfinite()) availableHeight else (availableHeight - padding.vertical).coerceAtLeast(0f)
		var maxContentWidth = 0f
		var maxContentHeight = 0f
		for (child in children) {
			if (!child.visible) continue
			child.measure(innerAvailableWidth, innerAvailableHeight)
			maxContentWidth = maxOf(maxContentWidth, child.width)
			maxContentHeight = maxOf(maxContentHeight, child.height)
		}
		width = resolveSize(widthSpec, availableWidth, maxContentWidth + padding.horizontal)
		height = resolveSize(heightSpec, availableHeight, maxContentHeight + padding.vertical)
		if (widthSpec !is Size.Wrap || heightSpec !is Size.Wrap) {
			val offeredWidth = if (widthSpec is Size.Wrap) innerAvailableWidth else (width - padding.horizontal).coerceAtLeast(0f)
			val offeredHeight = if (heightSpec is Size.Wrap) innerAvailableHeight else (height - padding.vertical).coerceAtLeast(0f)
			maxContentWidth = 0f
			maxContentHeight = 0f
			for (child in children) {
				if (!child.visible) continue
				child.measure(offeredWidth, offeredHeight)
				maxContentWidth = maxOf(maxContentWidth, child.width)
				maxContentHeight = maxOf(maxContentHeight, child.height)
			}
			width = resolveSize(widthSpec, availableWidth, maxContentWidth + padding.horizontal)
			height = resolveSize(heightSpec, availableHeight, maxContentHeight + padding.vertical)
		}
	}

	override fun onArrange() {
		val innerWidth = (width - padding.horizontal).coerceAtLeast(0f)
		val innerHeight = (height - padding.vertical).coerceAtLeast(0f)
		for (child in children) {
			if (!child.visible) continue
			if (child.manuallyPositioned) {
				child.arrange(child.x, child.y)
				continue
			}
			val align = child.alignmentOverride ?: alignment
			val childX = when (align) {
				Alignment.TOP_LEFT, Alignment.CENTER_LEFT, Alignment.BOTTOM_LEFT -> padding.left
				Alignment.TOP_CENTER, Alignment.CENTER, Alignment.BOTTOM_CENTER -> padding.left + (innerWidth - child.width) / 2f
				Alignment.TOP_RIGHT, Alignment.CENTER_RIGHT, Alignment.BOTTOM_RIGHT -> width - padding.right - child.width
			}
			val childY = when (align) {
				Alignment.TOP_LEFT, Alignment.TOP_CENTER, Alignment.TOP_RIGHT -> padding.top
				Alignment.CENTER_LEFT, Alignment.CENTER, Alignment.CENTER_RIGHT -> padding.top + (innerHeight - child.height) / 2f
				Alignment.BOTTOM_LEFT, Alignment.BOTTOM_CENTER, Alignment.BOTTOM_RIGHT -> height - padding.bottom - child.height
			}
			child.arrange(childX, childY)
		}
	}
}
