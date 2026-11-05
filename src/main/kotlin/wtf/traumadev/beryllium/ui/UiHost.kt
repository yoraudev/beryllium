package wtf.traumadev.beryllium.ui

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.Subscription
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.ui.component.TooltipView
import wtf.traumadev.beryllium.ui.draw.UiCanvas

internal class UiHost(
	val root: ViewGroup,
	val closeHandler: (() -> Unit)? = null,
	var drawGate: (() -> Boolean)? = null,
	surface: Renderer2D = Renderer.gui
) {
	val overlays = ArrayList<View>()

	var disposed: Boolean = false
		private set

	private val canvas = UiCanvas(surface)
	private val subscriptions = ArrayList<Subscription>()

	private var layoutDirty = true
	private var lastWidth = 0f
	private var lastHeight = 0f

	internal var mouseX = 0f
	internal var mouseY = 0f
	private var lastMouseX = 0f
	private var lastMouseY = 0f
	private var hovered: View? = null
	private var pressed: View? = null
	private var pressedButton = 0
	private var pressConsumed = false
	private var focused: View? = null
	private var hoverStartedAt = 0L
	private var tooltip: View? = null

	init {
		root.host = this
		attach(root)
		subscriptions += EventBus.subscribe<FrameEvent>(priority = FRAME_PRIORITY) { event ->
			if (event.phase == FrameEvent.Phase.END) onFrame()
		}
	}

	fun dispose() {
		if (disposed) return
		disposed = true
		for (subscription in subscriptions) subscription.unsubscribe()
		subscriptions.clear()
		clearFocus()
		setHovered(null)
		pressed = null
		pressConsumed = false
		root.resetInputState()
		for (overlay in overlays.toList()) overlay.resetInputState()
	}

	fun addOverlay(view: View) {
		check(view.parent == null) { "Overlay view already has a parent" }
		view.host = this
		attach(view)
		overlays.add(view)
		markLayoutDirty()
	}

	fun removeOverlay(view: View) {
		if (!overlays.remove(view)) return
		forgetView(view)
		view.host = null
		markLayoutDirty()
	}

	fun markLayoutDirty() {
		layoutDirty = true
	}

	fun requestFocus(view: View) {
		if (focused === view) return
		clearFocus()
		focused = view
		view.focused = true
		view.onFocusGained()
	}

	fun clearFocus() {
		val current = focused ?: return
		current.focused = false
		current.onFocusLost()
		focused = null
	}

	internal fun forgetView(view: View) {
		if (hovered === view) hovered = null
		if (pressed === view) {
			pressed = null
			pressConsumed = false
		}
		if (focused === view) focused = null
	}

	private fun attach(view: View) {
		view.host = this
		if (view is ViewGroup) for (child in view.children) attach(child)
	}

	private fun relayout(width: Float, height: Float) {
		root.measure(width, height)
		root.arrange(0f, 0f)
		for (overlay in overlays) {
			overlay.measure(width, height)
			overlay.arrange(overlay.x, overlay.y)
		}
		layoutDirty = false
		lastWidth = width
		lastHeight = height
	}

	private fun onFrame() {
		if (drawGate?.invoke() == false) return
		val g = Renderer.gui
		if (layoutDirty || g.guiWidth != lastWidth || g.guiHeight != lastHeight) {
			relayout(g.guiWidth, g.guiHeight)
		}
		root.draw(canvas)
		for (overlay in overlays) overlay.draw(canvas)
		updateTooltip()
	}

	internal fun routeMouseMove(x: Float, y: Float) {
		mouseX = x
		mouseY = y
		val current = pressed
		if (current != null) {
			current.onDrag(pressedButton, x - lastMouseX, y - lastMouseY)
		} else {
			val target = hitTest(x, y)
			setHovered(target)
			if (target != null) target.onMouseMove(x - target.absoluteX, y - target.absoluteY)
		}
		lastMouseX = x
		lastMouseY = y
	}

	internal fun routeMouseClick(x: Float, y: Float, button: Int, action: Int, mods: Int): Boolean {
		mouseX = x
		mouseY = y
		lastMouseX = x
		lastMouseY = y
		if (action == InputConstants.PRESS) {
			hideTooltip()
			val target = hitTest(x, y)
			pressed = target
			pressedButton = button
			pressConsumed = target != null
			if (target != null) {
				val focusTarget = generateSequence(target) { it.parent }.firstOrNull { it.focusable }
				if (focusTarget != null) requestFocus(focusTarget) else clearFocus()
				target.onPress(button)
				return true
			}
			clearFocus()
			return false
		}
		val target = pressed
		val consumed = pressConsumed
		pressed = null
		pressConsumed = false
		if (target == null || !consumed) return false
		val inside = target.containsPoint(x, y)
		target.onRelease(button, inside)
		if (inside) target.onClick(button)
		return true
	}

	internal fun routeMouseScroll(x: Float, y: Float, xOffset: Double, yOffset: Double): Boolean {
		var view = hitTest(x, y)
		while (view != null) {
			if (view.onScroll(xOffset, yOffset)) return true
			view = view.parent
		}
		return false
	}

	internal fun routeKey(key: Int, action: Int, mods: Int): Boolean {
		val current = focused ?: return false
		current.onKey(key, action, mods)
		return true
	}

	internal fun routeChar(codepoint: Int): Boolean {
		val current = focused ?: return false
		current.onChar(codepoint)
		return true
	}

	internal fun pressView(view: View, x: Float, y: Float, button: Int) {
		mouseX = x
		mouseY = y
		lastMouseX = x
		lastMouseY = y
		hideTooltip()
		pressed = view
		pressedButton = button
		pressConsumed = true
		val focusTarget = generateSequence(view) { it.parent }.firstOrNull { it.focusable }
		if (focusTarget != null) requestFocus(focusTarget) else clearFocus()
		view.onPress(button)
	}

	private fun setHovered(view: View?) {
		val current = hovered
		if (current === view) return
		hideTooltip()
		current?.onMouseExit()
		hovered = view
		view?.onMouseEnter()
		hoverStartedAt = if (view?.tooltipProvider != null) System.nanoTime() else 0L
	}

	private fun updateTooltip() {
		if (tooltip != null) return
		if (hoverStartedAt == 0L) return
		val provider = hovered?.tooltipProvider ?: return
		if (System.nanoTime() - hoverStartedAt < TOOLTIP_DELAY_NANOS) return
		val content = provider()
		if (content.isEmpty()) return
		val view = TooltipView(content)
		view.manuallyPositioned = true
		view.x = mouseX + TOOLTIP_OFFSET_X
		view.y = mouseY + TOOLTIP_OFFSET_Y
		view.zIndex = TOOLTIP_Z
		addOverlay(view)
		tooltip = view
	}

	internal fun suspendFrameDrawForTest() {
		drawGate = { false }
	}

	internal fun relayoutForTest(width: Float, height: Float) {
		relayout(width, height)
	}
	private fun hideTooltip() {
		val current = tooltip ?: return
		tooltip = null
		removeOverlay(current)
	}

	private fun hitTest(px: Float, py: Float): View? {
		for (overlay in overlays.asReversed()) {
			hitTestInView(overlay, px, py)?.let { return it }
		}
		return hitTestInView(root, px, py)
	}

	private fun hitTestInView(view: View, px: Float, py: Float): View? {
		if (!view.visible) return null
		if (!view.containsPoint(px, py)) return null
		if (view is ViewGroup) {
			val ordered = view.childrenByZ()
			for (index in ordered.indices.reversed()) {
				hitTestInView(ordered[index], px, py)?.let { return it }
			}
		}
		return if (view.interceptsInput) view else null
	}
	internal fun onEscapeFocusOrOverlay(): Boolean {
		if (focused != null) {
			clearFocus()
			return true
		}
		for (overlay in overlays.asReversed()) {
			if (overlay.onEscape()) return true
		}
		return false
	}

	internal fun onEscape(): Boolean {
		if (onEscapeFocusOrOverlay()) return true
		closeHandler?.invoke()
		return true
	}

	companion object {
		private const val FRAME_PRIORITY = 0
		private const val TOOLTIP_DELAY_NANOS = 400_000_000L
		private const val TOOLTIP_OFFSET_X = 12f
		private const val TOOLTIP_OFFSET_Y = 16f
		private const val TOOLTIP_Z = 1000
	}
}
