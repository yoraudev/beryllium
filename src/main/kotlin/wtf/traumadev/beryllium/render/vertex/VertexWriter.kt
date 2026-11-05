package wtf.traumadev.beryllium.render.vertex

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class VertexWriter(initialCapacity: Int = 2048) {
	var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity).order(ByteOrder.nativeOrder())
		private set
	var vertexCount: Int = 0
		private set

	val sizeBytes: Int
		get() = buffer.position()

	fun reset() {
		buffer.clear()
		vertexCount = 0
	}

	fun ensure(bytes: Int) {
		if (buffer.remaining() >= bytes) return
		var capacity = buffer.capacity()
		while (capacity - buffer.position() < bytes) capacity *= 2
		val grown = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
		buffer.flip()
		grown.put(buffer)
		buffer = grown
	}

	fun putFloat(value: Float) {
		ensure(4)
		buffer.putFloat(value)
	}

	fun putFloats(a: Float, b: Float) {
		ensure(8)
		buffer.putFloat(a)
		buffer.putFloat(b)
	}

	fun putFloats(a: Float, b: Float, c: Float) {
		ensure(12)
		buffer.putFloat(a)
		buffer.putFloat(b)
		buffer.putFloat(c)
	}

	fun putFloats(a: Float, b: Float, c: Float, d: Float) {
		ensure(16)
		buffer.putFloat(a)
		buffer.putFloat(b)
		buffer.putFloat(c)
		buffer.putFloat(d)
	}

	fun putByte(value: Int) {
		ensure(1)
		buffer.put(value.toByte())
	}

	fun putVertices(source: ByteBuffer, count: Int) {
		val duplicate = source.duplicate()
		ensure(duplicate.remaining())
		buffer.put(duplicate)
		vertexCount += count
	}

	fun putColor(argb: Int) {
		ensure(4)
		buffer.put(((argb shr 16) and 0xFF).toByte())
		buffer.put(((argb shr 8) and 0xFF).toByte())
		buffer.put(((argb and 0xFF)).toByte())
		buffer.put(((argb ushr 24) and 0xFF).toByte())
	}

	fun endVertex() {
		vertexCount++
	}

	fun view(): ByteBuffer {
		val duplicate = buffer.duplicate()
		duplicate.flip()
		return duplicate
	}
}
