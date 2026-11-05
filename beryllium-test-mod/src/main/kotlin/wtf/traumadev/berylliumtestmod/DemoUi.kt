package wtf.traumadev.berylliumtestmod

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import wtf.traumadev.beryllium.event.CancellableEvent
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.CharTypedEvent
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.event.impl.KeyPressEvent
import wtf.traumadev.beryllium.event.impl.MouseClickEvent
import wtf.traumadev.beryllium.event.impl.MouseMoveEvent
import wtf.traumadev.beryllium.event.impl.MouseScrollEvent
import wtf.traumadev.beryllium.event.impl.WorldRenderEvent
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.draw.Mesh
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.beryllium.ui.Ui
import wtf.traumadev.beryllium.ui.View
import wtf.traumadev.beryllium.ui.anim.AnimatableColor
import wtf.traumadev.beryllium.ui.anim.AnimatableFloat
import wtf.traumadev.beryllium.ui.anim.Easing
import wtf.traumadev.beryllium.ui.anim.Easings
import wtf.traumadev.beryllium.ui.anim.RepeatMode
import wtf.traumadev.beryllium.ui.anim.SpringFloat
import wtf.traumadev.beryllium.ui.anim.fadeTo
import wtf.traumadev.beryllium.ui.component.Button
import wtf.traumadev.beryllium.ui.component.Card
import wtf.traumadev.beryllium.ui.component.Checkbox
import wtf.traumadev.beryllium.ui.component.ProgressBar
import wtf.traumadev.beryllium.ui.component.TextAlign
import wtf.traumadev.beryllium.ui.component.TextView
import wtf.traumadev.beryllium.ui.component.Toggle
import wtf.traumadev.beryllium.ui.component.Window
import wtf.traumadev.beryllium.ui.dsl.UiScope
import wtf.traumadev.beryllium.ui.dsl.box
import wtf.traumadev.beryllium.ui.dsl.button
import wtf.traumadev.beryllium.ui.dsl.checkbox
import wtf.traumadev.beryllium.ui.dsl.column
import wtf.traumadev.beryllium.ui.dsl.dropdown
import wtf.traumadev.beryllium.ui.dsl.panel
import wtf.traumadev.beryllium.ui.dsl.progressBar
import wtf.traumadev.beryllium.ui.dsl.row
import wtf.traumadev.beryllium.ui.dsl.scroll
import wtf.traumadev.beryllium.ui.dsl.slider
import wtf.traumadev.beryllium.ui.dsl.svg
import wtf.traumadev.beryllium.ui.dsl.tabBar
import wtf.traumadev.beryllium.ui.dsl.text
import wtf.traumadev.beryllium.ui.dsl.textField
import wtf.traumadev.beryllium.ui.dsl.toggle
import wtf.traumadev.beryllium.ui.dsl.window
import wtf.traumadev.beryllium.ui.dsl.withTooltip
import wtf.traumadev.beryllium.ui.draw.UiCanvas
import wtf.traumadev.beryllium.ui.layout.Alignment
import wtf.traumadev.beryllium.ui.layout.CrossAlignment
import wtf.traumadev.beryllium.ui.layout.Flow
import wtf.traumadev.beryllium.ui.layout.MainAlignment
import wtf.traumadev.beryllium.ui.layout.Padding
import wtf.traumadev.beryllium.ui.layout.Size
import wtf.traumadev.beryllium.util.Logger
import kotlin.math.PI
import kotlin.math.sin

private val ACCENT = Colors.hex("#5B7CFF")
private val MUTED = Colors.hex("#8B8F9B")
private val SURFACE = Colors.hex("#16161C")
private val BORDER = Colors.hex("#34343F")
private val TEXT = Colors.hex("#E8EAF0")
private val DANGER = Colors.hex("#FF6B7C")
private val SUCCESS = Colors.hex("#57D38C")

private class ShowcaseEvent(val block: Boolean) : CancellableEvent() {
	val trace = ArrayList<String>()
}

private class MotionState {
	val drift = AnimatableFloat(0f)
	val color = AnimatableColor(Colors.hex("#5B7CFF"))
	val ease = AnimatableFloat(0f)
	val spring = SpringFloat(0f)

	fun start() {
		if (!drift.isPlaying) drift.animateTo(1f, 2.4f, Easings.easeInOutQuad, repeatMode = RepeatMode.PING_PONG)
		if (!color.isPlaying) color.animateTo(Colors.hex("#FF5B7C"), 2.4f, Easings.easeInOutCubic, repeatMode = RepeatMode.LOOP)
	}

	fun stop() {
		drift.stop()
		color.stop()
		ease.stop()
		spring.snapTo(spring.value)
	}

