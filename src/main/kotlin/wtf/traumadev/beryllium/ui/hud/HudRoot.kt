package wtf.traumadev.beryllium.ui.hud

import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.ui.ViewGroup

internal class HudRoot : ViewGroup() {
	init {
		interceptsInput = false
	}
	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = availableWidth
		height = availableHeight
		for (child in children) {
			if (!child.visible) continue
			child.measure(availableWidth, availableHeight)
		}
	}

	override fun onArrange() {
		val gui = Renderer.gui
		for (child in children) {
			if (!child.visible) continue
			val element = child as? HudElement ?: continue
			val x = element.anchoredX(gui.guiWidth).coerceIn(0f, (gui.guiWidth - child.width).coerceAtLeast(0f))
			val y = element.anchoredY(gui.guiHeight).coerceIn(0f, (gui.guiHeight - child.height).coerceAtLeast(0f))
			child.arrange(x, y)
		}
	}
}
