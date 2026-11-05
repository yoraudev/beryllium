package wtf.traumadev.beryllium.ui.hud

import wtf.traumadev.beryllium.ui.layout.Box

internal class HudElement internal constructor(internal val hud: Hud) : Box() {
	init {
		manuallyPositioned = true
	}

	internal fun buildContent() {
		hud.buildContent(this)
	}

	internal fun anchoredX(guiWidth: Float): Float = hud.anchoredX(guiWidth, width)

	internal fun anchoredY(guiHeight: Float): Float = hud.anchoredY(guiHeight, height)

	override fun onPress(button: Int): Boolean = HudManager.editMode

	override fun onDrag(button: Int, deltaX: Float, deltaY: Float): Boolean {
		if (!HudManager.editMode) return false
		hud.onElementDrag(deltaX, deltaY)
		return true
	}
}