	fun playEase(easing: Easing) {
		ease.snapTo(0f)
		ease.animateTo(1f, 1.1f, easing)
	}
}

object DemoUi : Ui("beryllium_demo") {
	var screenWindow: Window? = null
		private set

	private var worldEnabled = false
	private var showFaces = true
	private var showEdges = true
	private var showLine = true
	private var showQuad = true
	private var showImage = true
	private var worldThickness = 3f
	private var worldSpin = 1f
	private var worldColor = Colors.hex("#57D3FF")

	private val motion = MotionState()
	private val demoLog = Logger.channel("demo_ui")

	private var lastStatusAt = 0L
	private var iconsReady = false
	private var lastKey = "none"
	private var mousePosition = "0, 0"
	private var lastClick = "none"
	private var lastScroll = "none"
	private var lastChar = "none"
	private var lastTrace = "none"
	private var lastCancelled = false
	private var deliveredEvents = 0

	private var backendLabel: TextView? = null
	private var fpsLabel: TextView? = null
	private var frameLabel: TextView? = null
	private var pipelinesLabel: TextView? = null
	private var prewarmLabel: TextView? = null
	private var glyphsLabel: TextView? = null
	private var fpsBar: ProgressBar? = null
	private var readyToggle: Toggle? = null
	private var nativeCheck: Checkbox? = null
	private var keyLabel: TextView? = null
	private var mouseLabel: TextView? = null
	private var clickLabel: TextView? = null
	private var scrollLabel: TextView? = null
	private var charLabel: TextView? = null
	private var postsLabel: TextView? = null
	private var traceLabel: TextView? = null
	private var channelLabel: TextView? = null

	init {
		EventBus.subscribe<FrameEvent>(priority = 60) { event ->
			if (event.phase != FrameEvent.Phase.END) return@subscribe
			if (!iconsReady && DemoAssets.icon.image != null && DemoAssets.badge.image != null) {
				iconsReady = true
				screenWindow?.requestLayout()
			}
			val now = System.currentTimeMillis()
			if (now - lastStatusAt < 250L) return@subscribe
			lastStatusAt = now
			if (!isOpen) return@subscribe
			refreshStatus()
			refreshEventLabels()
		}
		EventBus.subscribe<KeyPressEvent>(priority = 300) { event ->
			lastKey = "key ${event.key} · action ${event.action} · mods ${event.mods}"
		}
		EventBus.subscribe<MouseMoveEvent>(priority = 300) { event ->
			mousePosition = "${event.x.toInt()}, ${event.y.toInt()}"
		}
		EventBus.subscribe<MouseClickEvent>(priority = 300) { event ->
			val action = if (event.action == InputConstants.PRESS) "press" else "release"
			lastClick = "button ${event.button} · $action · ${event.x.toInt()}, ${event.y.toInt()}"
		}
		EventBus.subscribe<MouseScrollEvent>(priority = 300) { event ->
			lastScroll = "${event.xOffset.toInt()}, ${event.yOffset.toInt()}"
		}
		EventBus.subscribe<CharTypedEvent>(priority = 300) { event ->
			lastChar = runCatching { String(Character.toChars(event.codepoint)) }.getOrDefault("${event.codepoint}")
		}
		EventBus.subscribe<ShowcaseEvent>(priority = 20) { event ->
			event.trace += "p20"
		}
		EventBus.subscribe<ShowcaseEvent>(priority = 10) { event ->
			if (event.block) {
				event.trace += "cancel@p10"
				event.isCancelled = true
			} else {
				event.trace += "p10"
			}
		}
		EventBus.subscribe<ShowcaseEvent>(priority = 0) { event ->
			event.trace += "p0"
			deliveredEvents++
		}
		EventBus.subscribe<WorldRenderEvent>(priority = 0) { event ->
			if (worldEnabled) drawWorldOverlay(event)
		}
	}

	override fun onOpened() {
		motion.start()
	}

	override fun onClosed() {
		motion.stop()
	}

