package wtf.traumadev.beryllium.ui.layout

import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.ViewGroup

class Flow : ViewGroup() {
	var gap: Float = 0f
	var padding: Padding = Padding.ZERO
	var flexGrow: Boolean = false
	var flexShrink: Boolean = true

	private var rows: List<List<View>> = emptyList()

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val innerWidth = (availableWidth - padding.horizontal).coerceAtLeast(0f)
		val innerHeight = (availableHeight - padding.vertical).coerceAtLeast(0f)
		val visible = children.filter { it.visible }
		for (child in visible) child.measure(innerWidth, innerHeight)
		rows = pack(visible, innerWidth)
		if (innerWidth.isFinite() && innerWidth > 0f) {
			for (row in rows) flexRow(row, innerWidth, innerHeight)
		}
		var widest = 0f
		var total = 0f
		for ((index, row) in rows.withIndex()) {
			var rowWidth = 0f
			var rowHeight = 0f
			for ((childIndex, child) in row.withIndex()) {
				if (childIndex > 0) rowWidth += gap
				rowWidth += child.width
				rowHeight = maxOf(rowHeight, child.height)
			}
			widest = maxOf(widest, rowWidth)
			if (index > 0) total += gap
			total += rowHeight
		}
		width = resolveSize(widthSpec, availableWidth, widest + padding.horizontal)
		height = resolveSize(heightSpec, availableHeight, total + padding.vertical)
	}

	override fun onArrange() {
		val top = padding.top
		var y = top
		for ((index, row) in rows.withIndex()) {
			if (index > 0) y += gap
			var x = padding.left
			var rowHeight = 0f
			for ((childIndex, child) in row.withIndex()) {
				if (childIndex > 0) x += gap
				child.arrange(x, y)
				x += child.width
				rowHeight = maxOf(rowHeight, child.height)
			}
			y += rowHeight
		}
	}

	private fun pack(visible: List<View>, innerWidth: Float): List<List<View>> {
		val packed = ArrayList<List<View>>()
		var row = ArrayList<View>()
		var x = 0f
		for (child in visible) {
			if (row.isNotEmpty() && x + child.width > innerWidth) {
				packed.add(row)
				row = ArrayList()
				x = 0f
			}
			if (row.isNotEmpty()) x += gap
			row.add(child)
			x += child.width
		}
		if (row.isNotEmpty()) packed.add(row)
		return packed
	}

	private fun flexRow(
		row: List<View>,
		innerWidth: Float,
		innerHeight: Float
	) {
		if (row.size == 1 && flexShrink && row[0].width > innerWidth) {
			resize(row[0], Size.Fixed(innerWidth), innerWidth, innerHeight)
			return
		}
		if (!flexGrow || row.isEmpty()) return
		var used = 0f
		for ((index, child) in row.withIndex()) {
			if (index > 0) used += gap
			used += child.width
		}
		val leftover = innerWidth - used
		if (leftover <= 0.5f) return
		val share = leftover / row.size
		for (child in row) {
			resize(child, Size.Fixed(child.width + share), innerWidth, innerHeight)
		}
	}

	private fun resize(
		child: View,
		spec: Size,
		innerWidth: Float,
		innerHeight: Float
	) {
		val original = child.widthSpec
		child.widthSpec = spec
		child.measure(innerWidth, innerHeight)
		child.widthSpec = original
	}
}
