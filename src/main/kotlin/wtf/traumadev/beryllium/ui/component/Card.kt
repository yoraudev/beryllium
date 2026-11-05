package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Box

class Card : Box() {
	var idleColor: Int = Colors.hex("#2E2E38")
	var hoverColor: Int = Colors.hex("#3C3C4A")
	var pressedColor: Int = Colors.hex("#26262E")
	var borderColor: Int = Colors.hex("#454550")
	var pressedBorderColor: Int = Colors.hex("#5B7CFF")
	var borderWidth: Float = 1f
	var cornerRadius: Float = 8f
	var shadowColor: Int = Colors.TRANSPARENT
	var shadowOffsetX: Float = 0f
	var shadowOffsetY: Float = 0f
	var shadowSpread: Float = 0f
	var onClicked: ((Int) -> Unit)? = null

	private val hoverSpring = SpringFloat(0f)
	private val pressSpring = SpringFloat(0f, stiffness = 400f, damping = 30f)

	override fun onMouseEnter() {
		hoverSpring.setTarget(1f)
	}

	override fun onMouseExit() {
		hoverSpring.setTarget(0f)
	}

	override fun onPress(button: Int): Boolean {
		if (button == InputConstants.MOUSE_BUTTON_LEFT) pressSpring.setTarget(1f)
		return true
	}

	override fun onRelease(button: Int, inside: Boolean): Boolean {
		pressSpring.setTarget(0f)
		return true
	}

	override fun onClick(button: Int): Boolean {
		onClicked?.invoke(button)
		return true
	}

	override fun onDraw(canvas: UiCanvas) {
		val hover = hoverSpring.value.coerceIn(0f, 1f)
		val press = pressSpring.value.coerceIn(0f, 1f)
		val fill = Colors.lerp(Colors.lerp(idleColor, hoverColor, hover), pressedColor, press * 0.6f)
		val border = Colors.lerp(borderColor, pressedBorderColor, press)
		canvas.panel(0f, 0f, width, height, cornerRadius, fill, fill, border, borderWidth, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread)
		super.onDraw(canvas)
	}
}
