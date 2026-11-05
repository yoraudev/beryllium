package wtf.traumadev.beryllium.render.pipeline

import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.CompareOp
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import com.mojang.renderpearl.api.vertex.VertexFormat
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.resources.Identifier
import wtf.traumadev.ModConstants
import wtf.traumadev.beryllium.util.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal object Pipelines {
	internal val transformLayout: BindGroupLayout = BindGroupLayout.builder()
		.withUniform(TransformUbo.UNIFORM_NAME, UniformType.UNIFORM_BUFFER)
		.build()

	internal val sampler0Layout: BindGroupLayout = BindGroupLayout.builder()
		.withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER)
		.build()

	private val sdfVertexFormat: VertexFormat = VertexFormat.builder(0)
		.addAttribute("Position", GpuFormat.RGB32_FLOAT)
		.addAttribute("Color", GpuFormat.RGBA8_UNORM)
		.addAttribute("LocalPos", GpuFormat.RG32_FLOAT)
		.addAttribute("HalfSize", GpuFormat.RG32_FLOAT)
		.addAttribute("Radius", GpuFormat.R32_FLOAT)
		.addAttribute("BorderWidth", GpuFormat.R32_FLOAT)
		.addAttribute("BorderColor", GpuFormat.RGBA8_UNORM)
		.addAttribute("ShadowColor", GpuFormat.RGBA8_UNORM)
		.addAttribute("ShadowParams", GpuFormat.RGBA32_FLOAT)
		.build()


	val overlayColored: RenderPipeline = builder("overlay_colored")
		.withBindGroupLayout(transformLayout)
		.withVertexShader(shaderId("core/colored"))
		.withFragmentShader(shaderId("core/colored"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val overlayTextured: RenderPipeline = builder("overlay_textured")
		.withBindGroupLayout(transformLayout)
		.withBindGroupLayout(sampler0Layout)
		.withVertexShader(shaderId("core/textured"))
		.withFragmentShader(shaderId("core/textured"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val overlayMsdf: RenderPipeline = builder("overlay_msdf")
		.withBindGroupLayout(transformLayout)
		.withBindGroupLayout(sampler0Layout)
		.withVertexShader(shaderId("core/msdf"))
		.withFragmentShader(shaderId("core/msdf"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val overlaySdf: RenderPipeline = builder("overlay_sdf")
		.withBindGroupLayout(transformLayout)
		.withVertexShader(shaderId("core/sdf"))
		.withFragmentShader(shaderId("core/sdf"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0, sdfVertexFormat)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val worldColored: RenderPipeline = builder("world_colored")
		.withBindGroupLayout(transformLayout)
		.withVertexShader(shaderId("core/colored"))
		.withFragmentShader(shaderId("core/colored"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val worldTextured: RenderPipeline = builder("world_textured")
		.withBindGroupLayout(transformLayout)
		.withBindGroupLayout(sampler0Layout)
		.withVertexShader(shaderId("core/textured"))
		.withFragmentShader(shaderId("core/textured"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.build()

	val worldLines: RenderPipeline = builder("world_lines")
		.withBindGroupLayout(transformLayout)
		.withVertexShader(shaderId("core/lines"))
		.withFragmentShader(shaderId("core/lines"))
		.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
		.withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
		.withPrimitiveTopology(PrimitiveTopology.LINES)
		.build()

	val all: List<RenderPipeline> = listOf(overlayColored, overlayTextured, overlayMsdf, overlaySdf, worldColored, worldTextured, worldLines)

	internal val custom: MutableList<RenderPipeline> = CopyOnWriteArrayList()
	private val customSamplers: MutableSet<RenderPipeline> = ConcurrentHashMap.newKeySet()

	internal fun registerCustom(pipeline: RenderPipeline, bindsSampler: Boolean) {
		if (all.contains(pipeline)) {
			Logger.warn("Render pipeline is already a built-in pipeline: $pipeline")
			return
		}
		if (!custom.contains(pipeline)) custom.add(pipeline)
		if (bindsSampler) customSamplers.add(pipeline)
	}

	internal fun isCustomRegistered(pipeline: RenderPipeline): Boolean = custom.contains(pipeline)

	internal fun bindsSampler(pipeline: RenderPipeline): Boolean =
		pipeline === overlayTextured || pipeline === overlayMsdf || pipeline === worldTextured || customSamplers.contains(pipeline)

	private fun builder(name: String): RenderPipeline.Builder =
		RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, name))

	private fun shaderId(path: String): Identifier = Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, path)
}
