package wtf.traumadev.beryllium.ui.hud

import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.ui.dsl.UiScope

abstract class Hud(
	val elementId: String,
	var anchor: HudAnchor = HudAnchor.TOP_LEFT,
	var offsetX: Float = 0f,
	var offsetY: Float = 0f,
) {
	protected abstract fun UiScope.build()

	var visible: Boolean = true

	private var element: HudElement? = null
	private var openFlag = false

	val isOpen: Boolean
		get() = openFlag

	fun toggle() {
		if (openFlag) close() else open()
	}

	fun open() {
		if (openFlag) return
		openFlag = true
		HudManager.opened(this)
	}

	fun close() {
		if (!openFlag) return
		openFlag = false
		HudManager.closed(this)
	}

	internal fun element(): HudElement? = element

	internal fun createElement(): HudElement = HudElement(this).also { element = it }

	internal fun destroyElement() {
		element = null
	}

	internal fun buildContent(element: HudElement) = with(this) { UiScope(element).build() }

	internal fun anchoredX(guiWidth: Float, elementWidth: Float): Float {
		val span = (guiWidth - elementWidth).coerceAtLeast(0f)
		return if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) span * (1f - offsetX) else span * offsetX
	}

	internal fun anchoredY(guiHeight: Float, elementHeight: Float): Float {
		val span = (guiHeight - elementHeight).coerceAtLeast(0f)
		return if (anchor == HudAnchor.BOTTOM_LEFT || anchor == HudAnchor.BOTTOM_RIGHT) span * (1f - offsetY) else span * offsetY
	}

	internal fun onElementDrag(deltaX: Float, deltaY: Float) {
		val element = element ?: return
		val gui = Renderer.gui
		val spanX = (gui.guiWidth - element.width).coerceAtLeast(1f)
		val spanY = (gui.guiHeight - element.height).coerceAtLeast(1f)
		val xSign = if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) -1f else 1f
		val ySign = if (anchor == HudAnchor.BOTTOM_LEFT || anchor == HudAnchor.BOTTOM_RIGHT) -1f else 1f
		offsetX = (offsetX + deltaX * xSign / spanX).coerceIn(0f, 1f)
		offsetY = (offsetY + deltaY * ySign / spanY).coerceIn(0f, 1f)
		element.requestLayout()
	}
}
