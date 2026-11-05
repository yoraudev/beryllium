package wtf.traumadev.beryllium.ui.hud

import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.ui.UiHost
import wtf.traumadev.beryllium.ui.ViewGroup

object HudManager {
	private val registered = LinkedHashMap<String, Hud>()
	private val huds = LinkedHashMap<String, Hud>()
	private val host = UiHost(HudRoot(), drawGate = { visible() }, surface = Renderer.hud)
	private var editorOpen = false
	private var initialized = false

	val editMode: Boolean
		get() = editorOpen

	internal fun init() {
		if (initialized) return
		initialized = true
		HudInputPolicy.attach(this)
	}

	fun register(hud: Hud) {
		val id = hud.elementId.lowercase()
		check(!registered.containsKey(id)) { "Duplicate HUD element: ${hud.elementId}" }
		registered[id] = hud
	}

	fun unregister(hud: Hud) {
		val id = hud.elementId.lowercase()
		if (registered.remove(id) == null) return
		hud.close()
	}

	fun find(name: String): Hud? = registered[name.lowercase()]

	internal fun opened(hud: Hud) {
		check(!huds.containsKey(hud.elementId.lowercase())) { "HUD already open: ${hud.elementId}" }
		val element = hud.createElement()
		element.buildContent()
		huds[hud.elementId.lowercase()] = hud
		host.root.addChild(element)
		element.visible = hud.visible
		host.markLayoutDirty()
	}

	internal fun closed(hud: Hud) {
		val element = hud.element() ?: return
		huds.remove(hud.elementId.lowercase())
		host.root.removeChild(element)
		hud.destroyElement()
		host.markLayoutDirty()
	}

	internal fun allHuds(): Collection<Hud> = registered.values

	internal fun host(): UiHost = host

	internal fun hudAt(x: Float, y: Float): Hud? {
		for (hud in huds.values.reversed()) {
			val element = hud.element() ?: continue
			if (!element.visible || !hud.visible) continue
			if (element.containsPoint(x, y)) return hud
		}
		return null
	}

	internal fun pressHud(hud: Hud, x: Float, y: Float, button: Int) {
		hud.element()?.let { host.pressView(it, x, y, button) }
	}

	internal fun routeEditClick(x: Float, y: Float, button: Int, action: Int): Boolean {
		if (action == InputConstants.PRESS) {
			val hud = hudAt(x, y) ?: return false
			pressHud(hud, x, y, button)
			return true
		}
		return host.routeMouseClick(x, y, button, action, 0)
	}

	internal fun visible(): Boolean = Minecraft.getInstance().level != null || editorOpen

	fun openEditor(onClosed: () -> Unit = {}) {
		if (editorOpen) return
		editorOpen = true
		Minecraft.getInstance().gui.setScreen(HudEditorScreen {
			editorOpen = false
			onClosed()
		})
	}

	fun closeEditor() {
		if (!editorOpen) return
		editorOpen = false
		val screen = Minecraft.getInstance().gui.screen()
		if (screen is HudEditorScreen) Minecraft.getInstance().gui.setScreen(null)
	}

	fun toggleEditor() {
		if (editorOpen) closeEditor() else openEditor()
	}
}
