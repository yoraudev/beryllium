package wtf.traumadev.beryllium.ui.component

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Box

open class Panel : Box() {
	var fill: Int = Colors.hex("#1A1A22")
	private var explicitFillBottom: Int? = null
	var fillBottom: Int
		get() = explicitFillBottom ?: fill
		set(value) {
			explicitFillBottom = value
		}
	var borderColor: Int = Colors.TRANSPARENT
	var borderWidth: Float = 0f
	var cornerRadius: Float = 8f
	var shadowColor: Int = Colors.TRANSPARENT
	var shadowOffsetX: Float = 0f
	var shadowOffsetY: Float = 0f
	var shadowSpread: Float = 0f

	override fun onDraw(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, cornerRadius, fill, fillBottom, borderColor, borderWidth, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread)
		super.onDraw(canvas)
	}
}
