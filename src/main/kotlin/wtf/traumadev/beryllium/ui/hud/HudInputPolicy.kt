package wtf.traumadev.beryllium.ui.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.CharTypedEvent
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.event.impl.KeyPressEvent
import wtf.traumadev.beryllium.event.impl.MouseClickEvent
import wtf.traumadev.beryllium.event.impl.MouseMoveEvent
import wtf.traumadev.beryllium.event.impl.MouseScrollEvent

internal object HudInputPolicy {
	private const val POLICY_PRIORITY = 150

	private var initialized = false
	private var chatOpen = false

	internal fun attach(manager: HudManager) {
		if (initialized) return
		initialized = true
		chatOpen = false
		EventBus.subscribe<FrameEvent>(priority = POLICY_PRIORITY) { event ->
			if (event.phase == FrameEvent.Phase.START) updateChatState()
		}
		EventBus.subscribe<MouseMoveEvent>(priority = POLICY_PRIORITY) { event ->
			if (!active()) return@subscribe
			HudManager.host().routeMouseMove(event.x, event.y)
		}
		EventBus.subscribe<MouseClickEvent>(priority = POLICY_PRIORITY) { event ->
			if (!active()) return@subscribe
			val consumed = if (HudManager.editMode) {
				HudManager.routeEditClick(event.x, event.y, event.button, event.action)
			} else {
				HudManager.host().routeMouseClick(event.x, event.y, event.button, event.action, event.mods)
			}
			if (consumed) event.isCancelled = true
		}
		EventBus.subscribe<MouseScrollEvent>(priority = POLICY_PRIORITY) { event ->
			if (!active()) return@subscribe
			if (HudManager.host().routeMouseScroll(event.x, event.y, event.xOffset, event.yOffset)) event.isCancelled = true
		}
		EventBus.subscribe<KeyPressEvent>(priority = POLICY_PRIORITY) { event ->
			if (!active()) return@subscribe
			if (HudManager.host().routeKey(event.key, event.action, event.mods)) event.isCancelled = true
		}
		EventBus.subscribe<CharTypedEvent>(priority = POLICY_PRIORITY) { event ->
			if (!active()) return@subscribe
			if (HudManager.host().routeChar(event.codepoint)) event.isCancelled = true
		}
	}

	private fun isChatOpen(): Boolean = Minecraft.getInstance().gui.screen() is ChatScreen

	private fun active(): Boolean {
		if (!chatOpen && !HudManager.editMode) return false
		return HudManager.visible()
	}

	private fun updateChatState() {
		val minecraft = Minecraft.getInstance()
		if (minecraft.gui == null) return
		val now = isChatOpen()
		if (now == chatOpen) return
		chatOpen = now
		if (!now) HudManager.host().clearFocus()
	}
}
