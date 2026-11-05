package wtf.traumadev.beryllium.ui

import com.mojang.blaze3d.systems.RenderSystem
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.hud.Hud
import wtf.traumadev.beryllium.ui.hud.HudEditorScreen
import wtf.traumadev.beryllium.ui.hud.HudManager

internal object UiPrewarm {
	private val surface = Renderer2D()
	private val canvas = UiCanvas(surface)
	private val steps = ArrayList<() -> Unit>()
	private var started = false
	private var finished = false

	fun initialize() {
		EventBus.subscribe<FrameEvent>(priority = 90) { event ->
			if (event.phase == FrameEvent.Phase.START) step()
		}
	}

	private fun step() {
		if (finished) return
		if (!ready()) return
		if (!started) {
			started = true
			buildSteps()
		}
		val gui = Renderer.gui
		surface.guiScale = gui.guiScale
		surface.guiWidth = gui.guiWidth
		surface.guiHeight = gui.guiHeight
		steps.removeFirst().invoke()
		surface.discard()
		if (steps.isEmpty()) finished = true
	}

	private fun ready(): Boolean =
		Renderer.initialized &&
			Fonts.prewarmComplete &&
			RenderSystem.tryGetDevice() != null

	private fun buildSteps() {
		for (ui in UiManager.allUis()) steps += { warmUi(ui) }
		for (hud in HudManager.allHuds()) steps += { warmHud(hud) }
		steps += { warmEditor() }
	}

	private fun warmUi(ui: Ui) {
		if (ui.isOpen) return
		layoutAndDraw(ui.prewarmRoot())
	}

	private fun warmHud(hud: Hud) {
		if (hud.isOpen) return
		val element = hud.createElement()
		element.buildContent()
		layoutAndDraw(element)
		hud.destroyElement()
	}

	private fun warmEditor() {
		layoutAndDraw(HudEditorScreen.buildRoot())
	}

	private fun layoutAndDraw(root: ViewGroup) {
		val gui = Renderer.gui
		root.measure(gui.guiWidth, gui.guiHeight)
		root.arrange(0f, 0f)
		root.draw(canvas)
	}
}
