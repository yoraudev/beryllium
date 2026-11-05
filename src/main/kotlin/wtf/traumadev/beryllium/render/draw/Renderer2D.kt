package wtf.traumadev.beryllium.render.draw

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector3f
import wtf.traumadev.beryllium.render.Colors
import wtf.traumadev.beryllium.render.Renderer
import wtf.traumadev.beryllium.render.font.FontRenderer
import wtf.traumadev.beryllium.render.font.Fonts
import wtf.traumadev.beryllium.render.pipeline.Pipelines
import wtf.traumadev.beryllium.render.pipeline.ShaderUniforms
import wtf.traumadev.beryllium.render.texture.Samplers
import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureSampling
import wtf.traumadev.beryllium.render.vertex.VertexWriter
import wtf.traumadev.beryllium.util.Logger
import java.util.ArrayDeque
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Renderer2D {
	var guiScale: Float = 1f
	var guiWidth: Float = 0f
	var guiHeight: Float = 0f

	private val activeBatches = ArrayList<Batch>()
	private val batchPool = ArrayList<Batch>()
	private val preparedBatches = ArrayList<PreparedBatch>()
	private val matrixStack = Matrix4fStack(TRANSFORM_STACK_DEPTH)
	private val scissorStack = ArrayDeque<ScissorRect>()
	private val transform = Vector3f()
	private val identity = Matrix4f()
	private val projection = Matrix4f()
	private var depth = 1
	private var firstDrawLogged = false

	fun pushMatrix() {
		check(depth < TRANSFORM_STACK_DEPTH) { "Transform stack overflow" }
		matrixStack.pushMatrix()
		depth++
	}

	fun popMatrix() {
		if (depth > 1) {
			matrixStack.popMatrix()
			depth--
		}
	}

	fun translate(x: Float, y: Float, z: Float = 0f) {
		matrixStack.translate(x, y, z)
	}

	fun rotateZ(angleRadians: Float) {
		matrixStack.rotateZ(angleRadians)
	}

	fun scale(x: Float, y: Float) {
		matrixStack.scale(x, y, 1f)
	}

	fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
		val requested = ScissorRect(floor(x).toInt(), floor(y).toInt(), ceil(width).toInt(), ceil(height).toInt())
		val current = scissorStack.peekLast()
		val resolved = if (current != null) current.intersect(requested) else requested
		scissorStack.addLast(resolved)
	}

	fun popScissor() {
		if (scissorStack.isNotEmpty()) scissorStack.removeLast()
	}

	fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) {
		coloredQuad(x, y, x + width, y, x + width, y + height, x, y + height, color, color, color, color)
	}

	fun gradientRect(x: Float, y: Float, width: Float, height: Float, topColor: Int, bottomColor: Int) {
		coloredQuad(x, y, x + width, y, x + width, y + height, x, y + height, topColor, topColor, bottomColor, bottomColor)
	}

	fun gradientRectH(x: Float, y: Float, width: Float, height: Float, leftColor: Int, rightColor: Int) {
		coloredQuad(x, y, x + width, y, x + width, y + height, x, y + height, leftColor, rightColor, rightColor, leftColor)
	}

	fun outline(x: Float, y: Float, width: Float, height: Float, color: Int, thickness: Float = 1f) {
		rect(x, y, width, thickness, color)
		rect(x, y + height - thickness, width, thickness, color)
		rect(x, y + thickness, thickness, height - thickness * 2f, color)
		rect(x + width - thickness, y + thickness, thickness, height - thickness * 2f, color)
	}

	fun roundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Int) {
		val r = radius.coerceIn(0f, min(width, height) / 2f)
		if (r < 0.5f) {
			rect(x, y, width, height, color)
			return
		}
		rect(x + r, y, width - r * 2f, height, color)
		rect(x, y + r, r, height - r * 2f, color)
		rect(x + width - r, y + r, r, height - r * 2f, color)
		val quarter = (PI / 2f).toFloat()
		cornerFan(x + r, y + r, r, PI.toFloat(), 3f * quarter, color)
		cornerFan(x + width - r, y + r, r, 3f * quarter, 2f * PI.toFloat(), color)
		cornerFan(x + width - r, y + height - r, r, 0f, quarter, color)
		cornerFan(x + r, y + height - r, r, quarter, PI.toFloat(), color)
	}

	fun sdfRect(
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		radius: Float,
		fill: Int,
		fillBottom: Int = fill,
		borderColor: Int = Colors.TRANSPARENT,
		borderWidth: Float = 0f,
		shadowColor: Int = Colors.TRANSPARENT,
		shadowOffsetX: Float = 0f,
		shadowOffsetY: Float = 0f,
		shadowSpread: Float = 0f
	) {
		val batch = batch(Pipelines.overlaySdf, null, PrimitiveTopology.QUADS)
		val writer = batch.writer
		val halfWidth = width / 2f
		val halfHeight = height / 2f
		val clampedRadius = radius.coerceIn(0f, min(halfWidth, halfHeight))
		val inflateX = shadowSpread + abs(shadowOffsetX)
		val inflateY = shadowSpread + abs(shadowOffsetY)
		val left = x - inflateX
		val top = y - inflateY
		val right = x + width + inflateX
		val bottom = y + height + inflateY
		putSdfVertex(writer, left, top, -halfWidth - inflateX, -halfHeight - inflateY, halfWidth, halfHeight, clampedRadius, borderWidth, borderColor, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread, fill)
		putSdfVertex(writer, right, top, halfWidth + inflateX, -halfHeight - inflateY, halfWidth, halfHeight, clampedRadius, borderWidth, borderColor, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread, fill)
		putSdfVertex(writer, right, bottom, halfWidth + inflateX, halfHeight + inflateY, halfWidth, halfHeight, clampedRadius, borderWidth, borderColor, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread, fillBottom)
		putSdfVertex(writer, left, bottom, -halfWidth - inflateX, halfHeight + inflateY, halfWidth, halfHeight, clampedRadius, borderWidth, borderColor, shadowColor, shadowOffsetX, shadowOffsetY, shadowSpread, fillBottom)
	}

	private fun putSdfVertex(
		writer: VertexWriter,
		x: Float,
		y: Float,
		localX: Float,
		localY: Float,
		halfWidth: Float,
		halfHeight: Float,
		radius: Float,
		borderWidth: Float,
		borderColor: Int,
		shadowColor: Int,
		shadowOffsetX: Float,
		shadowOffsetY: Float,
		shadowSpread: Float,
		fill: Int
	) {
		transform.set(x, y, 0f).mulPosition(matrixStack)
		writer.putFloats(transform.x, transform.y, transform.z)
		writer.putColor(fill)
		writer.putFloats(localX, localY)
		writer.putFloats(halfWidth, halfHeight)
		writer.putFloat(radius)
		writer.putFloat(borderWidth)
		writer.putColor(borderColor)
		writer.putColor(shadowColor)
		writer.putFloats(shadowOffsetX, shadowOffsetY, shadowSpread, 0f)
		writer.endVertex()
	}

	fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
		val batch = batch(Pipelines.overlayColored, null, PrimitiveTopology.TRIANGLES)
		val writer = batch.writer
		putColored(writer, x1, y1, color)
		putColored(writer, x2, y2, color)
		putColored(writer, x3, y3, color)
	}

	fun circle(centerX: Float, centerY: Float, radius: Float, color: Int, segments: Int = 32) {
		arc(centerX, centerY, radius, 0f, 2f * PI.toFloat(), color, segments)
	}

	fun arc(centerX: Float, centerY: Float, radius: Float, startRadians: Float, endRadians: Float, color: Int, segments: Int = 32) {
		if (radius <= 0f || endRadians <= startRadians) return
		val batch = batch(Pipelines.overlayColored, null, PrimitiveTopology.TRIANGLES)
		val writer = batch.writer
		for (segment in 0 until segments) {
			val a0 = startRadians + (endRadians - startRadians) * segment / segments
			val a1 = startRadians + (endRadians - startRadians) * (segment + 1) / segments
			putColored(writer, centerX, centerY, color)
			putColored(writer, centerX + cos(a0) * radius, centerY + sin(a0) * radius, color)
			putColored(writer, centerX + cos(a1) * radius, centerY + sin(a1) * radius, color)
		}
	}

	fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
		val dx = x2 - x1
		val dy = y2 - y1
		val length = sqrt(dx * dx + dy * dy)
		if (length < 1e-5f) return
		val halfThickness = thickness / 2f
		val nx = -dy / length * halfThickness
		val ny = dx / length * halfThickness
		coloredQuad(
			x1 + nx, y1 + ny,
			x2 + nx, y2 + ny,
			x2 - nx, y2 - ny,
			x1 - nx, y1 - ny,
			color, color, color, color
		)
	}

	fun image(
		texture: Texture2D,
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		color: Int = Colors.WHITE,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	) {
		texturedQuad(texture, x, y, width, height, 0f, 0f, 1f, 1f, color, sampling = sampling, uniforms = uniforms)
	}

	fun imageRegion(
		texture: Texture2D,
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		u0: Float,
		v0: Float,
		u1: Float,
		v1: Float,
		color: Int = Colors.WHITE,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	) {
		texturedQuad(texture, x, y, width, height, u0, v0, u1, v1, color, sampling = sampling, uniforms = uniforms)
	}

	fun texturedQuad(
		texture: Texture2D,
		x: Float,
		y: Float,
		width: Float,
		height: Float,
		u0: Float,
		v0: Float,
		u1: Float,
		v1: Float,
		color: Int,
		pipeline: RenderPipeline = Pipelines.overlayTextured,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	) {
		val batch = batch(pipeline, texture, PrimitiveTopology.QUADS, sampling, uniforms)
		val writer = batch.writer
		putTextured(writer, x, y, u0, v0, color)
		putTextured(writer, x + width, y, u1, v0, color)
		putTextured(writer, x + width, y + height, u1, v1, color)
		putTextured(writer, x, y + height, u0, v1, color)
	}

	fun text(
		text: String,
		x: Float,
		y: Float,
		size: Float,
		color: Int = Colors.WHITE,
		font: FontRenderer = Fonts.regular,
		shadow: Boolean = false
	): Float = font.draw(this, text, x, y, size, color, shadow)

	fun textWidth(text: String, size: Float, font: FontRenderer = Fonts.regular): Float = font.textWidth(text, size)

	fun quad(
		x1: Float, y1: Float,
		x2: Float, y2: Float,
		x3: Float, y3: Float,
		x4: Float, y4: Float,
		color: Int,
		pipeline: RenderPipeline = Pipelines.overlayColored,
		uniforms: ShaderUniforms? = null
	) {
		coloredQuad(x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color, pipeline, uniforms)
	}

	fun mesh(
		pipeline: RenderPipeline,
		mesh: Mesh,
		texture: Texture2D? = null,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	) {
		val batch = batch(pipeline, texture, mesh.topology, sampling, uniforms)
		batch.writer.putVertices(mesh.data, mesh.vertexCount)
	}

	private fun coloredQuad(
		x0: Float, y0: Float,
		x1: Float, y1: Float,
		x2: Float, y2: Float,
		x3: Float, y3: Float,
		c0: Int, c1: Int, c2: Int, c3: Int,
		pipeline: RenderPipeline = Pipelines.overlayColored,
		uniforms: ShaderUniforms? = null
	) {
		val batch = batch(pipeline, null, PrimitiveTopology.QUADS, TextureSampling.LINEAR, uniforms)
		val writer = batch.writer
		putColored(writer, x0, y0, c0)
		putColored(writer, x1, y1, c1)
		putColored(writer, x2, y2, c2)
		putColored(writer, x3, y3, c3)
	}

	private fun cornerFan(centerX: Float, centerY: Float, radius: Float, startRadians: Float, endRadians: Float, color: Int) {
		val batch = batch(Pipelines.overlayColored, null, PrimitiveTopology.TRIANGLES)
		val writer = batch.writer
		for (segment in 0 until CORNER_SEGMENTS) {
			val a0 = startRadians + (endRadians - startRadians) * segment / CORNER_SEGMENTS
			val a1 = startRadians + (endRadians - startRadians) * (segment + 1) / CORNER_SEGMENTS
			putColored(writer, centerX, centerY, color)
			putColored(writer, centerX + cos(a0) * radius, centerY + sin(a0) * radius, color)
			putColored(writer, centerX + cos(a1) * radius, centerY + sin(a1) * radius, color)
		}
	}

	private fun putColored(writer: VertexWriter, x: Float, y: Float, color: Int) {
		transform.set(x, y, 0f).mulPosition(matrixStack)
		writer.putFloats(transform.x, transform.y, transform.z)
		writer.putColor(color)
		writer.endVertex()
	}

	private fun putTextured(writer: VertexWriter, x: Float, y: Float, u: Float, v: Float, color: Int) {
		transform.set(x, y, 0f).mulPosition(matrixStack)
		writer.putFloats(transform.x, transform.y, transform.z)
		writer.putFloats(u, v)
		writer.putColor(color)
		writer.endVertex()
	}

	private fun batch(
		pipeline: RenderPipeline,
		texture: Texture2D?,
		topology: PrimitiveTopology,
		sampling: TextureSampling = TextureSampling.LINEAR,
		uniforms: ShaderUniforms? = null
	): Batch {
		val scissor = scissorStack.peekLast()
		val last = activeBatches.lastOrNull()
		if (last != null && last.pipeline == pipeline && last.texture === texture && last.topology == topology &&
			last.scissor == scissor && last.sampling == sampling && last.uniforms === uniforms
		) {
			return last
		}
		val batch = batchPool.removeLastOrNull() ?: Batch(pipeline, texture, topology, scissor, sampling, uniforms)
		batch.reset(pipeline, texture, topology, scissor, sampling, uniforms)
		activeBatches.add(batch)
		return batch
	}

	internal fun pendingVertices(): Int = activeBatches.sumOf { it.writer.vertexCount }

	internal fun discard() {
		recycleBatches()
	}

	private fun recycleBatches() {
		batchPool.addAll(activeBatches)
		activeBatches.clear()
	}

	internal fun flush(context: FlushContext) {
		if (activeBatches.isEmpty()) return
		val width = context.guiWidth
		val height = context.guiHeight
		val yScale = if (context.flipY) -2f / height else 2f / height
		val yOffset = if (context.flipY) 1f else -1f
		projection.set(
			2f / width, 0f, 0f, 0f,
			0f, yScale, 0f, 0f,
			0f, 0f, 1f, 0f,
			-1f, yOffset, 0f, 1f
		)
		context.transforms.update(context.encoder, projection, identity, context.framebufferWidth.toFloat(), context.framebufferHeight.toFloat())

		preparedBatches.clear()
		for (batch in activeBatches) {
			if (batch.isEmpty) continue
			if (!context.pipelineReady(batch.pipeline)) continue
			val sizeBytes = batch.writer.sizeBytes
			val vertexBuffer = context.bufferPool.acquire(max(sizeBytes, 16))
			context.encoder.writeToBuffer(vertexBuffer.slice(0, sizeBytes.toLong()), batch.writer.view())
			preparedBatches.add(
				PreparedBatch(
					batch,
					vertexBuffer.slice(0, sizeBytes.toLong()),
					prepareUniforms(batch.uniforms, context.encoder, context.uniformPool)
				)
			)
		}
		if (preparedBatches.isEmpty()) {
			recycleBatches()
			return
		}

		context.encoder.createRenderPass(
			{ "Beryllium overlay" },
			requireNotNull(context.target.getColorTextureView()),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		).use { pass ->
			context.transforms.bind(pass)
			for (item in preparedBatches) {
				val batch = item.batch
				Renderer.compiledPipeline(batch.pipeline)?.let { pass.setPipeline(it) } ?: return@use
				pass.bindUniforms(item.uniforms)
				val texture = batch.texture
				if (texture != null && Pipelines.bindsSampler(batch.pipeline)) {
					pass.setUniform("Sampler0", texture.view, Samplers.of(batch.sampling))
				}
				applyScissor(pass, batch.scissor, context)
				pass.setVertexBuffer(0, item.slice)
				when (batch.topology) {
					PrimitiveTopology.QUADS -> {
						val indexCount = batch.writer.vertexCount / 4 * 6
						val sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
						pass.setIndexBuffer(sequential.getBuffer(indexCount), sequential.type())
						pass.drawIndexed(indexCount, 1, 0, 0, 0)
					}
					else -> pass.draw(batch.writer.vertexCount, 1, 0, 0)
				}
			}
		}
		if (!firstDrawLogged && activeBatches.isNotEmpty()) {
			firstDrawLogged = true
			Logger.info("Overlay draw OK (${preparedBatches.size} batch(es), ${activeBatches.sumOf { it.writer.vertexCount }} vertices)")
		}
		recycleBatches()
	}

	private fun applyScissor(pass: RenderPass, scissor: ScissorRect?, context: FlushContext) {
		if (scissor == null || scissor.isEmpty) {
			pass.disableScissor()
			return
		}
		val scale = context.guiScale
		val left = floor(scissor.x * scale).toInt().coerceIn(0, context.framebufferWidth - 1)
		val right = ceil((scissor.x + scissor.width) * scale).toInt().coerceIn(1, context.framebufferWidth)
		val bottom: Int
		val top: Int
		if (context.flipY) {
			bottom = floor(context.framebufferHeight - (scissor.y + scissor.height) * scale).toInt().coerceIn(0, context.framebufferHeight - 1)
			top = ceil(context.framebufferHeight - scissor.y * scale).toInt().coerceIn(bottom + 1, context.framebufferHeight)
		} else {
			bottom = floor(scissor.y * scale).toInt().coerceIn(0, context.framebufferHeight - 1)
			top = ceil((scissor.y + scissor.height) * scale).toInt().coerceIn(bottom + 1, context.framebufferHeight)
		}
		pass.enableScissor(left, bottom, right - left, top - bottom)
	}

	companion object {
		private const val TRANSFORM_STACK_DEPTH = 24
		private const val CORNER_SEGMENTS = 4
	}
}
