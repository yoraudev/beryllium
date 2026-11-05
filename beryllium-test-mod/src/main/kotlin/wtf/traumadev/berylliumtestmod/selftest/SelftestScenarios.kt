package wtf.traumadev.berylliumtestmod.selftest

import net.minecraft.client.Minecraft
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import com.mojang.blaze3d.platform.InputConstants
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.RenderTarget2D
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.draw.Mesh
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.font.TextLayout
import wtf.traumadev.beryllium.render.pipeline.RenderPipelines
import wtf.traumadev.beryllium.render.svg.SvgImage
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.berylliumtestmod.selftest.Capture.Companion.blue
import wtf.traumadev.berylliumtestmod.selftest.Capture.Companion.green
import wtf.traumadev.berylliumtestmod.selftest.Capture.Companion.luminance
import wtf.traumadev.berylliumtestmod.selftest.Capture.Companion.red
import wtf.traumadev.berylliumtestmod.DemoUi
import wtf.traumadev.berylliumtestmod.DemoHud
import wtf.traumadev.berylliumtestmod.DemoAssets
import wtf.traumadev.berylliumtestmod.DemoExtensions
import kotlin.math.PI

object SelftestScenarios {
	private val BLACK = Colors.BLACK

	private fun command(text: String) {
		val player = Minecraft.getInstance().player ?: return
		player.connection.sendCommand(text)
	}

	private fun primitives() = Scenario(
		name = "primitives",
		drawGui = {
			rect(0f, 0f, guiWidth, guiHeight, BLACK)
			rect(20f, 20f, 40f, 30f, Colors.hex("#FF0000"))
			gradientRect(70f, 20f, 40f, 30f, Colors.hex("#FF0000"), Colors.hex("#0000FF"))
			gradientRectH(120f, 20f, 40f, 30f, Colors.hex("#00FF00"), Colors.hex("#FF0000"))
			outline(170f, 20f, 40f, 30f, Colors.WHITE, 2f)
			roundedRect(220f, 20f, 40f, 30f, 6f, Colors.hex("#0000FF"))
			triangle(270f, 50f, 290f, 20f, 310f, 50f, Colors.hex("#FFFF00"))
			circle(345f, 35f, 15f, Colors.hex("#FF00FF"))
			line(370f, 20f, 410f, 50f, 3f, Colors.hex("#00FFFF"))
			arc(445f, 45f, 15f, PI.toFloat(), 2f * PI.toFloat(), Colors.hex("#FF8000"))
			sdfRect(20f, 70f, 60f, 36f, 8f, Colors.hex("#FF8000"))
			sdfRect(90f, 70f, 60f, 36f, 8f, Colors.hex("#202020"), borderColor = Colors.WHITE, borderWidth = 2f)
			sdfRect(
				160f, 70f, 60f, 36f, 8f, Colors.hex("#303030"),
				shadowColor = Colors.withAlpha(Colors.WHITE, 200), shadowOffsetX = 0f, shadowOffsetY = 4f, shadowSpread = 6f
			)
		},
		verify = {
			listOfNotNull(
				expect("rect", 40f, 35f, Colors.hex("#FF0000")),
				expect("gradient top", 90f, 21f, Colors.hex("#FF0000")),
				expect("gradient bottom", 90f, 49f, Colors.hex("#0000FF")),
				expect("gradientH left", 121f, 35f, Colors.hex("#00FF00")),
				expect("gradientH right", 159f, 35f, Colors.hex("#FF0000")),
				expect("outline border", 171f, 21f, Colors.WHITE),
				expect("outline interior", 190f, 35f, BLACK),
				expect("rounded center", 240f, 35f, Colors.hex("#0000FF")),
				expect("rounded corner cut", 221f, 21f, BLACK),
				expect("rounded top-left fill", 225f, 22f, Colors.hex("#0000FF")),
				expect("rounded bottom-right fill", 255f, 48f, Colors.hex("#0000FF")),
				expect("triangle centroid", 290f, 45f, Colors.hex("#FFFF00")),
				expect("triangle outside", 269f, 48f, BLACK),
				expect("circle center", 345f, 35f, Colors.hex("#FF00FF")),
				expect("circle outside", 362f, 35f, BLACK),
				expect("line middle", 390f, 35f, Colors.hex("#00FFFF")),
				expect("line outside", 387f, 38f, BLACK),
				expect("arc inside", 445f, 35f, Colors.hex("#FF8000")),
				expect("arc outside", 445f, 55f, BLACK),
				expect("sdf fill", 50f, 88f, Colors.hex("#FF8000")),
				expect("sdf bordered interior", 120f, 88f, Colors.hex("#202020")),
				expect("sdf border", 120f, 71f, Colors.WHITE),
				check("sdf shadow", 190f, 110f, "shadow glow brighter than backdrop") {
					val lum = luminance(it)
					lum in 20..235
				}
			)
		}
	)

