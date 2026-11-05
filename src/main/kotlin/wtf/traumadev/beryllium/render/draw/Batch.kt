package wtf.traumadev.beryllium.render.draw

import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import wtf.traumadev.beryllium.render.buffer.GpuBufferPool
import wtf.traumadev.beryllium.render.pipeline.ShaderUniforms
import wtf.traumadev.beryllium.render.texture.Samplers
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.beryllium.render.vertex.VertexWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class BoundUniform(val name: String, val buffer: GpuBufferSlice?, val texture: Texture2D?, val sampling: TextureSampling)

internal class PreparedBatch(val batch: Batch, val slice: GpuBufferSlice, val uniforms: List<BoundUniform>)

internal class Batch(
	var pipeline: RenderPipeline,
	var texture: Texture2D?,
	var topology: PrimitiveTopology,
	var scissor: ScissorRect?,
	var sampling: TextureSampling,
	var uniforms: ShaderUniforms?
) {
	val writer = VertexWriter(INITIAL_CAPACITY)

	val isEmpty: Boolean
		get() = writer.vertexCount == 0 || scissor?.isEmpty == true

	fun reset(
		pipeline: RenderPipeline,
		texture: Texture2D?,
		topology: PrimitiveTopology,
		scissor: ScissorRect?,
		sampling: TextureSampling,
		uniforms: ShaderUniforms?
	) {
		this.pipeline = pipeline
		this.texture = texture
		this.topology = topology
		this.scissor = scissor
		this.sampling = sampling
		this.uniforms = uniforms
		writer.reset()
	}

	companion object {
		private const val INITIAL_CAPACITY = 8192
	}
}

internal fun prepareUniforms(uniforms: ShaderUniforms?, encoder: CommandEncoder, pool: GpuBufferPool): List<BoundUniform> {
	if (uniforms == null || uniforms.isEmpty) return emptyList()
	val bindings = ArrayList<BoundUniform>(uniforms.entries.size)
	for ((name, entry) in uniforms.entries) {
		when (entry) {
			is ShaderUniforms.BufferEntry -> {
				val data = entry.data.duplicate()
				val size = data.remaining()
				val sliceSize = (size + 15) / 16 * 16
				val buffer = pool.acquire(sliceSize)
				val slice = buffer.slice(0, sliceSize.toLong())
				if (sliceSize == size) {
					encoder.writeToBuffer(slice, data)
				} else {
					val padded = ByteBuffer.allocateDirect(sliceSize).order(ByteOrder.nativeOrder())
					padded.put(data)
					padded.flip()
					encoder.writeToBuffer(slice, padded)
				}
				bindings.add(BoundUniform(name, slice, null, TextureSampling.LINEAR))
			}

			is ShaderUniforms.TextureEntry -> {
				bindings.add(BoundUniform(name, null, entry.texture, entry.sampling))
			}
		}
	}
	return bindings
}

internal fun RenderPass.bindUniforms(bindings: List<BoundUniform>) {
	for (binding in bindings) {
		val buffer = binding.buffer
		if (buffer != null) {
			setUniform(binding.name, buffer)
			continue
		}
		val texture = binding.texture ?: continue
		setUniform(binding.name, texture.view, Samplers.of(binding.sampling))
	}
}