package wtf.traumadev.beryllium.ui.layout

import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.ViewGroup

enum class Orientation { ROW, COLUMN }

abstract class LinearLayout(orientation: Orientation) : ViewGroup() {
	var gap: Float = 0f
	var padding: Padding = Padding.ZERO
	var mainAlignment: MainAlignment = MainAlignment.START
	var crossAlignment: CrossAlignment = CrossAlignment.START

	private val isColumn: Boolean = orientation == Orientation.COLUMN
	private var lastMainAvailable: Float = 0f

	private fun mainSizeOf(view: View): Float = if (isColumn) view.height else view.width

	private fun crossSizeOf(view: View): Float = if (isColumn) view.width else view.height

	private fun mainSpecOf(view: View): Size = if (isColumn) view.heightSpec else view.widthSpec

	private fun crossSpecOf(view: View): Size = if (isColumn) view.widthSpec else view.heightSpec

	private fun mainPadding(): Float = if (isColumn) padding.vertical else padding.horizontal

	private fun crossPadding(): Float = if (isColumn) padding.horizontal else padding.vertical

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val mainAvailable = (if (isColumn) availableHeight else availableWidth) - mainPadding()
		val crossAvailable = (if (isColumn) availableWidth else availableHeight) - crossPadding()
		lastMainAvailable = mainAvailable
		val resolvedCross = when (val crossSpec = if (isColumn) widthSpec else heightSpec) {
			is Size.Fixed -> crossSpec.value - crossPadding()
			is Size.Fill -> if (crossAvailable.isInfinite()) crossAvailable else crossAvailable * crossSpec.fraction
			Size.Wrap -> crossAvailable
		}

		val visible = children.filter { it.visible }
		var wrappedMain = 0f
		var count = 0
		var maxCross = 0f
		val fills = ArrayList<View>()
		for (child in visible) {
			if (mainSpecOf(child) is Size.Fill) {
				fills.add(child)
				continue
			}
			child.measure(
				if (isColumn) resolvedCross else mainAvailable,
				if (isColumn) mainAvailable else resolvedCross,
			)
			wrappedMain += mainSizeOf(child)
			count++
			maxCross = maxOf(maxCross, crossSizeOf(child))
		}
		count += fills.size
		val wrapGapTotal = if (count > 0) gap * (count - 1) else 0f
		val free = (mainAvailable - wrappedMain - wrapGapTotal).coerceAtLeast(0f)
		var fillMain = 0f
		for (child in fills) {
			val offer = if (mainAvailable.isInfinite()) mainAvailable else free
			child.measure(
				if (isColumn) resolvedCross else offer,
				if (isColumn) offer else resolvedCross,
			)
			fillMain += mainSizeOf(child)
			maxCross = maxOf(maxCross, crossSizeOf(child))
		}
		val contentMain = wrappedMain + fillMain + (if (count > 0) gap * (count - 1) else 0f)
		val contentWidth = (if (isColumn) maxCross else contentMain) + padding.horizontal
		val contentHeight = (if (isColumn) contentMain else maxCross) + padding.vertical
		width = resolveSize(widthSpec, availableWidth, contentWidth)
		height = resolveSize(heightSpec, availableHeight, contentHeight)
	}

	override fun onArrange() {
		val visible = children.filter { it.visible }
		if (visible.isEmpty()) return
		val mainLength = (if (isColumn) height else width) - mainPadding()
		val crossLength = (if (isColumn) width else height) - crossPadding()

		var used = 0f
		for (child in visible) used += mainSizeOf(child)

		val baseGapTotal = gap * (visible.size - 1)
		var cursor = 0f
		var spacing = gap
		when (mainAlignment) {
			MainAlignment.START -> {}
			MainAlignment.CENTER -> cursor = (mainLength - used - baseGapTotal).coerceAtLeast(0f) / 2f
			MainAlignment.END -> cursor = (mainLength - used - baseGapTotal).coerceAtLeast(0f)
			MainAlignment.SPACE_BETWEEN -> spacing = if (visible.size > 1) ((mainLength - used) / (visible.size - 1)).coerceAtLeast(0f) else 0f
			MainAlignment.SPACE_AROUND -> spacing = if (visible.isNotEmpty()) ((mainLength - used) / visible.size).coerceAtLeast(0f) else 0f
		}
		if (mainAlignment == MainAlignment.SPACE_AROUND) cursor = spacing / 2f

		for (child in visible) {
			if (crossAlignment == CrossAlignment.STRETCH && crossSpecOf(child) == Size.Wrap) {
				child.measure(
					if (isColumn) crossLength else lastMainAvailable,
					if (isColumn) lastMainAvailable else crossLength,
				)
			}
			val crossPos = when (crossAlignment) {
				CrossAlignment.START, CrossAlignment.STRETCH -> 0f
				CrossAlignment.CENTER -> (crossLength - crossSizeOf(child)) / 2f
				CrossAlignment.END -> crossLength - crossSizeOf(child)
			}
			if (isColumn) child.arrange(padding.left + crossPos, padding.top + cursor)
			else child.arrange(padding.left + cursor, padding.top + crossPos)
			cursor += mainSizeOf(child) + spacing
		}
	}
}
