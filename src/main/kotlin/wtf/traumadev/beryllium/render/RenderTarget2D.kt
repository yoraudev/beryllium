package wtf.traumadev.beryllium.render

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.renderpearl.api.GpuFormat
import wtf.traumadev.beryllium.render.draw.Renderer2D
import wtf.traumadev.beryllium.render.pipeline.TransformUbo
import wtf.traumadev.beryllium.render.texture.Texture2D

class RenderTarget2D private constructor(
	val label: String,
	width: Int,
	height: Int,
	internal val target: TextureTarget,
	internal val transforms: TransformUbo,
	private val renderer: Renderer2D
) : AutoCloseable {
	var width: Int = width
		private set
	var height: Int = height
		private set

	var color: Texture2D = wrapTexture()
		private set

	fun draw(clearColor: Int? = Colors.TRANSPARENT, block: Renderer2D.() -> Unit) {
		renderer.guiScale = 1f
		renderer.guiWidth = width.toFloat()
		renderer.guiHeight = height.toFloat()
		renderer.block()
		Renderer.renderTo(this, renderer, clearColor)
	}

	fun resize(width: Int, height: Int) {
		require(width > 0 && height > 0) { "Render target size must be positive" }
		target.resize(width, height)
		this.width = width
		this.height = height
		color = wrapTexture()
	}

	override fun close() {
		renderer.discard()
		transforms.close()
		target.destroyBuffers()
	}

	private fun wrapTexture(): Texture2D = Texture2D.wrap(
		requireNotNull(target.getColorTexture()) { "Render target has no color texture" },
		requireNotNull(target.getColorTextureView()) { "Render target has no color view" },
		width,
		height
	)

	companion object {
		fun create(label: String, width: Int, height: Int): RenderTarget2D {
			require(width > 0 && height > 0) { "Render target size must be positive" }
			val target = TextureTarget(label, width, height, GpuFormat.RGBA8_UNORM, null)
			return RenderTarget2D(label, width, height, target, TransformUbo(), Renderer2D())
		}
	}
}