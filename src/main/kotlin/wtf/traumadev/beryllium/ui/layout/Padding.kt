package wtf.traumadev.beryllium.ui.layout

data class Padding(val left: Float, val top: Float, val right: Float, val bottom: Float) {
	constructor(all: Float) : this(all, all, all, all)

	val horizontal: Float
		get() = left + right

	val vertical: Float
		get() = top + bottom

	companion object {
		val ZERO = Padding(0f)
	}
}
