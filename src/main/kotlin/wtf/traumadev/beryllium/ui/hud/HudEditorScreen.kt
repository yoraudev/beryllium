package wtf.traumadev.beryllium.ui.hud

import net.minecraft.client.Minecraft
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.ui.UiHost
import wtf.traumadev.beryllium.ui.UiScreen
import wtf.traumadev.beryllium.ui.dsl.box
import wtf.traumadev.beryllium.ui.dsl.button
import wtf.traumadev.beryllium.ui.dsl.panel
import wtf.traumadev.beryllium.ui.dsl.row
import wtf.traumadev.beryllium.ui.dsl.text
import wtf.traumadev.beryllium.ui.dsl.ui
import wtf.traumadev.beryllium.ui.layout.Alignment
import wtf.traumadev.beryllium.ui.layout.CrossAlignment
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Size

internal class HudEditorScreen(onClosed: () -> Unit) : UiScreen(UiHost(buildRoot()), onClosed) {
	companion object {
		internal fun buildRoot() = ui {
			box(width = Size.Fill(), height = Size.Wrap, alignment = Alignment.TOP_CENTER) {
				panel(
					radius = 10f,
					fill = Colors.hex("#E614141B"),
					borderColor = Colors.hex("#3C3C46"),
					borderWidth = 1f,
					shadowColor = Colors.withAlpha(Colors.BLACK, 110),
					shadowOffsetY = 4f,
					shadowSpread = 12f
				) {
					row(gap = 12f, padding = Padding(12f, 8f, 12f, 8f), crossAlignment = CrossAlignment.CENTER) {
						text("HUD Editor - drag elements to move them, click Done to save", size = 13f)
						val done = button("Done") { Minecraft.getInstance().gui.setScreen(null) }
						done.padding = Padding(10f, 4f, 10f, 4f)
					}
				}
			}
		}
	}
}
