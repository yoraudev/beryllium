package wtf.traumadev.beryllium.render.pipeline

import wtf.traumadev.beryllium.render.texture.Texture2D
import wtf.traumadev.beryllium.render.texture.TextureSampling
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShaderUniforms {
	internal sealed interface Entry

	internal class BufferEntry(val data: ByteBuffer) : Entry

	internal class TextureEntry(val texture: Texture2D, val sampling: TextureSampling) : Entry

	internal val entries = LinkedHashMap<String, Entry>()

	val isEmpty: Boolean
		get() = entries.isEmpty()

	fun uniform(name: String, data: ByteBuffer): ShaderUniforms {
		require(name.isNotBlank()) { "Uniform name must not be blank" }
		require(data.remaining() > 0) { "Uniform data must not be empty" }
		val copy = ByteBuffer.allocateDirect(data.remaining()).order(ByteOrder.nativeOrder())
		copy.put(data.duplicate())
		copy.flip()
		entries[name] = BufferEntry(copy)
		return this
	}

	fun uniform(name: String, values: FloatArray): ShaderUniforms {
		require(values.isNotEmpty()) { "Uniform values must not be empty" }
		val buffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder())
		buffer.asFloatBuffer().put(values)
		buffer.position(0)
		buffer.limit(buffer.capacity())
		return uniform(name, buffer)
	}

	fun texture(name: String, texture: Texture2D, sampling: TextureSampling = TextureSampling.LINEAR): ShaderUniforms {
		require(name.isNotBlank()) { "Texture name must not be blank" }
		entries[name] = TextureEntry(texture, sampling)
		return this
	}
}