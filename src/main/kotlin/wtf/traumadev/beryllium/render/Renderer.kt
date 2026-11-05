package wtf.traumadev.beryllium.render

import com.mojang.blaze3d.pipeline.PipelineCache
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.ShaderSource
import com.mojang.renderpearl.api.pipeline.ShaderType
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderManager
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.FrameEvent
import wtf.traumadev.beryllium.event.impl.WorldRenderEvent
import wtf.traumadev.beryllium.render.buffer.GpuBufferPool
import wtf.traumadev.beryllium.render.draw.FlushContext
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.render.draw.Renderer3D
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.pipeline.Pipelines
import wtf.traumadev.beryllium.render.pipeline.TransformUbo
import wtf.traumadev.beryllium.render.svg.Svgs
import wtf.traumadev.beryllium.render.texture.Samplers
import wtf.traumadev.beryllium.util.Logger
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture

object Renderer {
	val gui = Renderer2D()
	val hud = Renderer2D()
	val world = Renderer3D()

	var backend: GraphicsBackend = GraphicsBackend.UNKNOWN
		private set
	internal var initialized: Boolean = false
		private set

	var framesPerSecond: Float = 0f
		private set

	val fontPrewarmComplete: Boolean
		get() = Fonts.prewarmComplete

	val pipelinesReady: Boolean
		get() = Pipelines.all.all(::isPipelineReady)

	fun pipelineReady(pipeline: RenderPipeline): Boolean = isPipelineReady(pipeline)

	val ready: Boolean
		get() = initialized && fontPrewarmComplete && pipelinesReady

	private val transforms = TransformUbo()
	private val bufferPool = GpuBufferPool("Beryllium renderer", VERTEX_USAGE)
	private val uniformPool = GpuBufferPool("Beryllium uniforms", UNIFORM_USAGE, 256)
	private val frameTimes = ArrayDeque<Long>()
	private var lastFrameStart = 0L
	private val pipelineState = HashMap<RenderPipeline, PipelineState>()
	private var backendLogged = false
	private var pipelinesSettled = false
	private var pipelineCache: PipelineCache? = null
	private val compiledPipelines = HashMap<RenderPipeline, CompiledRenderPipeline>()

	internal fun initialize() {
		if (initialized) return
		Fonts.init()
		EventBus.subscribe<FrameEvent>(priority = 100) { event ->
			if (event.phase == FrameEvent.Phase.START) onFrameStart()
		}
		EventBus.subscribe<FrameEvent>(priority = -100) { event ->
			if (event.phase == FrameEvent.Phase.END) onFrameEnd()
		}
		EventBus.subscribe<WorldRenderEvent>(priority = -100) { event ->
			onWorldRender(event)
		}
		ClientLifecycleEvents.CLIENT_STOPPING.register { shutdown() }
		initialized = true
		Logger.info("Renderer initialized (waiting for graphics device)")
	}

	private fun shutdown() {
		bufferPool.close()
		uniformPool.close()
		transforms.close()
		Fonts.close()
		pipelineCache?.close()
		pipelineCache = null
		compiledPipelines.clear()
		pipelineState.clear()
		pipelinesSettled = false
		initialized = false
	}

	private fun onFrameStart() {
		if (!initialized) return
		val now = System.nanoTime()
		if (lastFrameStart != 0L) {
			frameTimes.addLast(now - lastFrameStart)
			if (frameTimes.size > 60) frameTimes.removeFirst()
			var total = 0L
			for (time in frameTimes) total += time
			framesPerSecond = if (total > 0) (frameTimes.size * 1_000_000_000L / total).toFloat() else 0f
		}
		lastFrameStart = now

		if (!backendLogged) {
			val device = RenderSystem.tryGetDevice()
			if (device != null) {
				backendLogged = true
				Samplers.init()
				backend = GraphicsBackend.detect()
				val info = device.deviceInfo
				Logger.info("Render backend: ${backend.display} (${info.name()} / ${info.vendorName()})")
			} else {
				Logger.warn("Frame started without a graphics device")
				return
			}
		}
		ensurePipelines()
		if (!Fonts.prewarmComplete) Fonts.prewarmStep(PREWARM_BUDGET_NANOS)

		val minecraft = Minecraft.getInstance()
		val window = minecraft.getWindow()
		gui.guiScale = window.guiScale.toFloat()
		gui.guiWidth = window.guiScaledWidth.toFloat()
		gui.guiHeight = window.guiScaledHeight.toFloat()
		hud.guiScale = gui.guiScale
		hud.guiWidth = gui.guiWidth
		hud.guiHeight = gui.guiHeight
	}

	fun flushHud() {
		if (!initialized) return
		val device = RenderSystem.tryGetDevice() ?: return
		if (hud.pendingVertices() == 0) return
		val minecraft = Minecraft.getInstance()
		val target = minecraft.gameRenderer.mainRenderTarget()
		val context = FlushContext(
			device.createCommandEncoder(),
			bufferPool,
			uniformPool,
			transforms,
			target,
			hud.guiWidth,
			hud.guiHeight,
			hud.guiScale,
			target.width,
			target.height,
			pipelineReady = { isPipelineReady(it) }
		)
		Fonts.uploadPending(context.encoder)
		Svgs.uploadPending(context.encoder)
		hud.flush(context)
	}

