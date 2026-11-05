package wtf.traumadev.beryllium.render.draw

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Vector3f
import wtf.traumadev.beryllium.event.impl.WorldRenderEvent
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.pipeline.Pipelines
import wtf.traumadev.beryllium.render.pipeline.ShaderUniforms
import wtf.traumadev.beryllium.render.texture.Samplers
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.beryllium.render.vertex.VertexWriter
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.sqrt

class Renderer3D {
	private val batches = ArrayList<Batch>()
	private val batchPool = ArrayList<Batch>()

	internal fun pendingVertices(): Int = batches.sumOf { it.writer.vertexCount }
	fun line(
		x1: Double, y1: Double, z1: Double,
		x2: Double, y2: Double, z2: Double,
		thicknessPx: Float,
		color: Int,
		event: WorldRenderEvent
	) {
		val dx = (x2 - x1).toFloat()
		val dy = (y2 - y1).toFloat()
		val dz = (z2 - z1).toFloat()
		val length = sqrt(dx * dx + dy * dy + dz * dz)
		if (length < 1e-5f) return
		val writer = batch(Pipelines.worldLines, null, PrimitiveTopology.LINES, TextureSampling.LINEAR, null).writer
		val nx = (dx / length * 127f).toInt().toByte()
		val ny = (dy / length * 127f).toInt().toByte()
		val nz = (dz / length * 127f).toInt().toByte()
		val startX = (x1 - event.cameraX).toFloat()
		val startY = (y1 - event.cameraY).toFloat()
		val startZ = (z1 - event.cameraZ).toFloat()
		val endX = (x2 - event.cameraX).toFloat()
		val endY = (y2 - event.cameraY).toFloat()
		val endZ = (z2 - event.cameraZ).toFloat()
		putLineVertex(writer, startX, startY, startZ, color, nx, ny, nz, thicknessPx)
		putLineVertex(writer, endX, endY, endZ, color, nx, ny, nz, thicknessPx)
		putLineVertex(writer, endX, endY, endZ, color, nx, ny, nz, thicknessPx)
		putLineVertex(writer, startX, startY, startZ, color, nx, ny, nz, thicknessPx)
	}

	fun line(start: Vector3f, end: Vector3f, thicknessPx: Float, color: Int, event: WorldRenderEvent) {
		line(
			start.x.toDouble(), start.y.toDouble(), start.z.toDouble(),
			end.x.toDouble(), end.y.toDouble(), end.z.toDouble(),
			thicknessPx, color, event
		)
	}

	fun boxEdges(
		minX: Double, minY: Double, minZ: Double,
		maxX: Double, maxY: Double, maxZ: Double,
		thicknessPx: Float,
		color: Int,
		event: WorldRenderEvent
	) {
		line(minX, minY, minZ, maxX, minY, minZ, thicknessPx, color, event)
		line(maxX, minY, minZ, maxX, minY, maxZ, thicknessPx, color, event)
		line(maxX, minY, maxZ, minX, minY, maxZ, thicknessPx, color, event)
		line(minX, minY, maxZ, minX, minY, minZ, thicknessPx, color, event)
		line(minX, maxY, minZ, maxX, maxY, minZ, thicknessPx, color, event)
		line(maxX, maxY, minZ, maxX, maxY, maxZ, thicknessPx, color, event)
		line(maxX, maxY, maxZ, minX, maxY, maxZ, thicknessPx, color, event)
		line(minX, maxY, maxZ, minX, maxY, minZ, thicknessPx, color, event)
		line(minX, minY, minZ, minX, maxY, minZ, thicknessPx, color, event)
		line(maxX, minY, minZ, maxX, maxY, minZ, thicknessPx, color, event)
		line(maxX, minY, maxZ, maxX, maxY, maxZ, thicknessPx, color, event)
		line(minX, minY, maxZ, minX, maxY, maxZ, thicknessPx, color, event)
	}

	fun boxFaces(
		minX: Double, minY: Double, minZ: Double,
		maxX: Double, maxY: Double, maxZ: Double,
		color: Int,
		event: WorldRenderEvent
	) {
		quad(
			minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
			color, event
		)
		quad(
			minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
			color, event
		)
		quad(
			minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
			color, event
		)
		quad(
			maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
			color, event
		)
		quad(
			minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
			color, event
		)
		quad(
			minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
			color, event
		)
	}

	fun quad(
		x1: Double, y1: Double, z1: Double,
		x2: Double, y2: Double, z2: Double,
		x3: Double, y3: Double, z3: Double,
		x4: Double, y4: Double, z4: Double,
		color: Int,
		event: WorldRenderEvent,
		pipeline: RenderPipeline = Pipelines.worldColored,
		uniforms: ShaderUniforms? = null
	) {
		val writer = batch(pipeline, null, PrimitiveTopology.QUADS, TextureSampling.LINEAR, uniforms).writer
		putWorldVertex(writer, x1, y1, z1, color, event)
		putWorldVertex(writer, x2, y2, z2, color, event)
		putWorldVertex(writer, x3, y3, z3, color, event)
		putWorldVertex(writer, x4, y4, z4, color, event)
	}