	private fun text() = Scenario(
		name = "text",
		drawGui = {
			rect(0f, 0f, guiWidth, guiHeight, Colors.hex("#202020"))
			text("Beryllium size 8", 20f, 150f, 8f, Colors.WHITE)
			text("Beryllium size 14", 20f, 166f, 14f, Colors.WHITE)
			text("Beryllium size 24", 20f, 190f, 24f, Colors.WHITE)
			text("Beryllium size 36", 20f, 222f, 36f, Colors.WHITE)
			text("Shadow", 350f, 150f, 20f, Colors.WHITE, shadow = true)
			val lines = TextLayout.wrap("Beryllium wraps text around narrow boxes", Fonts.regular, 12f, 90f)
			val lineHeight = Fonts.regular.lineHeight(12f)
			lines.forEachIndexed { index, line ->
				text(line, 350f, 192f + index * lineHeight, 12f, Colors.WHITE)
			}
		},
		verify = {
			val failures = ArrayList<String>()
			val bright: (Int) -> Boolean = { luminance(it) > 200 }
			val rows = listOf(
				Triple("size 8", 150f, 10f),
				Triple("size 14", 166f, 17f),
				Triple("size 24", 190f, 28f),
				Triple("size 36", 222f, 42f)
			)
			for ((label, y, height) in rows) {
				val count = countWhere(20f, y, 260f, height, bright)
				if (count < 12) failures += "$label: too few bright pixels ($count)"
			}
			val smallSpread = maxLuminanceSpread(20f, 151f, 160f, 8f)
			if (smallSpread < 150) failures += "size 8 row lacks contrast (spread $smallSpread)"
			val shadowMin = minLuminance(350f, 150f, 110f, 30f)
			if (shadowMin >= 0x18) failures += "shadow row missing dark pixels (min $shadowMin)"
			val shadowBright = countWhere(350f, 150f, 110f, 26f, bright)
			if (shadowBright < 12) failures += "shadow row missing glyph pixels ($shadowBright)"
			val wrapFirst = countWhere(350f, 194f, 90f, 7f, bright)
			val wrapSecond = countWhere(350f, 208f, 90f, 7f, bright)
			if (wrapFirst < 3) failures += "wrap first line missing ($wrapFirst)"
			if (wrapSecond < 3) failures += "wrap second line missing ($wrapSecond)"
			failures
		}
	)

