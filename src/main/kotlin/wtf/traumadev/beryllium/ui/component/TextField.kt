package wtf.traumadev.beryllium.ui.component

import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Size
import kotlin.math.abs

class TextField : View() {
	var text: String = ""
		set(value) {
			field = value
			cursor = cursor.coerceIn(0, field.length)
			selectionAnchor = -1
			requestLayout()
		}
	var cursor: Int = text.length
		private set

	var hint: String = ""
	var textSize: Float = 14f
		set(value) {
			field = value
			requestLayout()
		}
	var font: FontRenderer = Fonts.regular
	var textColor: Int = Colors.WHITE
	var hintColor: Int = Colors.withAlpha(Colors.WHITE, 110)
	var backgroundColor: Int = Colors.hex("#1A1A22")
	var idleBorderColor: Int = Colors.hex("#3C3C46")
	var focusedBorderColor: Int = Colors.hex("#5B7CFF")
	var selectionColor: Int = Colors.withAlpha(Colors.hex("#5B7CFF"), 90)
	var caretColor: Int = Colors.WHITE
	var cornerRadius: Float = 6f
	var borderWidth: Float = 1f
	var paddingH: Float = 8f
	var onSubmitted: ((String) -> Unit)? = null
	var onTextChanged: ((String) -> Unit)? = null

	private var selectionAnchor: Int = -1
	private var scrollOffset: Float = 0f
	private var lastActivityNanos = System.nanoTime()

