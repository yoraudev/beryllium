package wtf.traumadev.beryllium.render.texture

interface ImageSource {
	val image: Texture2D?

	fun addChangeListener(listener: () -> Unit) {}
	fun removeChangeListener(listener: () -> Unit) {}
}
