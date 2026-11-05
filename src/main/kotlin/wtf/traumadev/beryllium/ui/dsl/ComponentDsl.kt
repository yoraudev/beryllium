package wtf.traumadev.beryllium.ui.dsl

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.svg.SvgImage
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.component.Button
import wtf.traumadev.beryllium.ui.component.Checkbox
import wtf.traumadev.beryllium.ui.component.Dropdown
import wtf.traumadev.beryllium.ui.component.ImageView
import wtf.traumadev.beryllium.ui.component.Panel
import wtf.traumadev.beryllium.ui.component.ProgressBar
import wtf.traumadev.beryllium.ui.component.Slider
import wtf.traumadev.beryllium.ui.component.TabBar
import wtf.traumadev.beryllium.ui.component.TextAlign
import wtf.traumadev.beryllium.ui.component.TextField
import wtf.traumadev.beryllium.ui.component.TextView
import wtf.traumadev.beryllium.ui.component.Toggle
import wtf.traumadev.beryllium.ui.component.Window
import wtf.traumadev.beryllium.ui.layout.Alignment
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Size

fun UiScope.panel(
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	radius: Float = 8f,
	fill: Int = Colors.hex("#1A1A22"),
	fillBottom: Int = fill,
	borderColor: Int = Colors.TRANSPARENT,
	borderWidth: Float = 0f,
	shadowColor: Int = Colors.TRANSPARENT,
	shadowOffsetX: Float = 0f,
	shadowOffsetY: Float = 0f,
	shadowSpread: Float = 0f,
	alignment: Alignment = Alignment.TOP_LEFT,
	block: (UiScope.() -> Unit)? = null
): Panel {
	val panel = Panel()
	panel.widthSpec = width
	panel.heightSpec = height
	panel.cornerRadius = radius
	panel.fill = fill
	panel.fillBottom = fillBottom
	panel.borderColor = borderColor
	panel.borderWidth = borderWidth
	panel.shadowColor = shadowColor
	panel.shadowOffsetX = shadowOffsetX
	panel.shadowOffsetY = shadowOffsetY
	panel.shadowSpread = shadowSpread
	panel.alignment = alignment
	add(panel)
	if (block != null) UiScope(panel).apply(block)
	return panel
}