	fun imageQuad(
		texture: Texture2D,
		x1: Double, y1: Double, z1: Double,
		x2: Double, y2: Double, z2: Double,
		x3: Double, y3: Double, z3: Double,
		x4: Double, y4: Double, z4: Double,
		color: Int,
		event: WorldRenderEvent,
		pipeline: RenderPipeline = Pipelines.worldTextured,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	) {
		val writer = batch(pipeline, texture, PrimitiveTopology.QUADS, sampling, uniforms).writer
		putWorldTextured(writer, x1, y1, z1, 0f, 0f, color, event)
		putWorldTextured(writer, x2, y2, z2, 1f, 0f, color, event)
		putWorldTextured(writer, x3, y3, z3, 1f, 1f, color, event)
		putWorldTextured(writer, x4, y4, z4, 0f, 1f, color, event)
	}

	private fun batch(
		pipeline: RenderPipeline,
		texture: Texture2D?,
		topology: PrimitiveTopology,
		sampling: TextureSampling,
		uniforms: ShaderUniforms?
	): Batch {
		val last = batches.lastOrNull()
		if (last != null && last.pipeline == pipeline && last.texture === texture && last.topology == topology &&
			last.sampling == sampling && last.uniforms === uniforms
		) {
			return last
		}
		val batch = batchPool.removeLastOrNull() ?: Batch(pipeline, texture, topology, null, sampling, uniforms)
		batch.reset(pipeline, texture, topology, null, sampling, uniforms)
		batches.add(batch)
		return batch
	}

	private fun recycleBatches() {
		batchPool.addAll(batches)
		batches.clear()
	}

	private fun putWorldVertex(writer: VertexWriter, x: Double, y: Double, z: Double, color: Int, event: WorldRenderEvent) {
		writer.putFloats((x - event.cameraX).toFloat(), (y - event.cameraY).toFloat(), (z - event.cameraZ).toFloat())
		writer.putColor(color)
		writer.endVertex()
	}

	private fun putWorldTextured(writer: VertexWriter, x: Double, y: Double, z: Double, u: Float, v: Float, color: Int, event: WorldRenderEvent) {
		writer.putFloats((x - event.cameraX).toFloat(), (y - event.cameraY).toFloat(), (z - event.cameraZ).toFloat())
		writer.putFloats(u, v)
		writer.putColor(color)
		writer.endVertex()
	}

	private fun putLineVertex(writer: VertexWriter, x: Float, y: Float, z: Float, color: Int, nx: Byte, ny: Byte, nz: Byte, width: Float) {
		writer.putFloats(x, y, z)
		writer.putColor(color)
		writer.putByte(nx.toInt())
		writer.putByte(ny.toInt())
		writer.putByte(nz.toInt())
		writer.putByte(0)
		writer.putFloat(width)
		writer.endVertex()
	}

	internal fun flush(context: FlushContext, event: WorldRenderEvent) {
		if (batches.isEmpty()) return

		context.transforms.update(
			context.encoder,
			event.projection,
			event.viewRotation,
			context.framebufferWidth.toFloat(),
			context.framebufferHeight.toFloat()
		)

		val prepared = ArrayList<PreparedBatch>(batches.size)
		for (candidate in batches) {
			if (candidate.isEmpty || !context.pipelineReady(candidate.pipeline)) continue
			val sizeBytes = candidate.writer.sizeBytes
			val vertexBuffer = context.bufferPool.acquire(maxOf(sizeBytes, 16))
			context.encoder.writeToBuffer(vertexBuffer.slice(0, sizeBytes.toLong()), candidate.writer.view())
			prepared.add(
				PreparedBatch(
					candidate,
					vertexBuffer.slice(0, sizeBytes.toLong()),
					prepareUniforms(candidate.uniforms, context.encoder, context.uniformPool)
				)
			)
		}
		if (prepared.isEmpty()) {
			recycleBatches()
			return
		}

		context.encoder.createRenderPass(
			{ "Beryllium world overlay" },
			requireNotNull(context.target.getColorTextureView()),
			Optional.empty(),
			context.target.getDepthTextureView(),
			OptionalDouble.empty()
		).use { pass ->
			context.transforms.bind(pass)
			for (item in prepared) {
				val batch = item.batch
				Renderer.compiledPipeline(batch.pipeline)?.let { pass.setPipeline(it) } ?: return@use
				pass.bindUniforms(item.uniforms)
				val texture = batch.texture
				if (texture != null && Pipelines.bindsSampler(batch.pipeline)) {
					pass.setUniform("Sampler0", texture.view, Samplers.of(batch.sampling))
				}
				pass.setVertexBuffer(0, item.slice)
				when {
					batch.topology == PrimitiveTopology.QUADS -> {
						val indexCount = batch.writer.vertexCount / 4 * 6
						val sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
						pass.setIndexBuffer(sequential.getBuffer(indexCount), sequential.type())
						pass.drawIndexed(indexCount, 1, 0, 0, 0)
					}

					batch.pipeline === Pipelines.worldLines -> {
						val indexCount = batch.writer.vertexCount / 4 * 6
						val sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.LINES)
						pass.setIndexBuffer(sequential.getBuffer(indexCount), sequential.type())
						pass.drawIndexed(indexCount, 1, 0, 0, 0)
					}

					else -> pass.draw(batch.writer.vertexCount, 1, 0, 0)
				}
			}
		}

		recycleBatches()
	}
}
