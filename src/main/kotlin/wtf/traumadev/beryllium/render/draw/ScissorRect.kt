package wtf.traumadev.beryllium.render.draw

internal data class ScissorRect(val x: Int, val y: Int, val width: Int, val height: Int) {
	val isEmpty: Boolean
		get() = width <= 0 || height <= 0

	fun intersect(other: ScissorRect): ScissorRect {
		val left = maxOf(x, other.x)
		val top = maxOf(y, other.y)
		val right = minOf(x + width, other.x + other.width)
		val bottom = minOf(y + height, other.y + other.height)
		return ScissorRect(left, top, right - left, bottom - top)
	}
}