	private fun transformScissor() = Scenario(
		name = "transform_scissor",
		drawGui = {
			rect(0f, 0f, guiWidth, guiHeight, BLACK)
			pushScissor(20f, 270f, 50f, 50f)
			rect(0f, 250f, 120f, 90f, Colors.hex("#FF0000"))
			popScissor()
			pushMatrix()
			translate(300f, 295f)
			rotateZ((PI / 4).toFloat())
			rect(-20f, -20f, 40f, 40f, Colors.hex("#00FF00"))
			popMatrix()
			pushMatrix()
			translate(400f, 295f)
			scale(2f, 0.5f)
			rect(-10f, -10f, 20f, 20f, Colors.hex("#0000FF"))
			popMatrix()
		},
		verify = {
			listOfNotNull(
				expect("scissor inside", 45f, 295f, Colors.hex("#FF0000")),
				expect("scissor right clipped", 90f, 295f, BLACK),
				expect("scissor top clipped", 10f, 260f, BLACK),
				expect("rotated center", 300f, 295f, Colors.hex("#00FF00")),
				expect("rotated axis point", 320f, 295f, Colors.hex("#00FF00")),
				expect("rotated corner outside", 320f, 275f, BLACK),
				expect("scaled wide", 415f, 295f, Colors.hex("#0000FF")),
				expect("scaled flat outside", 400f, 288f, BLACK),
				expect("scaled side outside", 421f, 295f, BLACK)
			)
		}
	)

	private fun svgScene(): Scenario {
		var svg: SvgImage? = null
		return object : Scenario(
			name = "svg",
			setup = {
				svg = SvgImage.ofResource("/assets/berylliumtestmod/textures/selftest.svg", "selftest")
			},
			teardown = {
				svg?.close()
				svg = null
			},
			drawGui = {
				rect(0f, 0f, guiWidth, guiHeight, BLACK)
				val texture = svg?.image
				if (texture != null) image(texture, 500f, 20f, 64f, 64f)
			},
			verify = {
				listOfNotNull(
					expect("svg rect", 504f, 24f, Colors.hex("#FF3B30"), tolerance = 48),
					expect("svg circle", 532f, 52f, Colors.hex("#34C759"), tolerance = 48),
					expect("svg triangle", 520f, 70f, Colors.hex("#0A84FF"), tolerance = 48)
				)
			}
		) {
			override fun onTick(): Boolean = super.onTick() && svg?.image != null
		}
	}

	private fun extensions(): Scenario {
		DemoExtensions.initialize()
		val pipeline = DemoExtensions.gradientPipeline
		val meshPipeline = DemoExtensions.meshPipeline
		var target: RenderTarget2D? = null
		return object : Scenario(
			name = "extensions",
			setup = {
				target = RenderTarget2D.create("selftest-offscreen", 64, 64)
			},
			teardown = {
				target?.close()
				target = null
			},
			drawGui = {
				rect(0f, 0f, guiWidth, guiHeight, BLACK)
				target?.draw {
					rect(0f, 0f, 64f, 32f, Colors.hex("#00FF00"))
					rect(0f, 32f, 64f, 32f, Colors.hex("#0000FF"))
				}
				target?.let { image(it.color, 20f, 20f, 64f, 64f) }
				val texture = DemoAssets.shaderPlate.image
				if (texture != null && Renderer.pipelineReady(pipeline)) {
					texturedQuad(texture, 100f, 20f, 64f, 64f, 0f, 0f, 1f, 1f, Colors.WHITE, pipeline)
				}
				image(DemoExtensions.stripeTexture, 180f, 20f, 64f, 64f, sampling = TextureSampling.NEAREST)
				if (Renderer.pipelineReady(meshPipeline)) {
					mesh(
						meshPipeline,
						Mesh.of(DemoExtensions.meshVertices, 3),
						uniforms = DemoExtensions.meshUniforms
					)
				}
				DemoExtensions.demoFont?.let { font ->
					text("Custom font", 20f, 150f, 24f, Colors.WHITE, font)
				}
				text("Bundled font", 20f, 190f, 24f, Colors.WHITE)
			},
			verify = {
				val failures = ArrayList<String>()
				expect("offscreen green", 30f, 30f, Colors.hex("#00FF00"))?.let(failures::add)
				expect("offscreen blue", 30f, 70f, Colors.hex("#0000FF"))?.let(failures::add)
				check("custom shader top", 110f, 30f, "blue-dominant gradient") { blue(it) > red(it) + 60 }?.let(failures::add)
				check("custom shader bottom", 110f, 70f, "red-dominant gradient") { red(it) > blue(it) + 30 }?.let(failures::add)
				check("nearest left texel", 192f, 50f, "red") { red(it) > blue(it) + 80 }?.let(failures::add)
				check("nearest right texel", 213f, 50f, "blue") { blue(it) > red(it) + 80 }?.let(failures::add)
				check("mesh left texel", 292f, 60f, "red from the bound mask") {
					red(it) > 180 && blue(it) < 80
				}?.let(failures::add)
				check("mesh right texel", 328f, 60f, "blue from the bound mask") {
					blue(it) > 180 && red(it) < 80
				}?.let(failures::add)
				val customPixels = countWhere(20f, 150f, 220f, 30f) { luminance(it) > 200 }
				if (customPixels < 20) failures += "custom font text missing ($customPixels bright pixels)"
				if (DemoExtensions.demoFont == null) failures += "custom font registration failed"
				if (!Fonts.names.contains("demo")) failures += "custom font name not exposed by Fonts.names"
				if (!RenderPipelines.isRegistered(pipeline)) failures += "custom pipeline not registered"
				if (!RenderPipelines.isRegistered(meshPipeline)) failures += "mesh pipeline not registered"
				if (!Renderer.pipelineReady(pipeline)) failures += "custom pipeline did not compile"
				if (!Renderer.pipelineReady(meshPipeline)) failures += "mesh pipeline did not compile"
				failures
			}
		) {
			override fun onTick(): Boolean =
				super.onTick() &&
					DemoAssets.shaderPlate.image != null &&
					Renderer.pipelineReady(pipeline) &&
					Renderer.pipelineReady(meshPipeline)
		}
	}