	private fun ensurePipelines() {
		val device = RenderSystem.tryGetDevice() ?: return
		val cache = pipelineCache ?: createPipelineCache(device).also { pipelineCache = it }
		if (!pipelinesSettled) {
			var failed = 0
			for (pipeline in Pipelines.all) {
				if (!compilePipeline(pipeline, cache)) failed++
			}
			pipelinesSettled = true
			if (failed == 0) {
				Logger.info("Renderer pipelines compiled for ${backend.display}")
			} else {
				Logger.error("$failed/${Pipelines.all.size} renderer pipelines failed to compile for ${backend.display}")
			}
		}
		for (pipeline in Pipelines.custom) {
			val state = pipelineState[pipeline]
			if (state == PipelineState.READY || state == PipelineState.DEAD) continue
			if (!compilePipeline(pipeline, cache)) {
				Logger.error("Failed to compile custom pipeline: $pipeline (its draws will be skipped)")
			}
		}
	}

	private fun compilePipeline(pipeline: RenderPipeline, cache: PipelineCache): Boolean {
		val compiled = try {
			cache.get(pipeline)
		} catch (throwable: Throwable) {
			Logger.error("Pipeline compilation threw: $pipeline", throwable)
			null
		}
		if (compiled == null) {
			pipelineState[pipeline] = PipelineState.DEAD
			return false
		}
		pipelineState[pipeline] = PipelineState.READY
		compiledPipelines[pipeline] = compiled
		return true
	}

	private fun createPipelineCache(device: com.mojang.renderpearl.api.device.GpuDevice): PipelineCache {
		val resourceManager = Minecraft.getInstance().resourceManager
		val includes = ShaderManager.listAllIncludes(resourceManager)
		val shaderSource = object : ShaderSource {
			override fun getShader(id: Identifier, type: ShaderType): String? {
				val location = type.idConverter().idToFile(id)
				return try {
					resourceManager.getResourceOrThrow(location).readAllAsString()
				} catch (exception: Exception) {
					Logger.error("Couldn't load shader $location", exception)
					null
				}
			}

			override fun getInclude(id: Identifier): ShaderSource.CachedIncludeSource? = includes[id]

			override fun close() {
				includes.values.forEach(ShaderSource.CachedIncludeSource::close)
			}
		}
		return PipelineCache(device, shaderSource)
	}

	internal fun compiledPipeline(pipeline: RenderPipeline): CompiledRenderPipeline? = compiledPipelines[pipeline]

	internal fun isPipelineReady(pipeline: RenderPipeline): Boolean = pipelineState[pipeline] == PipelineState.READY

	private enum class PipelineState {
		PENDING,
		READY,
		DEAD
	}

	internal fun renderTo(surface: RenderTarget2D, surfaceRenderer: Renderer2D, clearColor: Int?) {
		if (!initialized) return
		val device = RenderSystem.tryGetDevice() ?: return
		val encoder = device.createCommandEncoder()
		val colorTexture = surface.target.getColorTexture() ?: return
		if (clearColor != null) {
			encoder.clearColorTexture(
				colorTexture,
				org.joml.Vector4f(
					((clearColor shr 16) and 0xFF) / 255f,
					((clearColor shr 8) and 0xFF) / 255f,
					(clearColor and 0xFF) / 255f,
					((clearColor ushr 24) and 0xFF) / 255f
				)
			)
		}
		val context = FlushContext(
			encoder,
			bufferPool,
			uniformPool,
			surface.transforms,
			surface.target,
			surface.width.toFloat(),
			surface.height.toFloat(),
			1f,
			surface.width,
			surface.height,
			pipelineReady = { isPipelineReady(it) },
			flipY = false
		)
		Fonts.uploadPending(encoder)
		Svgs.uploadPending(encoder)
		surfaceRenderer.flush(context)
	}

	private fun onFrameEnd() {
		if (!initialized) return
		val device = RenderSystem.tryGetDevice() ?: return
		val minecraft = Minecraft.getInstance()
		val target = minecraft.gameRenderer.mainRenderTarget()
		val context = FlushContext(
			device.createCommandEncoder(),
			bufferPool,
			uniformPool,
			transforms,
			target,
			gui.guiWidth,
			gui.guiHeight,
			gui.guiScale,
			target.width,
			target.height,
			pipelineReady = { isPipelineReady(it) }
		)
		Fonts.uploadPending(context.encoder)
		Svgs.uploadPending(context.encoder)
		gui.flush(context)
		bufferPool.endFrame()
		uniformPool.endFrame()
	}

	private fun onWorldRender(event: WorldRenderEvent) {
		if (!initialized) return
		val device = RenderSystem.tryGetDevice() ?: return
		val minecraft = Minecraft.getInstance()
		val target = minecraft.gameRenderer.mainRenderTarget()
		val context = FlushContext(
			device.createCommandEncoder(),
			bufferPool,
			uniformPool,
			transforms,
			target,
			gui.guiWidth,
			gui.guiHeight,
			gui.guiScale,
			target.width,
			target.height,
			pipelineReady = { isPipelineReady(it) }
		)
		world.flush(context, event)
	}

	private const val VERTEX_USAGE: Int = com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_VERTEX or com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_COPY_DST
	private const val UNIFORM_USAGE: Int = com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_UNIFORM or com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_COPY_DST

	private const val PREWARM_BUDGET_NANOS = 1_000_000L
}
