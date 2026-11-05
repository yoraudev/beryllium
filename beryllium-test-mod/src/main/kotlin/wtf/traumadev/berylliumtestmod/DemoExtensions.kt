package wtf.traumadev.berylliumtestmod

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.vertex.VertexFormat
import net.minecraft.resources.Identifier
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.pipeline.RenderPipelines
import wtf.traumadev.beryllium.render.pipeline.ShaderUniforms
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.beryllium.util.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DemoExtensions {
	private const val NAMESPACE = "berylliumtestmod"

	val gradientPipeline: RenderPipeline by lazy {
		RenderPipelines.builder(Identifier.fromNamespaceAndPath(NAMESPACE, "demo_gradient"))
			.vertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/demo_gradient"))
			.fragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/demo_gradient"))
			.vertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR)
			.sampler(true)
			.register()
	}

	private val meshFormat: VertexFormat = VertexFormat.builder(0)
		.addAttribute("Position", GpuFormat.RGB32_FLOAT)
		.addAttribute("UV0", GpuFormat.RG32_FLOAT)
		.build()

	val meshPipeline: RenderPipeline by lazy {
		RenderPipelines.builder(Identifier.fromNamespaceAndPath(NAMESPACE, "demo_mesh"))
			.vertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/demo_mesh"))
			.fragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/demo_mesh"))
			.vertexFormat(meshFormat)
			.topology(PrimitiveTopology.TRIANGLES)
			.sampler(false)
			.uniformBuffer("TintBlock")
			.texture("Mask")
			.register()
	}

	val meshUniforms: ShaderUniforms by lazy {
		ShaderUniforms()
			.uniform("TintBlock", floatArrayOf(1f, 1f, 1f, 1f))
			.texture("Mask", stripeTexture, TextureSampling.NEAREST)
	}

	private fun triangleAt(x: Float, y: Float): FloatArray = floatArrayOf(
		x, y, 0f, 0f, 0f,
		x + 100f, y, 0f, 1f, 0f,
		x + 50f, y + 100f, 0f, 0.5f, 1f
	)

	val meshVertices: FloatArray = triangleAt(260f, 20f)

	val galleryMeshVertices: FloatArray = triangleAt(360f, 145f)

	val stripeTexture: Texture2D by lazy {
		val texture = Texture2D.create("demo-stripe", 2, 1)
		val pixels = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
		pixels.put(byteArrayOf(255.toByte(), 0, 0, 255.toByte(), 0, 0, 255.toByte(), 255.toByte()))
		pixels.flip()
		texture.upload(pixels)
		texture
	}

	val demoFont: FontRenderer? by lazy {
		if (!Fonts.nativeAvailable) {
			Logger.warn("Demo font skipped: native font library unavailable")
			return@lazy null
		}
		val stream = DemoExtensions::class.java.getResourceAsStream("/assets/$NAMESPACE/fonts/Oswald-Regular.ttf")
		if (stream == null) {
			Logger.warn("Demo font resource missing")
			return@lazy null
		}
		val data = stream.use { it.readBytes() }
		runCatching { Fonts.register("demo", data) }.getOrElse {
			Logger.warn("Demo font registration failed: ${it.message}")
			Fonts.find("demo")
		}
	}

	fun initialize() {
		gradientPipeline
		meshPipeline
		demoFont
	}
}