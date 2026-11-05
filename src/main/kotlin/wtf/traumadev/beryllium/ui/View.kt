package wtf.traumadev.beryllium.ui

import wtf.traumadev.beryllium.ui.anim.AnimatableFloat
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Alignment
import wtf.traumadev.beryllium.ui.layout.Size

open class View {
	var widthSpec: Size = Size.Wrap
		set(value) {
			field = value
			requestLayout()
		}
	var heightSpec: Size = Size.Wrap
		set(value) {
			field = value
			requestLayout()
		}

	var x: Float = 0f
		internal set
	var y: Float = 0f
		internal set

	var width: Float = 0f
		protected set
	var height: Float = 0f
		protected set

	var visible: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			requestLayout()
		}
	var alpha: Float = 1f
	var clipContent: Boolean = false
	var interceptsInput: Boolean = true
	var manuallyPositioned: Boolean = false
	var zIndex: Int = 0
	var focusable: Boolean = false
	var id: String? = null

	var focused: Boolean = false
		internal set

	internal var parent: ViewGroup? = null
	internal var host: UiHost? = null
	internal var alignmentOverride: Alignment? = null
	internal var tooltipProvider: (() -> String)? = null
	internal var fadeAnimation: AnimatableFloat? = null
	internal var slideXAnimation: AnimatableFloat? = null
	internal var slideYAnimation: AnimatableFloat? = null

	val absoluteX: Float
		get() {
			var value = x
			var ancestor = parent
			while (ancestor != null) {
				value += ancestor.x
				ancestor = ancestor.parent
			}
			return value
		}

	val absoluteY: Float
		get() {
			var value = y
			var ancestor = parent
			while (ancestor != null) {
				value += ancestor.y
				ancestor = ancestor.parent
			}
			return value
		}

	fun containsPoint(px: Float, py: Float): Boolean =
		px >= absoluteX && px < absoluteX + width && py >= absoluteY && py < absoluteY + height

	fun findById(targetId: String): View? {
		if (id == targetId) return this
		val group = this as? ViewGroup ?: return null
		for (child in group.children) {
			child.findById(targetId)?.let { return it }
		}
		return null
	}

	fun requestLayout() {
		host?.markLayoutDirty()
	}

	fun requestFocus() {
		host?.requestFocus(this)
	}

	internal fun detach() {
		val owner = parent
		if (owner != null) owner.removeChild(this) else host?.removeOverlay(this)
	}

	internal open fun resetInputState() {
		onMouseExit()
	}

	protected fun resolveSize(spec: Size, available: Float, content: Float): Float = when (spec) {
		is Size.Fixed -> spec.value
		is Size.Fill -> if (available.isInfinite()) content else available * spec.fraction
		Size.Wrap -> content
	}

	open fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 0f)
		height = resolveSize(heightSpec, availableHeight, 0f)
	}

	internal fun arrange(x: Float, y: Float) {
		this.x = x
		this.y = y
		onArrange()
	}

	protected open fun onArrange() {}

	internal fun draw(canvas: UiCanvas) {
		if (!visible) return
		canvas.pushView(this)
		onDraw(canvas)
		drawContent(canvas)
		canvas.popView(this)
	}

	protected open fun onDraw(canvas: UiCanvas) {}

	protected open fun drawContent(canvas: UiCanvas) {}

	open fun onMouseEnter() {}

	open fun onMouseExit() {}

	open fun onMouseMove(localX: Float, localY: Float): Boolean = false

	open fun onPress(button: Int): Boolean = false

	open fun onRelease(button: Int, inside: Boolean): Boolean = false

	open fun onClick(button: Int): Boolean = false

	open fun onDrag(button: Int, deltaX: Float, deltaY: Float): Boolean = false

	open fun onScroll(deltaX: Double, deltaY: Double): Boolean = false

	open fun onKey(key: Int, action: Int, mods: Int): Boolean = false

	open fun onChar(codepoint: Int): Boolean = false

	open fun onEscape(): Boolean = false

	open fun onFocusGained() {}

	open fun onFocusLost() {}
}
