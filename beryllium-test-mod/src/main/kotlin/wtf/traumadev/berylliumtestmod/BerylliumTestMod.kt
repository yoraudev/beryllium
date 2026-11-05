package wtf.traumadev.berylliumtestmod

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.TickEvent
import wtf.traumadev.beryllium.ui.UiManager
import wtf.traumadev.beryllium.ui.hud.HudManager
import wtf.traumadev.berylliumtestmod.selftest.Selftest

object BerylliumTestMod : ClientModInitializer {
	private val demoUiKey: KeyMapping = KeyMappingHelper.registerKeyMapping(
		KeyMapping(
			"key.berylliumtestmod.demo_ui",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_INSERT,
			KeyMapping.Category.MISC
		)
	)
	private val demoHudKey: KeyMapping = KeyMappingHelper.registerKeyMapping(
		KeyMapping(
			"key.berylliumtestmod.demo_hud",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_HOME,
			KeyMapping.Category.MISC
		)
	)

	override fun onInitializeClient() {
		DemoExtensions.initialize()
		HudManager.register(DemoHud)
		UiManager.register(DemoUi)
		if (!Selftest.enabled) DemoHud.open()
		if (Selftest.enabled) Selftest.initialize()
		EventBus.subscribe<TickEvent>(priority = 400) { event ->
			if (event.phase != TickEvent.Phase.START) return@subscribe
			val minecraft = Minecraft.getInstance()
			if (minecraft.player == null) return@subscribe
			if (minecraft.gui.screen() != null) return@subscribe
			while (demoUiKey.consumeClick()) DemoUi.toggle()
			while (demoHudKey.consumeClick()) DemoHud.toggle()
		}
	}
}
