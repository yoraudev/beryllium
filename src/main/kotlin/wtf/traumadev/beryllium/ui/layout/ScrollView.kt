package wtf.traumadev.beryllium.ui.layout

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.ViewGroup
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class ScrollView : ViewGroup() {
	var gap: Float = 0f
	var padding: Padding = Padding.ZERO
		set(value) {
			field = value
			requestLayout()
		}
	var scrollSpeed: Float = 32f
	var thumbWidth: Float = THUMB_WIDTH
	var thumbColor: Int = Colors.withAlpha(Colors.WHITE, 60)
	var alwaysShowScrollbar: Boolean = false

	var scrollY: Float = 0f
		private set
	var maxScroll: Float = 0f
		private set
	var contentWidth: Float = 0f
		private set
	var contentHeight: Float = 0f
		private set

	val scrollable: Boolean
		get() = maxScroll > 0f

	init {
		clipContent = true
	}

	fun scrollTo(target: Float) {
		val clamped = target.coerceIn(0f, maxScroll)
		if (clamped == scrollY) return
		scrollY = clamped
		requestLayout()
	}

	fun scrollBy(delta: Float) {
		scrollTo(scrollY + delta)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 0f)
		height = resolveSize(heightSpec, availableHeight, 0f)
		val innerWidth = (width - padding.horizontal).coerceAtLeast(0f)
		val innerHeight = (height - padding.vertical).coerceAtLeast(0f)
		val visible = children.filter { it.visible }
		measureChildren(visible, innerWidth)
		var total = totalHeight(visible)
		contentWidth = widest(visible)
		if ((total > innerHeight || alwaysShowScrollbar) && innerWidth >= SCROLLBAR_WIDTH) {
			measureChildren(visible, innerWidth - SCROLLBAR_WIDTH)
			total = totalHeight(visible)
			contentWidth = widest(visible)
		}
		contentHeight = total
		maxScroll = (total - innerHeight).coerceAtLeast(0f)
	}

	override fun onArrange() {
		var y = padding.top - scrollY.coerceIn(0f, maxScroll)
		for (child in children) {
			if (!child.visible) continue
			child.arrange(padding.left, y)
			y += child.height + gap
		}
	}

	override fun onScroll(deltaX: Double, deltaY: Double): Boolean {
		if (!scrollable) return false
		scrollBy((-deltaY * scrollSpeed).toFloat())
		return true
	}

	override fun drawContent(canvas: UiCanvas) {
		super.drawContent(canvas)
		drawScrollbar(canvas)
	}

	private fun measureChildren(visible: List<View>, availableWidth: Float) {
		for (child in visible) child.measure(availableWidth, Float.POSITIVE_INFINITY)
	}

	private fun totalHeight(visible: List<View>): Float {
		var total = 0f
		for (child in visible) total += child.height
		if (visible.size > 1) total += gap * (visible.size - 1)
		return total
	}

	private fun widest(visible: List<View>): Float {
		var max = 0f
		for (child in visible) max = maxOf(max, child.width)
		return max
	}

	private fun drawScrollbar(canvas: UiCanvas) {
		if (maxScroll <= 0f && !alwaysShowScrollbar) return
		val trackHeight = (height - padding.vertical).coerceAtLeast(0f)
		val scroll = scrollY.coerceIn(0f, maxScroll)
		val thumbHeight = if (maxScroll <= 0f) trackHeight else (trackHeight * trackHeight / (maxScroll + trackHeight)).coerceAtLeast(MIN_THUMB)
		val thumbY = padding.top + if (maxScroll <= 0f || thumbHeight >= trackHeight) 0f else (trackHeight - thumbHeight) * (scroll / maxScroll)
		val tw = thumbWidth.coerceIn(0f, SCROLLBAR_WIDTH)
		val thumbX = width - padding.right - SCROLLBAR_WIDTH + (SCROLLBAR_WIDTH - tw) / 2f
		canvas.panel(thumbX, thumbY, tw, thumbHeight, tw / 2f, thumbColor)
	}

	companion object {
		private const val SCROLLBAR_WIDTH = 8f
		private const val THUMB_WIDTH = 4f
		private const val MIN_THUMB = 16f
	}
}
