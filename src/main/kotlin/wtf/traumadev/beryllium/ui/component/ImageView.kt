package wtf.traumadev.beryllium.ui.component

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.texture.ImageSource
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas

class ImageView : View() {
	private val sourceChanged: () -> Unit = { requestLayout() }

	var source: ImageSource? = null
		set(value) {
			val previous = field
			field = value
			if (previous !== value) {
				previous?.removeChangeListener(sourceChanged)
				value?.addChangeListener(sourceChanged)
			}
			requestLayout()
		}
	var color: Int = Colors.WHITE

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val current = source?.image
		width = resolveSize(widthSpec, availableWidth, current?.width?.toFloat() ?: 0f)
		height = resolveSize(heightSpec, availableHeight, current?.height?.toFloat() ?: 0f)
	}

	override fun onDraw(canvas: UiCanvas) {
		val current = source?.image ?: return
		canvas.image(current, 0f, 0f, width, height, color)
	}
}
