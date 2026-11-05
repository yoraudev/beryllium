package wtf.traumadev.beryllium.ui.dsl

import wtf.traumadev.beryllium.ui.ViewGroup
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.layout.Alignment
import wtf.traumadev.beryllium.ui.layout.Box
import wtf.traumadev.beryllium.ui.layout.Column
import wtf.traumadev.beryllium.ui.layout.CrossAlignment
import wtf.traumadev.beryllium.ui.layout.MainAlignment
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Row
import wtf.traumadev.beryllium.ui.layout.ScrollView
import wtf.traumadev.beryllium.ui.layout.Size

class UiScope internal constructor(internal val container: ViewGroup) {
	fun add(view: View) {
		container.addChild(view)
	}
}

fun ui(block: UiScope.() -> Unit): ViewGroup {
	val root = Box()
	root.widthSpec = Size.Fill()
	root.heightSpec = Size.Fill()
	root.interceptsInput = false
	UiScope(root).apply(block)
	return root
}

fun UiScope.column(
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	gap: Float = 0f,
	padding: Padding = Padding.ZERO,
	mainAlignment: MainAlignment = MainAlignment.START,
	crossAlignment: CrossAlignment = CrossAlignment.START,
	block: (UiScope.() -> Unit)? = null
): Column {
	val column = Column()
	column.widthSpec = width
	column.heightSpec = height
	column.gap = gap
	column.padding = padding
	column.mainAlignment = mainAlignment
	column.crossAlignment = crossAlignment
	add(column)
	if (block != null) UiScope(column).apply(block)
	return column
}

fun UiScope.row(
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	gap: Float = 0f,
	padding: Padding = Padding.ZERO,
	mainAlignment: MainAlignment = MainAlignment.START,
	crossAlignment: CrossAlignment = CrossAlignment.START,
	block: (UiScope.() -> Unit)? = null
): Row {
	val row = Row()
	row.widthSpec = width
	row.heightSpec = height
	row.gap = gap
	row.padding = padding
	row.mainAlignment = mainAlignment
	row.crossAlignment = crossAlignment
	add(row)
	if (block != null) UiScope(row).apply(block)
	return row
}

fun UiScope.box(
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	alignment: Alignment = Alignment.TOP_LEFT,
	block: (UiScope.() -> Unit)? = null
): Box {
	val box = Box()
	box.widthSpec = width
	box.heightSpec = height
	box.alignment = alignment
	add(box)
	if (block != null) UiScope(box).apply(block)
	return box
}

fun UiScope.scroll(
	width: Size = Size.Fill(),
	height: Size = Size.Fill(),
	gap: Float = 0f,
	scrollSpeed: Float = 32f,
	block: (UiScope.() -> Unit)? = null
): ScrollView {
	val scrollView = ScrollView()
	scrollView.widthSpec = width
	scrollView.heightSpec = height
	scrollView.gap = gap
	scrollView.scrollSpeed = scrollSpeed
	add(scrollView)
	if (block != null) UiScope(scrollView).apply(block)
	return scrollView
}
