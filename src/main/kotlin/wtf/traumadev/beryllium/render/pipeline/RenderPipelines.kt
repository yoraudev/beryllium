package wtf.traumadev.beryllium.render.pipeline

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.CompareOp
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import com.mojang.renderpearl.api.vertex.VertexFormat
import net.minecraft.resources.Identifier

object RenderPipelines {
	val transformsLayout: BindGroupLayout
		get() = Pipelines.transformLayout

	val samplerLayout: BindGroupLayout
		get() = Pipelines.sampler0Layout

	fun builder(location: Identifier): Builder = Builder(location)

	fun register(pipeline: RenderPipeline, bindsSampler: Boolean = false): RenderPipeline {
		Pipelines.registerCustom(pipeline, bindsSampler)
		return pipeline
	}

	fun isRegistered(pipeline: RenderPipeline): Boolean = Pipelines.isCustomRegistered(pipeline)

	class Builder internal constructor(private val location: Identifier) {
		private var vertexShader: Identifier? = null
		private var fragmentShader: Identifier? = null
		private var vertexFormat: VertexFormat = DefaultVertexFormat.POSITION_TEX_COLOR
		private var topology: PrimitiveTopology = PrimitiveTopology.QUADS
		private var blend: BlendFunction = BlendFunction.TRANSLUCENT
		private var depthTest: CompareOp? = null
		private var cull: Boolean = false
		private var sampler: Boolean = true
		private val extraUniforms = LinkedHashMap<String, UniformType>()

		fun vertexShader(id: Identifier): Builder = apply { this.vertexShader = id }

		fun fragmentShader(id: Identifier): Builder = apply { this.fragmentShader = id }

		fun vertexFormat(format: VertexFormat): Builder = apply { this.vertexFormat = format }

		fun topology(topology: PrimitiveTopology): Builder = apply { this.topology = topology }

		fun blend(blend: BlendFunction): Builder = apply { this.blend = blend }

		fun depthTest(compare: CompareOp?): Builder = apply { this.depthTest = compare }

		fun cull(cull: Boolean): Builder = apply { this.cull = cull }

		fun sampler(sampler: Boolean): Builder = apply { this.sampler = sampler }

		fun uniformBuffer(name: String): Builder = apply { declare(name, UniformType.UNIFORM_BUFFER) }

		fun texture(name: String): Builder = apply { declare(name, UniformType.COMBINED_IMAGE_SAMPLER) }

		private fun declare(name: String, type: UniformType) {
			require(name.isNotBlank()) { "Bind name must not be blank" }
			require(name != TransformUbo.UNIFORM_NAME) { "$name is reserved for the transform block" }
			require(name != "Sampler0") { "Sampler0 is reserved for the primary texture binding" }
			extraUniforms[name] = type
		}

		fun build(): RenderPipeline {
			val vertex = vertexShader ?: error("Vertex shader is required")
			val fragment = fragmentShader ?: error("Fragment shader is required")
			val builder = RenderPipeline.builder()
				.withLocation(location)
				.withBindGroupLayout(Pipelines.transformLayout)
				.withVertexShader(vertex)
				.withFragmentShader(fragment)
				.withColorTargetState(ColorTargetState(blend))
				.withCull(cull)
				.withVertexBinding(0, vertexFormat)
				.withPrimitiveTopology(topology)
			depthTest?.let { builder.withDepthStencilState(DepthStencilState(it, false)) }
			if (sampler) builder.withBindGroupLayout(Pipelines.sampler0Layout)
			if (extraUniforms.isNotEmpty()) {
				val layout = BindGroupLayout.builder()
				for ((name, type) in extraUniforms) layout.withUniform(name, type)
				builder.withBindGroupLayout(layout.build())
			}
			return builder.build()
		}

		fun register(): RenderPipeline = RenderPipelines.register(build(), sampler)
	}
}