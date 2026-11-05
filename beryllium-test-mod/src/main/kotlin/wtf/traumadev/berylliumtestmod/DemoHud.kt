package wtf.traumadev.berylliumtestmod

import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.anim.AnimatableFloat
import wtf.traumadev.beryllium.ui.anim.Easings
import wtf.traumadev.beryllium.ui.anim.RepeatMode
import wtf.traumadev.beryllium.ui.component.Checkbox
import wtf.traumadev.beryllium.ui.component.TextView
import wtf.traumadev.beryllium.ui.dsl.UiScope
import wtf.traumadev.beryllium.ui.dsl.button
import wtf.traumadev.beryllium.ui.dsl.checkbox
import wtf.traumadev.beryllium.ui.dsl.column
import wtf.traumadev.beryllium.ui.dsl.panel
import wtf.traumadev.beryllium.ui.dsl.row
import wtf.traumadev.beryllium.ui.dsl.svg
import wtf.traumadev.beryllium.ui.dsl.text
import wtf.traumadev.beryllium.ui.dsl.withTooltip
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.hud.Hud
import wtf.traumadev.beryllium.ui.hud.HudAnchor
import wtf.traumadev.beryllium.ui.hud.HudManager
import wtf.traumadev.beryllium.ui.layout.CrossAlignment
import wtf.traumadev.beryllium.ui.layout.MainAlignment
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Size

private val ACCENT = Colors.hex("#5B7CFF")
private val MUTED = Colors.hex("#8B8F9B")

object DemoHud : Hud("beryllium_demo_hud", HudAnchor.BOTTOM_LEFT, 0f, 0f) {
	private val samples = FloatArray(64)
	private var sampleHead = 0
	private var lastSampleAt = 0L
	private val pulse = AnimatableFloat(0f)

	private var fpsLabel: TextView? = null
	private var frameLabel: TextView? = null
	private var subtitleLabel: TextView? = null
	private var readyCheck: Checkbox? = null
	private var pipelinesCheck: Checkbox? = null
	private var fontsCheck: Checkbox? = null
	private var panelView: View? = null

	init {
		EventBus.subscribe<FrameEvent>(priority = 60) { event ->
			if (event.phase != FrameEvent.Phase.END) return@subscribe
			if (!isOpen) {
				if (pulse.isPlaying || pulse.value != 0f) pulse.snapTo(0f)
				return@subscribe
			}
			if (!pulse.isPlaying) pulse.animateTo(1f, 1.6f, Easings.easeInOutQuad, repeatMode = RepeatMode.PING_PONG)
			val now = System.currentTimeMillis()
			if (now - lastSampleAt < 200L) return@subscribe
			lastSampleAt = now
			samples[sampleHead % samples.size] = Renderer.framesPerSecond
			sampleHead++
			refresh()
		}
	}

