package wtf.traumadev.beryllium.ui.draw

import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.font.TextLayout
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.ui.View

class UiCanvas(private val g: Renderer2D) {
	var alpha: Float = 1f
		private set

	private class PushState(val translated: Boolean, val clipped: Boolean)

	private val pushStates = ArrayDeque<PushState>()
	private val alphaStack = ArrayDeque<Float>()
	private val offsetStackX = ArrayDeque<Float>()
	private val offsetStackY = ArrayDeque<Float>()
	private var currentOffsetX = 0f
	private var currentOffsetY = 0f

	internal fun pushView(view: View) {
		offsetStackX.addLast(currentOffsetX)
		offsetStackY.addLast(currentOffsetY)
		currentOffsetX = view.absoluteX
		currentOffsetY = view.absoluteY
		val translated = view.x != 0f || view.y != 0f
		if (translated) {
			g.pushMatrix()
			g.translate(view.x, view.y)
		}
		val clipped = view.clipContent
		if (clipped) g.pushScissor(view.absoluteX, view.absoluteY, view.width, view.height)
		pushStates.addLast(PushState(translated, clipped))
		alphaStack.addLast(alpha)
		alpha *= view.alpha.coerceIn(0f, 1f)
	}

	internal fun popView(view: View) {
		val state = pushStates.removeLast()
		if (state.clipped) g.popScissor()
		if (state.translated) g.popMatrix()
		alpha = alphaStack.removeLast()
		currentOffsetX = offsetStackX.removeLast()
		currentOffsetY = offsetStackY.removeLast()
	}

	fun pushClip(x: Float, y: Float, width: Float, height: Float) {
		g.pushScissor(currentOffsetX + x, currentOffsetY + y, width, height)
	}

	fun popClip() {
		g.popScissor()
	}

	private fun color(color: Int): Int = if (alpha >= 1f) color else Colors.multiplyAlpha(color, alpha)

	fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) {
		g.rect(x, y, width, height, color(color))
	}

	fun panel(
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		radius: Float,
		fill: Int,
		fillBottom: Int = fill,
		borderColor: Int = Colors.TRANSPARENT,
		borderWidth: Float = 0f,
		shadowColor: Int = Colors.TRANSPARENT,
		shadowOffsetX: Float = 0f,
		shadowOffsetY: Float = 0f,
		shadowSpread: Float = 0f
	) {
		g.sdfRect(x, y, width, height, radius, color(fill), color(fillBottom), color(borderColor), borderWidth, color(shadowColor), shadowOffsetX, shadowOffsetY, shadowSpread)
	}

	fun text(
		text: String,
		x: Float,
		y: Float,
		size: Float,
		color: Int = Colors.WHITE,
		font: FontRenderer = Fonts.regular,
		shadow: Boolean = false
	): Float = g.text(text, x, y, size, color(color), font, shadow)

	fun textWidth(text: String, size: Float, font: FontRenderer = Fonts.regular): Float = g.textWidth(text, size, font)

	fun wrap(text: String, font: FontRenderer, size: Float, maxWidth: Float): List<String> =
		TextLayout.wrap(text, font, size, maxWidth)

	fun image(texture: Texture2D, x: Float, y: Float, width: Float, height: Float, color: Int = Colors.WHITE) {
		g.image(texture, x, y, width, height, color(color))
	}

	fun imageRegion(
		texture: Texture2D,
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		u0: Float,
		v0: Float,
		u1: Float,
		v1: Float,
		color: Int = Colors.WHITE
	) {
		g.imageRegion(texture, x, y, width, height, u0, v0, u1, v1, color(color))
	}

	fun circle(centerX: Float, centerY: Float, radius: Float, color: Int, segments: Int = 32) {
		g.circle(centerX, centerY, radius, color(color), segments)
	}

	fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
		g.line(x1, y1, x2, y2, thickness, color(color))
	}

	fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
		g.triangle(x1, y1, x2, y2, x3, y3, color(color))
	}
}