	private fun worldOverlay() = Scenario(
		name = "world",
		warmupTicks = 45,
		setup = {
			Minecraft.getInstance().gui.setScreen(null)
			command("tp @s 0.5 150.0 8.5 180 -13")
		},
		drawWorld = { event ->
			Renderer.world.boxFaces(-2.0, 152.0, -9.0, 3.0, 157.0, -4.0, Colors.hex("#E6FF0000"), event)
			Renderer.world.boxEdges(-2.0, 152.0, -9.0, 3.0, 157.0, -4.0, 4f, Colors.hex("#FF00FF"), event)
			Renderer.world.line(-2.0, 152.0, -9.0, 3.0, 157.0, -4.0, 6f, Colors.hex("#00FF00"), event)
		},
		verify = {
			val failures = ArrayList<String>()
			val redFaces = countWhere(160f, 90f, 320f, 180f) { red(it) > 140 && red(it) - blue(it) > 50 }
			if (redFaces < 2000) failures += "world box faces not visible (red pixels: $redFaces)"
			val magentaEdges = countWhere(140f, 70f, 360f, 220f) { red(it) > 170 && blue(it) > 170 && green(it) < 110 }
			if (magentaEdges < 100) failures += "world box edges not visible (magenta pixels: $magentaEdges)"
			val greenLine = countWhere(140f, 70f, 360f, 220f) { green(it) > 140 && green(it) - red(it) > 50 && green(it) - blue(it) > 50 }
			if (greenLine < 30) failures += "world line not visible (green pixels: $greenLine)"
			failures
		}
	)

	private fun hud() = Scenario(
		name = "hud",
		warmupTicks = 30,
		setup = {
			command("tp @s 0.5 150.0 8.5 180 -90")
			DemoHud.open()
		},
		teardown = { DemoHud.close() },
		verify = {
			val failures = ArrayList<String>()
			val dark = countWhere(0f, 310f, 170f, 50f) { luminance(it) < 100 }
			if (dark < 30) failures += "hud panel not visible bottom-left (dark pixels: $dark)"
			check("sky above panel", 320f, 60f, "bright blue sky") { blue(it) > 140 && blue(it) > red(it) }?.let(failures::add)
			failures
		}
	)