	override fun UiScope.build() {
		panelView = panel(
			width = Size.Fixed(248f),
			radius = 10f,
			fill = Colors.hex("#E61A1A22"),
			fillBottom = Colors.hex("#DE14141C"),
			borderColor = Colors.hex("#3C3C46"),
			borderWidth = 1f,
			shadowColor = Colors.withAlpha(Colors.BLACK, 130),
			shadowOffsetY = 4f,
			shadowSpread = 12f
		) {
			column(width = Size.Fill(), padding = Padding(10f), gap = 6f) {
				row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN, crossAlignment = CrossAlignment.CENTER) {
					row(gap = 8f, crossAlignment = CrossAlignment.CENTER) {
						svg(DemoAssets.icon, width = Size.Fixed(18f), height = Size.Fixed(18f))
						column(gap = 0f) {
							text("Beryllium", size = 13f, font = Fonts.bold)
							subtitleLabel = text("HUD showcase", size = 9f, color = MUTED)
						}
					}
					add(PulseDot { pulse.value })
				}
				add(Sparkline(samples) { sampleHead })
				row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN) {
					text("fps", size = 9f, color = MUTED)
					fpsLabel = text("0", size = 11f, font = Fonts.bold)
				}
				row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN) {
					text("frame", size = 9f, color = MUTED)
					frameLabel = text("--", size = 11f, font = Fonts.bold)
				}
				row(width = Size.Fill(), gap = 6f) {
					readyCheck = checkbox(checked = Renderer.ready, label = "ready").apply {
						interceptsInput = false
						boxSize = 10f
						textSize = 9f
					}
					pipelinesCheck = checkbox(checked = Renderer.pipelinesReady, label = "pipes").apply {
						interceptsInput = false
						boxSize = 10f
						textSize = 9f
					}
					fontsCheck = checkbox(checked = Fonts.nativeAvailable, label = "fonts").apply {
						interceptsInput = false
						boxSize = 10f
						textSize = 9f
					}
				}
				row(width = Size.Fill(), gap = 6f) {
					val anchorButton = button("anchor", textSize = 10f) { cycleAnchor() }
					anchorButton.padding = Padding(8f, 4f, 8f, 4f)
					anchorButton.heightSpec = Size.Fixed(24f)
					anchorButton.withTooltip { "Cycle the HUD anchor corner" }
					val editorButton = button("editor", textSize = 10f) { HudManager.toggleEditor() }
					editorButton.padding = Padding(8f, 4f, 8f, 4f)
					editorButton.heightSpec = Size.Fixed(24f)
					editorButton.withTooltip { "Open the HUD editor and drag this panel" }
				}
				text("Click in chat or the HUD editor", size = 8f, color = MUTED, width = Size.Fill())
			}
		}
		refresh()
	}

	private fun cycleAnchor() {
		anchor = when (anchor) {
			HudAnchor.TOP_LEFT -> HudAnchor.TOP_RIGHT
			HudAnchor.TOP_RIGHT -> HudAnchor.BOTTOM_RIGHT
			HudAnchor.BOTTOM_RIGHT -> HudAnchor.BOTTOM_LEFT
			HudAnchor.BOTTOM_LEFT -> HudAnchor.TOP_LEFT
		}
		offsetX = 0f
		offsetY = 0f
		panelView?.requestLayout()
		refresh()
	}

	private fun refresh() {
		val fps = Renderer.framesPerSecond
		update(fpsLabel, fps.toInt().toString())
		update(frameLabel, if (fps > 0.5f) "${((1000f / fps) * 10f).toInt() / 10f} ms" else "--")
		update(subtitleLabel, "${Renderer.backend.display} · ${anchor.name.lowercase().replace('_', ' ')}")
		setChecked(readyCheck, Renderer.ready)
		setChecked(pipelinesCheck, Renderer.pipelinesReady)
		setChecked(fontsCheck, Fonts.nativeAvailable)
	}

	private fun update(view: TextView?, value: String) {
		if (view != null && view.text != value) view.text = value
	}

	private fun setChecked(view: Checkbox?, value: Boolean) {
		if (view != null && view.checked != value) view.checked = value
	}
}

private class Sparkline(private val samples: FloatArray, private val head: () -> Int) : View() {
	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 200f)
		height = resolveSize(heightSpec, availableHeight, 32f)
	}

	override fun drawContent(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, 6f, Colors.hex("#99101016"), borderColor = Colors.hex("#33333E"), borderWidth = 1f)
		var previousX = 0f
		var previousY = height - 3f
		for (index in samples.indices) {
			val value = samples[(head() + index) % samples.size].coerceIn(0f, 160f)
			val x = 3f + index * (width - 6f) / (samples.size - 1)
			val y = height - 3f - value / 160f * (height - 6f)
			if (index > 0) {
				canvas.line(previousX, previousY, x, y, 1.2f, Colors.lerp(Colors.hex("#57D3FF"), Colors.hex("#FFD166"), index.toFloat() / samples.size))
			}
			previousX = x
			previousY = y
		}
		canvas.circle(previousX, previousY, 1.8f, Colors.hex("#FFD166"))
	}
}

private class PulseDot(private val value: () -> Float) : View() {
	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 10f)
		height = resolveSize(heightSpec, availableHeight, 10f)
	}

	override fun drawContent(canvas: UiCanvas) {
		val pulse = value().coerceIn(0f, 1f)
		val centerX = width / 2f
		val centerY = height / 2f
		canvas.circle(centerX, centerY, 4f, Colors.withAlpha(ACCENT, (100 + 155 * pulse).toInt()))
		canvas.circle(centerX, centerY, 1.8f, Colors.WHITE)
	}
}