	override fun UiScope.build() {
		val pageViews = ArrayList<View>()
		val showcase = window(
			title = "Beryllium Showcase",
			width = Size.Fixed(600f),
			height = Size.Fixed(300f),
			screenMargin = 8f,
			scrollContent = false
		) {
			column(width = Size.Fill(), height = Size.Fill(), gap = 10f) {
				val tabs = tabBar(width = Size.Fill(), gap = 12f, indicatorColor = ACCENT)
				tabs.addTab("Overview", DemoAssets.icon)
				tabs.addTab("Components", DemoAssets.icon)
				tabs.addTab("Canvas", DemoAssets.icon)
				tabs.addTab("Motion", DemoAssets.icon)
				tabs.addTab("World", DemoAssets.icon)
				tabs.addTab("Events", DemoAssets.icon)
				val pages = box(width = Size.Fill(), height = Size.Fill()) {
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { overviewPage() }
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { componentsPage() }
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { canvasPage() }
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { motionPage() }
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { worldPage() }
					pageViews += scroll(width = Size.Fill(), height = Size.Fill(), gap = 8f) { eventsPage() }
				}
				val selectPage = { index: Int ->
					if (index in pageViews.indices) {
						pageViews.forEachIndexed { pageIndex, page -> page.visible = pageIndex == index }
						tabs.selected = index
						pages.requestLayout()
					}
				}
				tabs.onSelected = selectPage
				selectPage(0)
			}
		}
		screenWindow = showcase
	}

