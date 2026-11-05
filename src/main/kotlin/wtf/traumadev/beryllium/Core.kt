package wtf.traumadev.beryllium

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.ui.UiPrewarm
import wtf.traumadev.beryllium.ui.hud.HudManager

internal object Core : ModInitializer, ClientModInitializer {
	override fun onInitialize() {
	}

	override fun onInitializeClient() {
		Renderer.initialize()
		HudManager.init()
		UiPrewarm.initialize()
	}
}
