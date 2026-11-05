package wtf.traumadev.beryllium.render.draw

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mesh private constructor(
	internal val data: ByteBuffer,
	val vertexCount: Int,
	val topology: PrimitiveTopology
) {
	companion object {
		fun of(data: ByteBuffer, vertexCount: Int, topology: PrimitiveTopology = PrimitiveTopology.TRIANGLES): Mesh {
			require(vertexCount > 0) { "vertexCount must be positive" }
			require(data.remaining() > 0) { "vertex data must not be empty" }
			return Mesh(data, vertexCount, topology)
		}

		fun of(floats: FloatArray, vertexCount: Int, topology: PrimitiveTopology = PrimitiveTopology.TRIANGLES): Mesh {
			require(floats.isNotEmpty()) { "vertex data must not be empty" }
			val buffer = ByteBuffer.allocateDirect(floats.size * 4).order(ByteOrder.nativeOrder())
			buffer.asFloatBuffer().put(floats)
			buffer.position(0)
			buffer.limit(buffer.capacity())
			return of(buffer, vertexCount, topology)
		}
	}
}