	private fun UiScope.overviewPage() {
		row(width = Size.Fill(), gap = 12f) {
			column(width = Size.Fill(0.5f), gap = 5f) {
				section("RENDERER")
				backendLabel = statusLine("Backend")
				fpsLabel = statusLine("Frames per second")
				frameLabel = statusLine("Frame time")
				pipelinesLabel = statusLine("Pipelines")
				prewarmLabel = statusLine("Glyph prewarm")
				glyphsLabel = statusLine("Font faces")
				spacer()
				readyToggle = toggleLine("Ready indicator", Renderer.ready)
				nativeCheck = checkbox(checked = Fonts.nativeAvailable, label = "Native font library").apply { interceptsInput = false }
				spacer()
				text("FPS vs 144", size = 9f, color = MUTED)
				fpsBar = progressBar(progress = 0f)
			}
			column(width = Size.Fill(0.5f), gap = 5f) {
				section("SHOWCASE")
				text("Tabs exercise every component family, the retained layout engine, animation, direct Renderer2D drawing and the world renderer.", size = 10f, color = TEXT, width = Size.Fill())
				panel(width = Size.Fill(), radius = 8f, fill = Colors.hex("#20202A"), borderColor = BORDER, borderWidth = 1f) {
					column(padding = Padding(8f), gap = 4f) {
						text("Controls", size = 11f, font = Fonts.bold, color = ACCENT)
						text("Insert toggles this UI. Home toggles the HUD. Escape closes the active UI; the title bar drags and the X closes.", size = 10f, color = MUTED, width = Size.Fill())
					}
				}
				text("HudManager.toggleEditor() opens the transparent HUD editor at any time. DemoHud exposes the same action from chat.", size = 10f, color = MUTED, width = Size.Fill())
				text("UiManager.find(\"beryllium_demo\") resolves this entity; UiManager.unregister removes and closes it.", size = 10f, color = MUTED, width = Size.Fill())
			}
		}
		spacer()
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN) {
			text("Single-UI rule: opening another Ui stops this one.", size = 9f, color = MUTED)
			text("Insert", size = 9f, font = Fonts.bold, color = ACCENT)
		}
	}

	private fun UiScope.componentsPage() {
		section("BUTTONS")
		var actionStatus: TextView? = null
		actionStatus = text("Last action: none", size = 10f, color = MUTED)
		row(width = Size.Fill(), gap = 8f) {
			val primary = button("Icon button") { actionStatus?.text = "icon button" }
			primary.icon = DemoAssets.badge
			primary.iconSize = 14f
			primary.selected = true
			val tooltipButton = button("Tooltip") { actionStatus?.text = "tooltip button" }
			tooltipButton.withTooltip { "withTooltip(provider) draws this while hovered" }
			button("Cycle selection") { primary.selected = !primary.selected }
		}
		section("TOGGLE / CHECKBOX")
		row(width = Size.Fill(), gap = 10f, crossAlignment = CrossAlignment.CENTER) {
			toggle(checked = true) { actionStatus?.text = "toggle ${if (it) "on" else "off"}" }
			checkbox(checked = false, label = "Checkbox") { actionStatus?.text = "checkbox ${if (it) "on" else "off"}" }
		}
		section("TEXT FIELD")
		var typed: TextView? = null
		typed = text("Typed: (empty)", size = 10f, color = MUTED)
		textField(hint = "Type, select, paste, press Enter...", onTextChanged = { typed?.text = "Typed: $it" }, onSubmitted = { typed?.text = "Submitted: $it" }).withTooltip { "Caret, shift selection, Ctrl+A/C/X/V, Enter" }
		section("DROPDOWN / SLIDER / PROGRESS")
		var dropdownStatus: TextView? = null
		dropdownStatus = text("Dropdown: none", size = 10f, color = MUTED)
		dropdown(options = listOf("Panel", "Card", "Window", "Flow", "ScrollView"), selected = 0, width = Size.Fixed(180f)) { _, value -> dropdownStatus?.text = "Dropdown: $value" }
		val bar = progressBar(progress = 0.35f)
		var sliderStatus: TextView? = null
		sliderStatus = text("Slider: 35%", size = 10f, color = MUTED)
		slider(min = 0f, max = 1f, initial = 0.35f) { value ->
			bar.progress = value
			val rounded = (value * 100f).toInt()
			val next = "Slider: $rounded%"
			if (sliderStatus?.text != next) sliderStatus?.text = next
		}
		section("CARD")
		var cardStatus: TextView? = null
		val card = Card()
		card.widthSpec = Size.Fill()
		card.heightSpec = Size.Fixed(46f)
		card.padding = Padding(10f)
		card.onClicked = { button -> cardStatus?.text = "Card clicked with button $button" }
		val cardLabel = TextView()
		cardLabel.text = "Card · hover and press springs, any mouse button"
		cardLabel.textSize = 10f
		cardLabel.color = TEXT
		cardLabel.interceptsInput = false
		card.addChild(cardLabel)
		add(card)
		cardStatus = text("Card clicked: none", size = 10f, color = MUTED)
		section("FLOW")
		val chips = Flow()
		chips.widthSpec = Size.Fill()
		chips.heightSpec = Size.Wrap
		chips.gap = 6f
		for (label in listOf("Row", "Column", "Box", "Flow", "ScrollView", "Size", "Padding", "Alignment")) {
			val chip = Button()
			chip.label = label
			chip.textSize = 10f
			chip.padding = Padding(8f, 3f, 8f, 3f)
			chip.cornerRadius = 10f
			chip.onClicked = { chip.selected = !chip.selected }
			chips.addChild(chip)
		}
		add(chips)
		section("PANEL / IMAGE / LAYOUT")
		panel(width = Size.Fill(), radius = 10f, fill = Colors.hex("#26304A"), fillBottom = Colors.hex("#141A2A"), borderColor = ACCENT, borderWidth = 1f, shadowColor = Colors.withAlpha(Colors.BLACK, 100), shadowOffsetY = 3f, shadowSpread = 8f) {
			column(padding = Padding(10f), gap = 3f) {
				text("Panel", size = 12f, font = Fonts.bold)
				text("SDF fill gradient, border and outer shadow in one quad.", size = 10f, color = MUTED, width = Size.Fill())
			}
		}
		row(width = Size.Fill(), gap = 10f, crossAlignment = CrossAlignment.CENTER) {
			svg(DemoAssets.badge, width = Size.Fixed(48f), height = Size.Fixed(48f)).withTooltip { "SvgImage.ofSource" }
			svg(DemoAssets.icon, color = Colors.hex("#FFD166"), width = Size.Fixed(48f), height = Size.Fixed(48f)).withTooltip { "SvgImage.ofResource, tinted" }
			text("ImageView draws Texture2D or SvgImage and re-measures when a texture swaps.", size = 10f, color = MUTED, width = Size.Fill())
		}
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN) {
			text("Row · SPACE_BETWEEN", size = 10f, color = MUTED)
			text("two ends", size = 10f, color = MUTED)
		}
		box(width = Size.Fill(), height = Size.Fixed(36f), alignment = Alignment.CENTER) {
			panel(width = Size.Fixed(120f), height = Size.Fixed(20f), radius = 10f, fill = ACCENT)
		}
	}

	private fun UiScope.canvasPage() {
		section("RENDERER2D")
		text("Every mark below is recorded through Renderer.gui while the retained tree draws. The scroll viewport and this view both clip it.", size = 10f, color = TEXT, width = Size.Fill())
		add(PrimitiveGallery().apply {
			widthSpec = Size.Fill()
			heightSpec = Size.Fixed(280f)
		})
	}

	private fun UiScope.motionPage() {
		section("ANIMATION")
		add(MotionView(motion).apply {
			widthSpec = Size.Fill()
			heightSpec = Size.Fixed(170f)
		})
		val easings = listOf(
			"linear" to Easings.linear,
			"inQuad" to Easings.easeInQuad,
			"outQuad" to Easings.easeOutQuad,
			"inOutQuad" to Easings.easeInOutQuad,
			"inCubic" to Easings.easeInCubic,
			"outCubic" to Easings.easeOutCubic,
			"inOutCubic" to Easings.easeInOutCubic,
			"outBack" to Easings.easeOutBack,
			"outElastic" to Easings.easeOutElastic,
			"outBounce" to Easings.easeOutBounce
		)
		val easingFlow = Flow()
		easingFlow.widthSpec = Size.Fill()
		easingFlow.heightSpec = Size.Wrap
		easingFlow.gap = 6f
		for ((label, easing) in easings) {
			val easingButton = Button()
			easingButton.label = label
			easingButton.textSize = 9f
			easingButton.padding = Padding(6f, 2f, 6f, 2f)
			easingButton.cornerRadius = 9f
			easingButton.onClicked = { motion.playEase(easing) }
			easingFlow.addChild(easingButton)
		}
		add(easingFlow)
		text("Easings drive AnimatableFloat; RepeatMode.PING_PONG and LOOP drive the drift and color animations.", size = 10f, color = MUTED, width = Size.Fill())
		val fadeTarget = text("fadeTo() target", size = 18f, font = Fonts.bold, color = ACCENT)
		row(width = Size.Fill(), gap = 6f) {
			button("Fade out", textSize = 10f) { fadeTarget.fadeTo(0.15f) }
			button("Fade in", textSize = 10f) { fadeTarget.fadeTo(1f) }
		}
		val springLabel = text("SpringFloat: ${format(motion.spring.value)}", size = 10f, color = MUTED)
		slider(min = 0f, max = 1f, initial = motion.spring.target) { value ->
			motion.spring.setTarget(value)
			val next = "SpringFloat: ${format(value)}"
			if (springLabel.text != next) springLabel.text = next
		}
		text("Slider output feeds SpringFloat.setTarget; the bar in the panel eases toward it.", size = 10f, color = MUTED, width = Size.Fill())
	}

	private fun UiScope.worldPage() {
		section("WORLD RENDERER")
		text("Enable the overlay, close this UI with Insert, then look around. Renderer3D draws depth-tested faces, edges, a line, an arbitrary quad and a textured quad during WorldRenderEvent.", size = 10f, color = TEXT, width = Size.Fill())
		val overlayStatus = text(if (worldEnabled) "Overlay: on" else "Overlay: off", size = 10f, color = MUTED)
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN, crossAlignment = CrossAlignment.CENTER) {
			text("World overlay", size = 10f, color = MUTED)
			toggle(checked = worldEnabled) { on ->
				worldEnabled = on
				overlayStatus.text = if (on) "Overlay: on" else "Overlay: off"
			}
		}
		checkbox(checked = showFaces, label = "Box faces") { showFaces = it }
		checkbox(checked = showEdges, label = "Box edges") { showEdges = it }
		checkbox(checked = showLine, label = "Line") { showLine = it }
		checkbox(checked = showQuad, label = "Arbitrary quad") { showQuad = it }
		checkbox(checked = showImage, label = "Textured image quad") { showImage = it }
		val thicknessLabel = text("Edge width: ${worldThickness.toInt()} px", size = 10f, color = MUTED)
		slider(min = 1f, max = 8f, initial = worldThickness) { value ->
			worldThickness = value
			thicknessLabel.text = "Edge width: ${value.toInt()} px"
		}
		val spinLabel = text("Orbit speed: ${format(worldSpin / 2f)}", size = 10f, color = MUTED)
		slider(min = 0f, max = 2f, initial = worldSpin) { value ->
			worldSpin = value
			spinLabel.text = "Orbit speed: ${format(value / 2f)}"
		}
		val palette = listOf(
			"Cyan" to Colors.hex("#57D3FF"),
			"Magenta" to Colors.hex("#FF5BD3"),
			"Amber" to Colors.hex("#FFC857"),
			"Lime" to Colors.hex("#8CFF5B")
		)
		dropdown(options = palette.map { it.first }, selected = 0, width = Size.Fixed(160f)) { index, _ ->
			worldColor = palette[index].second
		}
		text("The overlay orbits around the point the player is looking at, so it stays visible while moving.", size = 10f, color = MUTED, width = Size.Fill())
	}

	private fun UiScope.eventsPage() {
		section("EVENT BUS")
		text("Mouse, keyboard, char and scroll events are recorded here. ShowcaseEvent is custom and cancellable: p20 runs first, p10 may cancel, p0 only receives uncancelled events.", size = 10f, color = TEXT, width = Size.Fill())
		keyLabel = statusLine("Last key")
		mouseLabel = statusLine("Mouse position")
		clickLabel = statusLine("Last click")
		scrollLabel = statusLine("Last scroll")
		charLabel = statusLine("Last char")
		spacer()
		section("CUSTOM EVENT")
		var cancelToggle: Toggle? = null
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN, crossAlignment = CrossAlignment.CENTER) {
			text("Cancel at priority 10", size = 10f, color = MUTED)
			cancelToggle = toggle(checked = false)
		}
		row(width = Size.Fill(), gap = 8f) {
			button("Dispatch ShowcaseEvent") { dispatchShowcase(cancelToggle?.checked == true) }
			button("Reset trace") {
				lastTrace = "none"
				lastCancelled = false
				refreshEventLabels()
			}
		}
		postsLabel = statusLine("Delivered events")
		traceLabel = text("Trace: none", size = 10f, color = MUTED, width = Size.Fill())
		section("LOGGER CHANNEL")
		channelLabel = statusLine("demo_ui channel")
		row(width = Size.Fill(), gap = 8f) {
			button("Toggle channel") {
				demoLog.toggle()
				refreshEventLabels()
			}
			button("Log line") { demoLog.info("ShowcaseEvent trace=$lastTrace cancelled=$lastCancelled") }
		}
	}

	private fun UiScope.section(title: String) {
		text(title, size = 9f, color = MUTED, font = Fonts.bold)
	}

	private fun UiScope.spacer() {
		panel(width = Size.Fill(), height = Size.Fixed(1f), radius = 0f, fill = BORDER)
	}

	private fun UiScope.statusLine(label: String): TextView {
		var value: TextView? = null
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN, crossAlignment = CrossAlignment.CENTER) {
			text(label, size = 10f, color = MUTED)
			value = text("--", size = 10f, font = Fonts.bold, align = TextAlign.RIGHT)
		}
		return value ?: error("status line $label has no value")
	}

	private fun UiScope.toggleLine(label: String, initial: Boolean): Toggle {
		var view: Toggle? = null
		row(width = Size.Fill(), mainAlignment = MainAlignment.SPACE_BETWEEN, crossAlignment = CrossAlignment.CENTER) {
			text(label, size = 10f, color = MUTED)
			view = toggle(checked = initial).apply {
				interceptsInput = false
				trackWidth = 30f
				trackHeight = 16f
			}
		}
		return view ?: error("toggle line $label has no toggle")
	}

	private fun refreshStatus() {
		val fps = Renderer.framesPerSecond
		update(backendLabel, Renderer.backend.display)
		update(fpsLabel, fps.toInt().toString())
		update(frameLabel, if (fps > 0.5f) "${((1000f / fps) * 10f).toInt() / 10f} ms" else "--")
		update(pipelinesLabel, if (Renderer.pipelinesReady) "ready" else "compiling")
		update(prewarmLabel, if (Renderer.fontPrewarmComplete) "complete" else "warming")
		update(glyphsLabel, Fonts.names.joinToString(", "))
		setToggle(readyToggle, Renderer.ready)
		setCheck(nativeCheck, Fonts.nativeAvailable)
		fpsBar?.progress = (fps / 144f).coerceIn(0f, 1f)
	}

	private fun refreshEventLabels() {
		update(keyLabel, lastKey)
		update(mouseLabel, mousePosition)
		update(clickLabel, lastClick)
		update(scrollLabel, lastScroll)
		update(charLabel, lastChar)
		update(postsLabel, deliveredEvents.toString())
		update(traceLabel, "Trace: $lastTrace")
		update(channelLabel, if (demoLog.isEnabled) "enabled" else "silent")
	}

	private fun dispatchShowcase(block: Boolean) {
		val event = ShowcaseEvent(block)
		lastCancelled = EventBus.post(event)
		lastTrace = event.trace.joinToString(" > ")
		refreshEventLabels()
		demoLog.info("ShowcaseEvent block=$block trace=$lastTrace cancelled=$lastCancelled")
	}

	private fun update(view: TextView?, value: String) {
		if (view != null && view.text != value) view.text = value
	}

	private fun setToggle(view: Toggle?, value: Boolean) {
		if (view != null && view.checked != value) view.checked = value
	}

	private fun setCheck(view: Checkbox?, value: Boolean) {
		if (view != null && view.checked != value) view.checked = value
	}

	private fun format(value: Float): String = "${(value * 100f).toInt()}%"

	private fun drawWorldOverlay(event: WorldRenderEvent) {
		val player = Minecraft.getInstance().player ?: return
		val position = player.position()
		val look = player.lookAngle
		val seconds = (System.nanoTime() % 1_000_000_000L) / 1_000_000_000.0
		val orbit = sin(seconds * worldSpin * 2.0 * PI) * 1.2
		val centerX = position.x + look.x * 7.0 + orbit
		val centerY = position.y + 1.6 + look.y * 7.0
		val centerZ = position.z + look.z * 7.0 - orbit
		val minX = centerX - 1.0
		val minY = centerY - 1.0
		val minZ = centerZ - 1.0
		val maxX = centerX + 1.0
		val maxY = centerY + 1.0
		val maxZ = centerZ + 1.0
		if (showFaces) Renderer.world.boxFaces(minX, minY, minZ, maxX, maxY, maxZ, Colors.withAlpha(worldColor, 70), event)
		if (showEdges) Renderer.world.boxEdges(minX, minY, minZ, maxX, maxY, maxZ, worldThickness, Colors.withAlpha(worldColor, 235), event)
		if (showLine) Renderer.world.line(centerX - 2.4, centerY, centerZ, centerX + 2.4, centerY, centerZ, worldThickness, Colors.withAlpha(Colors.WHITE, 200), event)
		if (showQuad) {
			val bottom = centerY + 1.8
			val top = bottom + 1.4
			Renderer.world.quad(centerX - 1.6, bottom, centerZ, centerX + 1.6, bottom, centerZ, centerX + 1.6, top, centerZ, centerX - 1.6, top, centerZ, Colors.withAlpha(Colors.WHITE, 40), event)
		}
		val texture = DemoAssets.badge.image
		if (showImage && texture != null) {
			val y = maxY + 0.35
			Renderer.world.imageQuad(texture, minX, y, minZ, maxX, y, minZ, maxX, y, maxZ, minX, y, maxZ, Colors.WHITE, event)
		}
	}
}

