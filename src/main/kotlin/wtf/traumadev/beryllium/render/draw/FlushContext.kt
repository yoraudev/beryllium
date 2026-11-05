package wtf.traumadev.beryllium.render.draw

import com.mojang.renderpearl.api.commands.CommandEncoder
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import wtf.traumadev.beryllium.render.buffer.GpuBufferPool
import wtf.traumadev.beryllium.render.pipeline.TransformUbo

internal class FlushContext(
	val encoder: CommandEncoder,
	val bufferPool: GpuBufferPool,
	val uniformPool: GpuBufferPool,
	val transforms: TransformUbo,
	val target: RenderTarget,
	val guiWidth: Float,
	val guiHeight: Float,
	val guiScale: Float,
	val framebufferWidth: Int,
	val framebufferHeight: Int,
	val pipelineReady: (RenderPipeline) -> Boolean,
	val flipY: Boolean = true
)
