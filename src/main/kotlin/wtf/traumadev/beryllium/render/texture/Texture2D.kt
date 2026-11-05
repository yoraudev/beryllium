package wtf.traumadev.beryllium.render.texture

import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.AddressMode
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuSampler
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalDouble

class Texture2D private constructor(val texture: GpuTexture, val view: GpuTextureView, val width: Int, val height: Int) : AutoCloseable, ImageSource {
	override val image: Texture2D
		get() = this

	fun upload(source: ByteBuffer) {
		upload(0, 0, width, height, source, 0, width * 4)
	}

	fun upload(x: Int, y: Int, width: Int, height: Int, source: ByteBuffer, offset: Int = 0, stride: Int = width * 4) {
		RenderSystem.assertOnRenderThread()
		val device = RenderSystem.tryGetDevice() ?: return
		UPLOADER.upload(device.createCommandEncoder(), this, x, y, width, height, source, offset, stride)
	}

	override fun close() {
		view.close()
		texture.close()
	}

	companion object {
		private val UPLOADER: TextureUploader by lazy { TextureUploader() }

		internal fun wrap(texture: GpuTexture, view: GpuTextureView, width: Int, height: Int): Texture2D =
			Texture2D(texture, view, width, height)

		fun create(label: String, width: Int, height: Int): Texture2D {
			val device = RenderSystem.getDevice()
			val texture = device.createTexture(
				label,
				GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
				GpuFormat.RGBA8_UNORM,
				width,
				height,
				1,
				1
			)
			return Texture2D(texture, device.createTextureView(texture), width, height)
		}
	}
}

internal class TextureUploader(private val initialCapacity: Int = 1 shl 16) : AutoCloseable {
	private var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity).order(ByteOrder.nativeOrder())

	fun upload(encoder: CommandEncoder, texture: Texture2D, x: Int, y: Int, width: Int, height: Int, source: ByteBuffer, sourceOffset: Int, sourceStride: Int) {
		val size = width * height * 4
		if (buffer.capacity() < size) {
			var capacity = buffer.capacity()
			while (capacity < size) capacity *= 2
			buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
		}
		buffer.clear()
		buffer.limit(size)
		if (sourceStride == width * 4) {
			val duplicate = source.duplicate()
			duplicate.position(sourceOffset)
			duplicate.limit(sourceOffset + size)
			buffer.put(duplicate)
		} else {
			for (row in 0 until height) {
				source.position(sourceOffset + row * sourceStride)
				for (column in 0 until width) {
					buffer.put(source.get())
					buffer.put(source.get())
					buffer.put(source.get())
					buffer.put(source.get())
				}
			}
		}
		buffer.position(0)
		encoder.writeToTexture(texture.texture, buffer, 0, 0, x, y, width, height)
	}

	override fun close() {
		buffer = ByteBuffer.allocateDirect(0)
	}
}

internal object Samplers {
	lateinit var linear: GpuSampler
		private set
	lateinit var nearest: GpuSampler
		private set

	fun init() {
		val device = RenderSystem.getDevice()
		linear = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty())
		nearest = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty())
	}

	fun of(sampling: TextureSampling): GpuSampler = when (sampling) {
		TextureSampling.LINEAR -> linear
		TextureSampling.NEAREST -> nearest
	}
}