	private fun demoUi() = Scenario(
		name = "ui_demo",
		warmupTicks = 15,
		setup = { DemoUi.open() },
		teardown = { DemoUi.close() },
		verify = {
			val failures = ArrayList<String>()
			val window = DemoUi.screenWindow
			if (window == null) {
				failures += "demo window missing"
			} else {
				expect("window background", window.absoluteX + 8f, window.absoluteY + window.titleBarHeight / 2f, Colors.hex("#1E1E26"), tolerance = 32)?.let(failures::add)
				val titlePixels = countWhere(window.absoluteX, window.absoluteY, window.width, window.titleBarHeight) { luminance(it) > 180 }
				if (titlePixels < 8) failures += "window title text missing (bright pixels: $titlePixels)"
				val tabPixels = countWhere(window.absoluteX, window.absoluteY + window.titleBarHeight, window.width, 30f) { luminance(it) > 180 }
				if (tabPixels < 20) failures += "tab bar labels missing (bright pixels: $tabPixels)"
			}
			check("world visible behind window", 10f, 10f, "sky blue") { blue(it) > red(it) }?.let(failures::add)
			failures
		}
	)

	private class InteractionScenario : Scenario(
		name = "ui_interaction",
		warmupTicks = 0,
		teardown = { SelftestUi.close() }
	) {
		private var step = 0
		private var wait = 0

		override fun onTick(): Boolean {
			if (wait > 0) {
				wait--
				return false
			}
			when (step) {
				0 -> {
					SelftestUi.resetState()
					SelftestUi.open()
					advance(4)
				}

				1 -> {
					val toggle = SelftestUi.toggle
					if (toggle == null) stepFailures += "toggle view missing"
					else click(toggle.absoluteX + toggle.width / 2f, toggle.absoluteY + toggle.height / 2f)
					advance(3)
				}

				2 -> {
					if (SelftestUi.toggle?.checked != true) stepFailures += "toggle did not flip on click"
					val slider = SelftestUi.slider
					if (slider == null) stepFailures += "slider view missing"
					else click(slider.absoluteX + slider.width * 0.75f, slider.absoluteY + slider.height / 2f)
					advance(3)
				}

				3 -> {
					val value = SelftestUi.slider?.value ?: -1f
					if (value < 0.6f || value > 0.9f) stepFailures += "slider value $value after 75% click"
					val buttonView = SelftestUi.buttonCenter()
					if (buttonView == null) stepFailures += "button view missing"
					else click(buttonView.first, buttonView.second)
					advance(3)
				}

				4 -> {
					if (SelftestUi.clicks != 1) stepFailures += "button clicked ${SelftestUi.clicks} times, expected 1"
					val field = SelftestUi.field
					if (field == null) stepFailures += "text field missing"
					else click(field.absoluteX + 10f, field.absoluteY + field.height / 2f)
					advance(3)
				}

				5 -> {
					type("abc")
					advance(3)
				}

				6 -> {
					if (SelftestUi.field?.text != "abc") stepFailures += "text field content: '${SelftestUi.field?.text}'"
					return true
				}
			}
			return false
		}

		private fun advance(ticks: Int) {
			wait = ticks
			step++
		}

		private fun click(gx: Float, gy: Float) {
			val screen = Minecraft.getInstance().gui.screen()
			if (screen == null) {
				stepFailures += "no screen open for click"
				return
			}
			val event = MouseButtonEvent(gx.toDouble(), gy.toDouble(), MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0))
			screen.mouseClicked(event, false)
			screen.mouseReleased(event)
		}

		private fun type(text: String) {
			val screen = Minecraft.getInstance().gui.screen()
			if (screen == null) {
				stepFailures += "no screen open for typing"
				return
			}
			for (char in text) screen.charTyped(CharacterEvent(char.code))
		}
	}

	fun all(): List<Scenario> = listOf(
		worldOverlay(),
		hud(),
		primitives(),
		text(),
		transformScissor(),
		svgScene(),
		extensions(),
		demoUi(),
		InteractionScenario()
	)
}