private class PrimitiveGallery : View() {
	private val startedAt = System.nanoTime()

	init {
		clipContent = true
	}

	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 560f)
		height = resolveSize(heightSpec, availableHeight, 280f)
	}

	override fun drawContent(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, 10f, SURFACE, borderColor = BORDER, borderWidth = 1f)
		val g = Renderer.gui
		g.rect(14f, 14f, 44f, 28f, Colors.hex("#FF5B7C"))
		label(canvas, "rect", 14f, 44f)
		g.gradientRect(66f, 14f, 44f, 28f, Colors.hex("#FF5B7C"), Colors.hex("#5B7CFF"))
		label(canvas, "gradientRect", 66f, 44f)
		g.gradientRectH(118f, 14f, 44f, 28f, Colors.hex("#57D38C"), Colors.hex("#FFD166"))
		label(canvas, "gradientRectH", 118f, 44f)
		g.outline(170f, 14f, 44f, 28f, Colors.WHITE, 1.5f)
		label(canvas, "outline", 170f, 44f)
		g.roundedRect(222f, 14f, 44f, 28f, 8f, Colors.hex("#57D38C"))
		label(canvas, "roundedRect", 222f, 44f)
		g.sdfRect(274f, 14f, 52f, 28f, 8f, Colors.hex("#2E2E38"), borderColor = ACCENT, borderWidth = 1.5f, shadowColor = Colors.withAlpha(Colors.BLACK, 120), shadowOffsetY = 3f, shadowSpread = 6f)
		label(canvas, "sdfRect", 274f, 44f)
		g.triangle(338f, 42f, 362f, 14f, 386f, 42f, Colors.hex("#FFD166"))
		label(canvas, "triangle", 348f, 44f)
		g.circle(414f, 28f, 14f, Colors.hex("#57D3FF"))
		label(canvas, "circle", 400f, 44f)
		g.arc(456f, 42f, 14f, PI.toFloat(), 2f * PI.toFloat(), Colors.hex("#FF8A5B"))
		label(canvas, "arc", 444f, 44f)
		g.line(490f, 14f, 530f, 42f, 3f, Colors.hex("#C08BFF"))
		label(canvas, "line", 492f, 44f)

		canvas.text("MSDF regular 14", 14f, 58f, 14f, TEXT)
		canvas.text("MSDF bold 18", 14f, 78f, 18f, TEXT, Fonts.bold, shadow = true)
		var wrapY = 104f
		for (line in canvas.wrap("TextLayout.wrap splits long lines without losing words.", Fonts.regular, 10f, 180f)) {
			canvas.text(line, 14f, wrapY, 10f, MUTED)
			wrapY += 12f
		}

		val icon = DemoAssets.icon.image
		if (icon != null) {
			g.image(icon, 300f, 58f, 44f, 44f)
			g.imageRegion(icon, 350f, 58f, 44f, 44f, 0.5f, 0f, 1f, 1f)
			label(canvas, "image", 300f, 104f)
			label(canvas, "imageRegion", 350f, 104f)
		}
		val badge = DemoAssets.badge.image
		if (badge != null) {
			g.image(badge, 400f, 58f, 44f, 44f)
			label(canvas, "SVG texture", 400f, 104f)
		}
		val plate = DemoAssets.shaderPlate.image
		if (plate != null && Renderer.pipelineReady(DemoExtensions.gradientPipeline)) {
			g.texturedQuad(plate, 460f, 58f, 44f, 44f, 0f, 0f, 1f, 1f, Colors.WHITE, DemoExtensions.gradientPipeline)
			label(canvas, "custom shader", 458f, 104f)
		}
		DemoExtensions.demoFont?.let { font ->
			canvas.text("Custom font face", 300f, 140f, 12f, ACCENT, font)
		}
		if (Renderer.pipelineReady(DemoExtensions.meshPipeline)) {
			g.mesh(
				DemoExtensions.meshPipeline,
				Mesh.of(DemoExtensions.galleryMeshVertices, 3),
				uniforms = DemoExtensions.meshUniforms
			)
			label(canvas, "mesh + uniform", 370f, 250f)
		}
		if (Renderer.ready) {
			g.image(DemoExtensions.stripeTexture, 480f, 150f, 48f, 80f, sampling = TextureSampling.NEAREST)
			label(canvas, "nearest", 488f, 235f)
		}

		canvas.text("pushMatrix / translate / rotateZ / scale", 14f, 140f, 9f, MUTED)
		val seconds = ((System.nanoTime() - startedAt) / 1_000_000_000.0).toFloat()
		g.pushMatrix()
		g.translate(70f, 176f)
		g.rotateZ(seconds)
		g.scale(1.2f, 0.8f)
		g.rect(-18f, -12f, 36f, 24f, ACCENT)
		g.popMatrix()
		g.pushMatrix()
		g.translate(150f, 176f)
		g.rotateZ(-seconds * 0.7f)
		g.triangle(-18f, 14f, 0f, -16f, 18f, 14f, SUCCESS)
		g.popMatrix()
		canvas.pushClip(210f, 146f, 120f, 56f)
		g.rect(200f, 136f, 160f, 76f, Colors.hex("#7A2E4A"))
		canvas.popClip()
		label(canvas, "pushClip / popClip", 210f, 204f)

		val swatches = listOf(
			"hex" to Colors.hex("#5B7CFF"),
			"rgb" to Colors.rgb(87, 211, 140),
			"rgba" to Colors.rgba(255, 138, 91, 180),
			"lerp" to Colors.lerp(Colors.hex("#FF5B7C"), Colors.hex("#5B7CFF"), 0.5f),
			"alpha" to Colors.multiplyAlpha(Colors.WHITE, 0.4f)
		)
		swatches.forEachIndexed { index, (name, color) ->
			val x = 14f + index * 68f
			canvas.rect(x, 216f, 58f, 18f, color)
			canvas.text(name, x, 238f, 9f, MUTED)
		}
		canvas.text("Colors.hex / rgb / rgba / lerp / multiplyAlpha", 14f, 256f, 9f, MUTED)
		canvas.text("textWidth(\"Beryllium\", 14) = ${Fonts.regular.textWidth("Beryllium", 14f).toInt()} px · lineHeight = ${Fonts.regular.lineHeight(14f)}", 14f, 268f, 9f, MUTED)
	}

	private fun label(canvas: UiCanvas, text: String, x: Float, y: Float) {
		canvas.text(text, x, y, 9f, MUTED)
	}
}

