package wtf.traumadev.beryllium.ui

import net.minecraft.client.Minecraft
import wtf.traumadev.beryllium.ui.component.Window
import wtf.traumadev.beryllium.ui.dsl.UiScope
import wtf.traumadev.beryllium.ui.dsl.ui

abstract class Ui(val id: String) {
	protected abstract fun UiScope.build()

	val name: String
		get() = id

	protected open fun onOpened() {}

	protected open fun onClosed() {}

	private var host: UiHost? = null
	private var cachedRoot: ViewGroup? = null
	private var openFlag = false

	val isOpen: Boolean
		get() = openFlag

	fun toggle() {
		if (openFlag) close() else open()
	}

	fun open() {
		val active = activeUi
		if (active === this) {
			closeInternal()
			cachedRoot = null
			openInternal()
			return
		}
		active?.stop()
		activeUi = this
		openInternal()
	}

	fun close() {
		if (activeUi === this) activeUi = null
		closeInternal()
	}

	internal fun stop() {
		if (activeUi === this) activeUi = null
		closeInternal()
	}

	private fun openInternal() {
		check(!openFlag) { "UI already open: $id" }
		openFlag = true
		val root = cachedRoot ?: ui { build() }.also {
			cachedRoot = it
			installCloseHandlers(it)
		}
		val screenHost = UiHost(root, closeHandler = { closeInternal() })
		host = screenHost
		Minecraft.getInstance().gui.setScreen(UiScreen(screenHost) { closeInternal() })
		onOpened()
	}

	internal fun prewarmRoot(): ViewGroup {
		val existing = cachedRoot
		if (existing != null) return existing
		return ui { build() }.also {
			cachedRoot = it
			installCloseHandlers(it)
		}
	}

	private fun closeInternal() {
		if (!openFlag) return
		openFlag = false
		if (activeUi === this) activeUi = null
		host?.dispose()
		host = null
		onClosed()
	}

	private fun installCloseHandlers(root: ViewGroup) {
		for (child in root.children) {
			if (child is Window && child.onClose == null) child.onClose = { closeInternal() }
		}
	}

	companion object {
		private var activeUi: Ui? = null
	}
}
