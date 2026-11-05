package wtf.traumadev.beryllium.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.platform.InputConstants

internal open class UiScreen(
	private val host: UiHost,
	private val onClosed: (() -> Unit)? = null
) : Screen(Component.literal("Beryllium")) {

	override fun init() {
		host.markLayoutDirty()
	}

	override fun removed() {
		host.dispose()
		onClosed?.invoke()
	}

	override fun tick() {
		if (host.disposed) this.minecraft.gui.setScreen(null)
	}

	override fun isPauseScreen(): Boolean = false

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
	}

	override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.isEscape()) {
			if (host.onEscapeFocusOrOverlay()) return true
			this.onClose()
			return true
		}
		host.routeKey(event.key(), InputConstants.PRESS, event.modifiers())
		return true
	}

	override fun keyReleased(event: KeyEvent): Boolean {
		host.routeKey(event.key(), InputConstants.RELEASE, event.modifiers())
		return true
	}

	override fun charTyped(event: CharacterEvent): Boolean {
		host.routeChar(event.codepoint())
		return true
	}

	override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
		host.routeMouseClick(event.x().toFloat(), event.y().toFloat(), event.button(), InputConstants.PRESS, event.modifiers())
		return true
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		host.routeMouseClick(event.x().toFloat(), event.y().toFloat(), event.button(), InputConstants.RELEASE, event.modifiers())
		return true
	}

	override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
		host.routeMouseMove(event.x().toFloat(), event.y().toFloat())
		return true
	}

	override fun mouseMoved(x: Double, y: Double) {
		host.routeMouseMove(x.toFloat(), y.toFloat())
	}

	override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
		host.routeMouseScroll(x.toFloat(), y.toFloat(), scrollX, scrollY)
		return true
	}
}