private class MotionView(private val motion: MotionState) : View() {
	override fun measure(availableWidth: Float, availableHeight: Float) {
		width = resolveSize(widthSpec, availableWidth, 520f)
		height = resolveSize(heightSpec, availableHeight, 170f)
	}

	override fun drawContent(canvas: UiCanvas) {
		canvas.panel(0f, 0f, width, height, 10f, SURFACE, borderColor = BORDER, borderWidth = 1f)
		val drift = motion.drift.value.coerceIn(0f, 1f)
		canvas.circle(18f + drift * (width - 54f), 28f, 12f, Colors.lerp(ACCENT, DANGER, drift))
		canvas.text("AnimatableFloat · RepeatMode.PING_PONG", 18f, 46f, 9f, MUTED)
		canvas.panel(18f, 66f, 64f, 26f, 7f, motion.color.value)
		canvas.text("AnimatableColor", 92f, 72f, 10f, MUTED)
		val ease = motion.ease.value.coerceIn(0f, 1f)
		canvas.rect(18f + ease * (width - 72f), 104f, 32f, 16f, SUCCESS)
		canvas.text("Easings on AnimatableFloat", 18f, 126f, 9f, MUTED)
		val spring = motion.spring.value.coerceIn(0f, 1f)
		canvas.panel(18f, 146f, (width - 36f) * spring + 4f, 8f, 4f, ACCENT)
		canvas.text("SpringFloat", 18f, 156f, 9f, MUTED)
	}
}