package wtf.traumadev.beryllium.ui

import wtf.traumadev.beryllium.ui.draw.UiCanvas

open class ViewGroup : View() {
	internal val children = ArrayList<View>()

	fun addChild(view: View) {
		check(view.parent == null) { "View already has a parent" }
		children.add(view)
		view.parent = this
		attachTree(view)
		requestLayout()
	}

	fun removeChild(view: View) {
		if (!children.remove(view)) return
		detachTree(view)
		requestLayout()
	}

	private fun attachTree(view: View) {
		view.host = host
		if (view is ViewGroup) for (child in view.children) attachTree(child)
	}

	private fun detachTree(view: View) {
		view.host?.forgetView(view)
		view.parent = null
		view.host = null
		if (view is ViewGroup) for (child in view.children) detachTree(child)
	}

	fun clearChildren() {
		for (child in children.toList()) removeChild(child)
	}

	internal fun childrenByZ(): List<View> =
		if (children.any { it.zIndex != 0 }) children.sortedBy { it.zIndex } else children

	override fun drawContent(canvas: UiCanvas) {
		for (child in childrenByZ()) child.draw(canvas)
	}

	override fun resetInputState() {
		super.resetInputState()
		for (child in children) child.resetInputState()
	}
}
