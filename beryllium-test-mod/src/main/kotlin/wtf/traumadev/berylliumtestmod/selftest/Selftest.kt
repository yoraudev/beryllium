package wtf.traumadev.berylliumtestmod.selftest

import com.google.gson.GsonBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.NarratorStatus
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.event.impl.TickEvent
import wtf.traumadev.beryllium.event.impl.WorldRenderEvent
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.ui.UiManager
import wtf.traumadev.berylliumtestmod.DemoHud
import wtf.traumadev.beryllium.util.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object Selftest {
	private const val PROP_ENABLE = "beryllium.selftest"
	private const val PROP_WORLD = "beryllium.selftest.world"
	private const val PROP_SEED = "beryllium.selftest.seed"
	private const val PROP_TIMEOUT = "beryllium.selftest.timeout"

	private const val BOOT_TIMEOUT_TICKS = 2400
	private const val JOIN_TIMEOUT_TICKS = 3600
	private const val SCENARIO_WARM_TIMEOUT_TICKS = 1200
	private const val CAPTURE_TIMEOUT_TICKS = 400
	private const val WORLD_SETTLE_TICKS = 60

	val enabled: Boolean = System.getProperty(PROP_ENABLE) != null

	private enum class State {
		BOOT, PREPARE, JOIN, SETUP_WORLD, SCENARIO_SETUP, SCENARIO_TICK, SCENARIO_CAPTURE, REPORT, SHUTDOWN, DONE
	}

	private class ScenarioResult(val scenario: Scenario) {
		val failures = ArrayList<String>()
		var screenshot: String? = null
	}

	private var state = State.BOOT
	private var stateTicks = 0
	private var startedAt = 0L
	private var initialized = false
	@Volatile private var finished = false

	private var worldId = "beryllium-selftest"
	private var seed = 20241028L
	private var timeoutSeconds = 300
	private lateinit var outDir: Path

	private val scenarios = ArrayList<Scenario>()
	private val results = ArrayList<ScenarioResult>()
	private val globalFailures = ArrayList<String>()
	private var scenarioIndex = 0
	private var activeScenario: Scenario? = null

	private var captureRequested = false
	private var captureInFlight = false
	private var captureErrorReported = false
	@Volatile private var pendingCapture: Capture? = null

	private val exitCode = AtomicInteger(0)

	fun initialize() {
		if (initialized) return
		initialized = true
		startedAt = System.currentTimeMillis()
		worldId = System.getProperty(PROP_WORLD, worldId)
		seed = System.getProperty(PROP_SEED, seed.toString()).toLongOrNull() ?: seed
		timeoutSeconds = System.getProperty(PROP_TIMEOUT, timeoutSeconds.toString()).toIntOrNull() ?: timeoutSeconds
		outDir = Minecraft.getInstance().gameDirectory.toPath().resolve("selftest")

		UiManager.register(SelftestUi)
		scenarios += SelftestScenarios.all()

		EventBus.subscribe<TickEvent>(priority = 500) { event ->
			if (event.phase == TickEvent.Phase.END) tick()
		}
		EventBus.subscribe<FrameEvent>(priority = 0) { event ->
			if (event.phase == FrameEvent.Phase.END) drawActive()
		}
		EventBus.subscribe<FrameEvent>(priority = -1000) { event ->
			if (event.phase == FrameEvent.Phase.END) captureFrame()
		}
		EventBus.subscribe<WorldRenderEvent>(priority = 0) { event ->
			if (state == State.SCENARIO_TICK || state == State.SCENARIO_CAPTURE) {
				activeScenario?.drawWorld?.invoke(event)
			}
		}

		Runtime.getRuntime().addShutdownHook(Thread({ Runtime.getRuntime().halt(exitCode.get()) }, "beryllium-selftest-exit"))
		startWatchdog()
		Logger.info("Selftest armed: ${scenarios.size} scenarios, world '$worldId', output $outDir")
	}

	private fun startWatchdog() {
		thread(name = "beryllium-selftest-watchdog", isDaemon = true) {
			val deadline = startedAt + timeoutSeconds * 1000L
			while (!finished) {
				if (System.currentTimeMillis() > deadline) {
					System.err.println("[Beryllium] Selftest watchdog timed out after ${timeoutSeconds}s in state $state")
					runCatching { writeReport() }
					Runtime.getRuntime().halt(3)
				}
				Thread.sleep(500)
			}
		}
	}

	private fun enter(next: State) {
		state = next
		stateTicks = 0
	}

	private fun tick() {
		if (finished) return
		stateTicks++
		try {
			when (state) {
				State.BOOT -> tickBoot()
				State.PREPARE -> tickPrepare()
				State.JOIN -> tickJoin()
				State.SETUP_WORLD -> tickSetupWorld()
				State.SCENARIO_SETUP -> tickScenarioSetup()
				State.SCENARIO_TICK -> tickScenarioTick()
				State.SCENARIO_CAPTURE -> tickScenarioCapture()
				State.REPORT -> tickReport()
				State.SHUTDOWN -> tickShutdown()
				State.DONE -> {}
			}
		} catch (throwable: Throwable) {
			Logger.error("Selftest failed in state $state", throwable)
			globalFailures += "state $state threw: ${throwable.message}"
			enter(State.REPORT)
		}
	}

	private fun tickBoot() {
		if (stateTicks == 1) {
			val minecraft = Minecraft.getInstance()
			minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0)
			minecraft.options.narrator().set(NarratorStatus.OFF)
			minecraft.getSoundManager().stop()
		}
		val gui = Minecraft.getInstance().gui
		if (gui.overlay() != null) {
			if (stateTicks > BOOT_TIMEOUT_TICKS) abort("game never finished loading")
			return
		}
		val screen = gui.screen()
		when {
			screen is TitleScreen -> enter(State.PREPARE)
			screen == null -> gui.setScreen(TitleScreen())
			else -> gui.setScreen(null)
		}
		if (stateTicks > BOOT_TIMEOUT_TICKS) abort("game never reached the title screen")
	}

	private fun tickPrepare() {
		val minecraft = Minecraft.getInstance()
		minecraft.options.guiScale().set(2)
		minecraft.options.pauseOnLostFocus = false
		minecraft.resizeGui()
		DemoHud.close()
		val worldDir = minecraft.gameDirectory.toPath().resolve("saves").resolve(worldId)
		if (Files.exists(worldDir) && !worldDir.toFile().deleteRecursively()) {
			abort("could not delete previous world at $worldDir")
			return
		}
		enter(State.JOIN)
		val settings = LevelSettings(
			"Selftest",
			GameType.CREATIVE,
			LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
			true,
			WorldDataConfiguration.DEFAULT
		)
		val options = WorldOptions(seed, false, false)
		minecraft.createWorldOpenFlows().createFreshLevel(worldId, settings, options, WorldPresets::createNormalWorldDimensions, TitleScreen())
	}

	private fun tickJoin() {
		val minecraft = Minecraft.getInstance()
		if (minecraft.level != null && minecraft.player != null && minecraft.gui.overlay() == null && minecraft.gui.screen() == null) {
			Logger.info("Selftest joined world '$worldId'")
			enter(State.SETUP_WORLD)
			return
		}
		if (stateTicks > JOIN_TIMEOUT_TICKS) abort("world join timed out")
	}

	private fun tickSetupWorld() {
		if (stateTicks == 1) {
			command("gamerule send_command_feedback false")
			command("gamerule advance_time false")
			command("gamerule advance_weather false")
			command("gamerule spawn_mobs false")
			command("gamerule fall_damage false")
			command("time set 6000")
			command("weather clear")
			command("effect give @s minecraft:night_vision infinite 0 true")
			command("attribute @s minecraft:gravity base set 0")
		}
		if (stateTicks >= WORLD_SETTLE_TICKS) enter(State.SCENARIO_SETUP)
	}

	private fun tickScenarioSetup() {
		if (scenarioIndex >= scenarios.size) {
			enter(State.REPORT)
			return
		}
		val scenario = scenarios[scenarioIndex]
		activeScenario = scenario
		Logger.info("Selftest scenario ${scenarioIndex + 1}/${scenarios.size}: ${scenario.name}")
		runCatching { scenario.setup() }.onFailure { scenario.stepFailures += "setup threw: ${it.message}" }
		enter(State.SCENARIO_TICK)
	}

	private fun tickScenarioTick() {
		val scenario = activeScenario ?: run { enter(State.SCENARIO_SETUP); return }
		val ready = runCatching { scenario.onTick() }.getOrElse {
			scenario.stepFailures += "tick threw: ${it.message}"
			true
		}
		if (ready && Renderer.ready) {
			captureRequested = true
			captureInFlight = false
			captureErrorReported = false
			enter(State.SCENARIO_CAPTURE)
			return
		}
		if (stateTicks > SCENARIO_WARM_TIMEOUT_TICKS) {
			scenario.stepFailures += "warmup timed out (prewarm=${Renderer.fontPrewarmComplete}, pipelinesReady=${Renderer.pipelinesReady})"
			captureRequested = true
			captureInFlight = false
			enter(State.SCENARIO_CAPTURE)
		}
	}

	private fun tickScenarioCapture() {
		val scenario = activeScenario ?: run { enter(State.SCENARIO_SETUP); return }
		val capture = pendingCapture
		if (capture != null) {
			pendingCapture = null
			finishScenario(scenario, capture)
			advanceScenario()
			return
		}
		if (stateTicks > CAPTURE_TIMEOUT_TICKS) {
			scenario.stepFailures += "screenshot timed out"
			recordScenario(scenario, null)
			advanceScenario()
		}
	}

	private fun finishScenario(scenario: Scenario, capture: Capture) {
		val failures = runCatching { scenario.verify(capture) }.getOrElse { listOf("verify threw: ${it.message}") }
		scenario.stepFailures += failures
		val fileName = "${scenario.name}.png"
		runCatching {
			Files.createDirectories(outDir)
			capture.image.writeToFile(outDir.resolve(fileName))
		}.onFailure { scenario.stepFailures += "screenshot write failed: ${it.message}" }
		capture.image.close()
		recordScenario(scenario, fileName)
	}

	private fun recordScenario(scenario: Scenario, screenshot: String?) {
		val result = ScenarioResult(scenario)
		result.failures += scenario.stepFailures
		result.screenshot = screenshot
		results += result
		val status = if (result.failures.isEmpty()) "PASS" else "FAIL"
		Logger.info("Selftest scenario ${scenario.name}: $status (${result.failures.size} failures)")
		result.failures.forEach { Logger.warn("  - $it") }
	}

	private fun advanceScenario() {
		val scenario = activeScenario
		if (scenario != null) {
			runCatching { scenario.teardown() }.onFailure { globalFailures += "teardown of ${scenario.name} threw: ${it.message}" }
		}
		activeScenario = null
		captureRequested = false
		scenarioIndex++
		enter(State.SCENARIO_SETUP)
	}

	private fun tickReport() {
		writeReport()
		enter(State.SHUTDOWN)
	}

	private fun tickShutdown() {
		if (results.any { it.failures.isNotEmpty() } || globalFailures.isNotEmpty()) exitCode.set(1)
		finished = true
		enter(State.DONE)
		Minecraft.getInstance().stop()
	}

	private fun abort(reason: String) {
		Logger.error("Selftest aborting: $reason")
		globalFailures += reason
		enter(State.REPORT)
	}

	private fun command(text: String) {
		val player = Minecraft.getInstance().player ?: return
		player.connection.sendCommand(text)
	}

	private fun drawActive() {
		if (state != State.SCENARIO_TICK && state != State.SCENARIO_CAPTURE) return
		val draw = activeScenario?.drawGui ?: return
		runCatching { Renderer.gui.draw() }.onFailure { activeScenario?.stepFailures?.add("drawGui threw: ${it.message}") }
	}

	private fun captureFrame() {
		if (state != State.SCENARIO_CAPTURE || !captureRequested || captureInFlight) return
		val minecraft = Minecraft.getInstance()
		captureInFlight = true
		try {
			Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), 1) { image ->
				val window = minecraft.window
				pendingCapture = Capture(
					image,
					image.width / window.guiScaledWidth.toFloat(),
					image.height / window.guiScaledHeight.toFloat()
				)
			}
		} catch (throwable: Throwable) {
			captureInFlight = false
			if (!captureErrorReported) {
				captureErrorReported = true
				activeScenario?.stepFailures?.add("screenshot failed: ${throwable.message}")
			}
		}
	}

	private fun writeReport() {
		val success = results.size == scenarios.size && results.all { it.failures.isEmpty() } && globalFailures.isEmpty()
		val minecraft = Minecraft.getInstance()
		val report = linkedMapOf<String, Any?>(
			"success" to success,
			"durationMillis" to (System.currentTimeMillis() - startedAt),
			"backend" to Renderer.backend.display,
			"window" to linkedMapOf(
				"width" to minecraft.window.width,
				"height" to minecraft.window.height,
				"guiScale" to minecraft.window.guiScale
			),
			"fontPrewarmComplete" to Renderer.fontPrewarmComplete,
			"worldTime" to (minecraft.level?.gameTime ?: -1L),
			"pipelinesReady" to Renderer.pipelinesReady,
			"globalFailures" to globalFailures,
			"scenarios" to results.map { result ->
				linkedMapOf(
					"name" to result.scenario.name,
					"success" to result.failures.isEmpty(),
					"failures" to result.failures,
					"screenshot" to result.screenshot
				)
			}
		)
		runCatching {
			Files.createDirectories(outDir)
			val json = GsonBuilder().setPrettyPrinting().create().toJson(report)
			Files.writeString(outDir.resolve("report.json"), json)
		}.onFailure { Logger.error("Failed to write selftest report", it) }
		Logger.info("Selftest finished: ${if (success) "SUCCESS" else "FAILURE"} (${results.count { it.failures.isEmpty() }}/${scenarios.size} scenarios passed)")
	}
}
