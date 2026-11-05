package wtf.traumadev.beryllium.ui.layout

sealed interface Size {
	data class Fixed(val value: Float) : Size

	data class Fill(val fraction: Float = 1f) : Size

	object Wrap : Size
}