fun UiScope.text(
	label: String,
	size: Float = 14f,
	color: Int = Colors.WHITE,
	font: FontRenderer = Fonts.regular,
	shadow: Boolean = false,
	align: TextAlign = TextAlign.LEFT,
	maxWidth: Float = 0f,
	width: Size = Size.Wrap,
	height: Size = Size.Wrap
): TextView {
	val view = TextView()
	view.text = label
	view.textSize = size
	view.color = color
	view.font = font
	view.shadow = shadow
	view.align = align
	view.maxWidth = maxWidth
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.image(
	texture: Texture2D,
	color: Int = Colors.WHITE,
	width: Size = Size.Wrap,
	height: Size = Size.Wrap
): ImageView {
	val view = ImageView()
	view.source = texture
	view.color = color
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.svg(
	svg: SvgImage,
	color: Int = Colors.WHITE,
	width: Size = Size.Wrap,
	height: Size = Size.Wrap
): ImageView {
	val view = ImageView()
	view.source = svg
	view.color = color
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.button(
	label: String,
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	textSize: Float = 14f,
	onClick: (Button) -> Unit = {}
): Button {
	val view = Button()
	view.label = label
	view.textSize = textSize
	view.widthSpec = width
	view.heightSpec = height
	view.onClicked = onClick
	add(view)
	return view
}

fun UiScope.tabBar(
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	gap: Float = 18f,
	padding: Padding = Padding.ZERO,
	textSize: Float = 13.5f,
	indicatorColor: Int = Colors.WHITE,
	indicatorHeight: Float = 2f,
	onSelected: ((Int) -> Unit)? = null
): TabBar {
	val view = TabBar()
	view.widthSpec = width
	view.heightSpec = height
	view.gap = gap
	view.padding = padding
	view.textSize = textSize
	view.indicatorColor = indicatorColor
	view.indicatorHeight = indicatorHeight
	view.onSelected = onSelected
	add(view)
	return view
}

fun UiScope.toggle(
	checked: Boolean = false,
	width: Size = Size.Wrap,
	height: Size = Size.Wrap,
	onToggled: ((Boolean) -> Unit)? = null
): Toggle {
	val view = Toggle(checked)
	view.onToggled = onToggled
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.checkbox(
	checked: Boolean = false,
	label: String = "",
	onToggled: ((Boolean) -> Unit)? = null
): Checkbox {
	val view = Checkbox(checked)
	view.label = label
	view.onToggled = onToggled
	add(view)
	return view
}

fun UiScope.slider(
	min: Float = 0f,
	max: Float = 1f,
	initial: Float = 0f,
	width: Size = Size.Fill(),
	height: Size = Size.Wrap,
	onValueChanged: ((Float) -> Unit)? = null
): Slider {
	val view = Slider()
	view.minValue = min
	view.maxValue = max
	view.value = initial
	view.onValueChanged = onValueChanged
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.progressBar(
	progress: Float = 0f,
	width: Size = Size.Fill(),
	height: Size = Size.Fixed(8f)
): ProgressBar {
	val view = ProgressBar()
	view.widthSpec = width
	view.heightSpec = height
	view.progress = progress
	add(view)
	return view
}

fun UiScope.textField(
	hint: String = "",
	width: Size = Size.Fill(),
	height: Size = Size.Fixed(28f),
	textSize: Float = 14f,
	onSubmitted: ((String) -> Unit)? = null,
	onTextChanged: ((String) -> Unit)? = null
): TextField {
	val view = TextField()
	view.hint = hint
	view.widthSpec = width
	view.heightSpec = height
	view.textSize = textSize
	view.onSubmitted = onSubmitted
	view.onTextChanged = onTextChanged
	add(view)
	return view
}

fun UiScope.dropdown(
	options: List<String>,
	selected: Int = -1,
	width: Size = Size.Wrap,
	height: Size = Size.Fixed(28f),
	onSelected: ((Int, String) -> Unit)? = null
): Dropdown {
	val view = Dropdown()
	view.options = options
	view.selected = selected
	view.onSelected = onSelected
	view.widthSpec = width
	view.heightSpec = height
	add(view)
	return view
}

fun UiScope.window(
	title: String,
	x: Float? = null,
	y: Float? = null,
	width: Size = Size.Fixed(300f),
	height: Size = Size.Wrap,
	gap: Float = 8f,
	alignment: Alignment = Alignment.CENTER,
	screenMargin: Float = 0f,
	titleBar: Boolean = true,
	draggable: Boolean = true,
	scrollContent: Boolean = true,
	shadowColor: Int = Colors.withAlpha(Colors.BLACK, 110),
	shadowOffsetX: Float = 0f,
	shadowOffsetY: Float = 4f,
	shadowSpread: Float = 16f,
	block: (UiScope.() -> Unit)? = null
): Window {
	val view = Window()
	view.title = title
	view.widthSpec = width
	view.heightSpec = height
	view.screenMargin = screenMargin
	view.titleBar = titleBar
	view.draggable = draggable
	view.scrollContent = scrollContent
	view.shadowColor = shadowColor
	view.shadowOffsetX = shadowOffsetX
	view.shadowOffsetY = shadowOffsetY
	view.shadowSpread = shadowSpread
	view.viewport.gap = gap
	if (x != null || y != null) {
		view.manuallyPositioned = true
		view.x = x ?: 0f
		view.y = y ?: 0f
	} else {
		view.alignmentOverride = alignment
	}
	add(view)
	if (block != null) UiScope(if (view.scrollContent) view.viewport else view).apply(block)
	return view
}

fun View.withTooltip(provider: () -> String): View {
	tooltipProvider = provider
	return this
}