	init {
		focusable = true
		heightSpec = Size.Fixed(28f)
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 80f)
		height = resolveSize(heightSpec, availableHeight, font.lineHeight(textSize) + 12f)
	}

	override fun onPress(button: Int): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		lastActivityNanos = System.nanoTime()
		val contentX = (host?.mouseX ?: 0f) - absoluteX - paddingH + scrollOffset
		cursor = indexAtX(contentX)
		selectionAnchor = cursor
		return true
	}

	override fun onDrag(button: Int, deltaX: Float, deltaY: Float): Boolean {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
		lastActivityNanos = System.nanoTime()
		val contentX = (host?.mouseX ?: 0f) - absoluteX - paddingH + scrollOffset
		cursor = indexAtX(contentX)
		return true
	}

	override fun onChar(codepoint: Int): Boolean {
		if (codepoint < 32) return false
		lastActivityNanos = System.nanoTime()
		eraseSelection()
		val inserted = String(Character.toChars(codepoint))
		text = text.substring(0, cursor) + inserted + text.substring(cursor)
		cursor += inserted.length
		onTextChanged?.invoke(text)
		return true
	}

	override fun onKey(key: Int, action: Int, mods: Int): Boolean {
		if (action != InputConstants.PRESS && action != InputConstants.REPEAT) return false
		lastActivityNanos = System.nanoTime()
		val control = mods and InputConstants.MOD_CONTROL != 0
		val shift = mods and InputConstants.MOD_SHIFT != 0
		return when (key) {
			InputConstants.KEY_BACKSPACE -> {
				eraseBackward()
				true
			}
			InputConstants.KEY_DELETE -> {
				eraseForward()
				true
			}
			InputConstants.KEY_LEFT -> {
				moveCursor(-1, shift)
				true
			}
			InputConstants.KEY_RIGHT -> {
				moveCursor(1, shift)
				true
			}
			InputConstants.KEY_HOME -> {
				if (shift) {
					if (selectionAnchor < 0) selectionAnchor = cursor
				} else {
					selectionAnchor = -1
				}
				cursor = 0
				true
			}
			InputConstants.KEY_END -> {
				if (shift) {
					if (selectionAnchor < 0) selectionAnchor = cursor
				} else {
					selectionAnchor = -1
				}
				cursor = text.length
				true
			}
			InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				onSubmitted?.invoke(text)
				true
			}
			InputConstants.KEY_A -> if (control) {
				selectionAnchor = 0
				cursor = text.length
				true
			} else false
			InputConstants.KEY_C -> if (control) {
				copySelection()
				true
			} else false
			InputConstants.KEY_X -> if (control) {
				copySelection()
				eraseSelection()
			} else false
			InputConstants.KEY_V -> if (control) {
				insert(Minecraft.getInstance().keyboardHandler.getClipboard())
				true
			} else false
			else -> false
		}
	}

	override fun onDraw(canvas: UiCanvas) {
		updateScroll()
		val border = if (focused) focusedBorderColor else idleBorderColor
		canvas.panel(0f, 0f, width, height, cornerRadius, backgroundColor, borderColor = border, borderWidth = borderWidth)
		val innerWidth = (width - paddingH * 2f).coerceAtLeast(0f)
		val lineHeight = font.lineHeight(textSize)
		val textY = (height - lineHeight) / 2f
		canvas.pushClip(paddingH, 0f, innerWidth, height)
		if (text.isEmpty() && !focused && hint.isNotEmpty()) {
			canvas.text(hint, paddingH - scrollOffset, textY, textSize, hintColor, font)
		}
		val range = selectionRange()
		if (range != null) {
			val selectionStartX = paddingH - scrollOffset + font.textWidth(text.substring(0, range.first), textSize)
			val selectionEndX = paddingH - scrollOffset + font.textWidth(text.substring(0, range.last + 1), textSize)
			canvas.rect(selectionStartX, textY, selectionEndX - selectionStartX, lineHeight, selectionColor)
		}
		if (text.isNotEmpty()) {
			val caretX = paddingH - scrollOffset + font.textWidth(text.substring(0, cursor), textSize)
			if (cursor > 0) {
				canvas.text(text.substring(0, cursor), paddingH - scrollOffset, textY, textSize, textColor, font)
			}
			if (focused && caretVisible()) {
				canvas.rect(caretX, textY, 1f, lineHeight, caretColor)
			}
			if (cursor < text.length) {
				canvas.text(text.substring(cursor), caretX + if (focused) CARET_GAP else 0f, textY, textSize, textColor, font)
			}
		} else if (focused && caretVisible()) {
			canvas.rect(paddingH - scrollOffset, textY, 1f, lineHeight, caretColor)
		}
		canvas.popClip()
	}

	private fun caretVisible(): Boolean {
		val elapsed = System.nanoTime() - lastActivityNanos
		if (elapsed < BLINK_NANOS) return true
		return elapsed / BLINK_NANOS % 2L == 0L
	}

	fun setCursor(value: Int) {
		cursor = value.coerceIn(0, text.length)
	}

	private fun selectionRange(): IntRange? {
		val anchor = selectionAnchor
		if (anchor < 0 || anchor == cursor) return null
		return if (anchor < cursor) anchor until cursor else cursor until anchor
	}

	private fun copySelection() {
		val range = selectionRange() ?: return
		Minecraft.getInstance().keyboardHandler.setClipboard(text.substring(range.first, range.last + 1))
	}

	private fun eraseSelection(): Boolean {
		val range = selectionRange() ?: return false
		text = text.removeRange(range.first, range.last + 1)
		cursor = range.first
		selectionAnchor = -1
		onTextChanged?.invoke(text)
		return true
	}

	private fun eraseBackward() {
		if (eraseSelection()) return
		if (cursor == 0) return
		val index = cursor - 1
		text = text.removeRange(index, index + 1)
		cursor = index
		onTextChanged?.invoke(text)
	}

	private fun eraseForward() {
		if (eraseSelection()) return
		if (cursor >= text.length) return
		text = text.removeRange(cursor, cursor + 1)
		onTextChanged?.invoke(text)
	}

	private fun moveCursor(delta: Int, extendSelection: Boolean) {
		if (extendSelection) {
			if (selectionAnchor < 0) selectionAnchor = cursor
		} else {
			selectionAnchor = -1
		}
		cursor = (cursor + delta).coerceIn(0, text.length)
	}

	private fun insert(inserted: String) {
		if (inserted.isEmpty()) return
		eraseSelection()
		text = text.substring(0, cursor) + inserted + text.substring(cursor)
		cursor += inserted.length
		onTextChanged?.invoke(text)
	}

	private fun indexAtX(contentX: Float): Int {
		var best = 0
		var bestDistance = Float.MAX_VALUE
		for (index in 0..text.length) {
			val boundaryX = if (index == 0) 0f else font.textWidth(text.substring(0, index), textSize)
			val distance = abs(boundaryX - contentX)
			if (distance < bestDistance) {
				bestDistance = distance
				best = index
			}
		}
		return best
	}

	private fun updateScroll() {
		val innerWidth = (width - paddingH * 2f).coerceAtLeast(0f)
		val trailing = if (focused) CARET_GAP else 0f
		val caretX = font.textWidth(text.substring(0, cursor), textSize)
		if (caretX + trailing - scrollOffset > innerWidth) scrollOffset = caretX + trailing - innerWidth
		if (caretX - scrollOffset < 0f) scrollOffset = caretX
		val textWidth = font.textWidth(text, textSize)
		scrollOffset = scrollOffset.coerceIn(0f, (textWidth + trailing - innerWidth).coerceAtLeast(0f))
	}

	companion object {
		private const val BLINK_NANOS = 500_000_000L
		private const val CARET_GAP = 2f
	}
}
