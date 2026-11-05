package wtf.traumadev.beryllium.render.pipeline

import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.device.GpuDevice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4fc
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class TransformUbo : AutoCloseable {
	private var buffer: GpuBuffer? = null
	private val staging: ByteBuffer = ByteBuffer.allocateDirect(SIZE_BYTES.toInt()).order(ByteOrder.nativeOrder())

	val slice: GpuBufferSlice
		get() = requireNotNull(buffer).slice()

	fun update(encoder: CommandEncoder, projection: Matrix4fc, modelView: Matrix4fc, screenWidth: Float, screenHeight: Float) {
		val target = buffer ?: createBuffer().also { buffer = it }
		staging.clear()
		Std140Builder.intoBuffer(staging)
			.putMat4f(projection)
			.putMat4f(modelView)
			.putVec2(screenWidth, screenHeight)
		staging.position(0)
		staging.limit(SIZE_BYTES.toInt())
		encoder.writeToBuffer(target.slice(0, SIZE_BYTES), staging)
	}

	fun bind(pass: RenderPass) {
		pass.setUniform(UNIFORM_NAME, slice)
	}

	private fun createBuffer(): GpuBuffer =
		RenderSystem.getDevice().createBuffer(
			{ "Beryllium transforms UBO" },
			GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,
			SIZE_BYTES
		)

	override fun close() {
		buffer?.close()
		buffer = null
	}

	companion object {
		const val UNIFORM_NAME = "Transforms"
		val SIZE_BYTES: Long = 144
	}
}
