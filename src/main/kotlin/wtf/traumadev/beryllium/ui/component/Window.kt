package wtf.traumadev.beryllium.ui.component

import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.ViewGroup
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.ScrollView
import wtf.traumadev.beryllium.ui.layout.Size

class Window : ViewGroup() {
	var title: String = ""
	var titleTextSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var font: FontRenderer = Fonts.regular
	var titleColor: Int = Colors.WHITE
	var titleBarHeight: Float = 30f
	var background: Int = Colors.hex("#1E1E26")
	var borderColor: Int = Colors.hex("#3C3C46")
	var closeButtonColor: Int = Colors.withAlpha(Colors.WHITE, 150)
	var cornerRadius: Float = 10f
	var borderWidth: Float = 1f
	var shadowColor: Int = Colors.withAlpha(Colors.BLACK, 110)
	var shadowOffsetX: Float = 0f
	var shadowOffsetY: Float = 4f
	var shadowSpread: Float = 16f
	var padding: Padding = Padding(12f)
		set(value) {
			field = value
			requestLayout()
		}
	var closable: Boolean = true
	var onClose: (() -> Unit)? = null
	var screenMargin: Float = 0f
	var titleBar: Boolean = true
	var draggable: Boolean = true
	var scrollContent: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			if (value) addChild(viewport) else removeChild(viewport)
			requestLayout()
		}

	internal val viewport = ScrollView()

	init {
		viewport.widthSpec = Size.Fill()
		viewport.heightSpec = Size.Fill()
		addChild(viewport)
	}

	private var dragging = false

	private fun barHeight(): Float = if (titleBar) titleBarHeight else 0f

	override fun measure(availableWidth: Float, availableHeight: Float) {
		val bar = barHeight()
		if (scrollContent) {
			val innerWidth = (availableWidth - padding.horizontal).coerceAtLeast(0f)
			viewport.measure(innerWidth, Float.POSITIVE_INFINITY)
			width = resolveSize(widthSpec, availableWidth, viewport.contentWidth + padding.horizontal)
			height = resolveSize(heightSpec, availableHeight, bar + padding.vertical + viewport.contentHeight)
		} else {
			val innerWidth = (availableWidth - padding.horizontal).coerceAtLeast(0f)
			val innerHeight = (availableHeight - bar - padding.vertical).coerceAtLeast(0f)
			var contentWidth = 0f
			var contentHeight = 0f
			for (child in children) {
				if (!child.visible) continue
				child.measure(innerWidth, innerHeight)
				contentWidth = maxOf(contentWidth, child.width)
				contentHeight = maxOf(contentHeight, child.height)
			}
			width = resolveSize(widthSpec, availableWidth, contentWidth + padding.horizontal)
			height = resolveSize(heightSpec, availableHeight, bar + padding.vertical + contentHeight)
		}
		if (availableWidth.isFinite()) width = width.coerceAtMost((availableWidth - screenMargin * 2f).coerceAtLeast(0f))
		if (availableHeight.isFinite()) height = height.coerceAtMost((availableHeight - screenMargin * 2f).coerceAtLeast(0f))
		val finalWidth = (width - padding.horizontal).coerceAtLeast(0f)
		val finalHeight = (height - bar - padding.vertical).coerceAtLeast(0f)
		if (scrollContent) {
			viewport.measure(
				finalWidth,
				finalHeight
			)
		} else {
			for (child in children) {
				if (child.visible) child.measure(finalWidth, finalHeight)
			}
		}
	}

	override fun onArrange() {
		if (manuallyPositioned) {
			clampOnScreen()
		} else if (screenMargin > 0f) {
			val gui = Renderer.gui
			val upperX = (gui.guiWidth - width - screenMargin).coerceAtLeast(screenMargin)
			val upperY = (gui.guiHeight - height - screenMargin).coerceAtLeast(screenMargin)
			x = x.coerceIn(screenMargin, upperX)
			y = y.coerceIn(screenMargin, upperY)
		}
		val top = barHeight() + padding.top
		if (scrollContent) {
			viewport.arrange(padding.left, top)
		} else {
			for (child in children) {
				if (child.visible) child.arrange(padding.left, top)
			}
		}
	}

	override fun onPress(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		if (titleBar) {
			val bar = barHeight()
			val localX = (host?.mouseX ?: 0f) - absoluteX
			val localY = (host?.mouseY ?: 0f) - absoluteY
			if (closable && localY <= bar && localX >= width - bar) {
				close()
				return true
			}
			dragging = draggable && localY <= bar
		}
		return true
	}

	override fun onDrag(button: Int, deltaX: Float, deltaY: Float): Boolean {
		if (!dragging || !draggable || button != InputConstants.MOUSE_BUTTON_LEFT) return true
		manuallyPositioned = true
		x += deltaX
		y += deltaY
		clampOnScreen()
		return true
	}

	override fun onRelease(button: Int, inside: Boolean): Boolean {
		dragging = false
		return true
	}

	private fun clampOnScreen() {
		val gui = Renderer.gui
		val lowerX = (TITLE_MARGIN - width).coerceAtMost(0f)
		x = x.coerceIn(lowerX, (gui.guiWidth - TITLE_MARGIN).coerceAtLeast(0f))
		y = y.coerceIn(0f, (gui.guiHeight - barHeight()).coerceAtLeast(0f))
	}

	private fun close() {
		val handler = onClose
		if (handler != null) {
			handler.invoke()
		} else {
			detach()
		}
	}

	override fun onDraw(canvas: UiCanvas) {
		canvas.panel(
			0f, 0f, width, height, cornerRadius, background,
			borderColor = borderColor,
			borderWidth = borderWidth,
			shadowColor = shadowColor,
			shadowOffsetX = shadowOffsetX,
			shadowOffsetY = shadowOffsetY,
			shadowSpread = shadowSpread
		)
		val titleY = (barHeight() - font.lineHeight(titleTextSize)) / 2f
		if (titleBar) {
			canvas.text(title, padding.left, titleY, titleTextSize, titleColor, font)
			if (closable) {
				val centerX = width - barHeight() / 2f
				val centerY = barHeight() / 2f
				val half = 5f
				val thickness = 1.6f
				val mouseX = host?.mouseX ?: Float.NaN
				val mouseY = host?.mouseY ?: Float.NaN
				val hovered = mouseX >= absoluteX + width - barHeight() && mouseX < absoluteX + width &&
					mouseY >= absoluteY && mouseY < absoluteY + barHeight()
				val color = if (hovered) Colors.WHITE else closeButtonColor
				canvas.line(centerX - half, centerY - half, centerX + half, centerY + half, thickness, color)
				canvas.line(centerX - half, centerY + half, centerX + half, centerY - half, thickness, color)
				val cap = thickness / 2f
				canvas.circle(centerX - half, centerY - half, cap, color)
				canvas.circle(centerX + half, centerY + half, cap, color)
				canvas.circle(centerX - half, centerY + half, cap, color)
				canvas.circle(centerX + half, centerY - half, cap, color)
			}
			canvas.rect(0f, barHeight(), width, 1f, borderColor)
		}
	}

	companion object {
		private const val TITLE_MARGIN = 80f
	}
}
