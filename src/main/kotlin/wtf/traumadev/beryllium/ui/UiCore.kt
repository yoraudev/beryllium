package wtf.traumadev.beryllium.ui

import net.fabricmc.api.ClientModInitializer
import wtf.traumadev.beryllium.ui.hud.HudManager

object UiCore : ClientModInitializer {
	override fun onInitializeClient() {
		HudManager.init()
		UiPrewarm.initialize()
	}
}