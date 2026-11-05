package wtf.traumadev.beryllium.render.buffer

import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.GpuFence
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.Mth
import wtf.traumadev.beryllium.util.Logger

internal class GpuBufferPool(private val label: String, private val usage: Int, private val increment: Int = 262144) : AutoCloseable {
	private val available = ArrayDeque<GpuBuffer>()
	private val usedThisFrame = ArrayList<GpuBuffer>()
	private val pendingRecycle = ArrayList<PendingRecycle>()

	fun acquire(minSize: Int): GpuBuffer {
		recyclePending()
		val rounded = Mth.roundToward(minSize, increment)
		var best: GpuBuffer? = null
		for (candidate in available) {
			if (candidate.size() >= rounded && (best == null || candidate.size() < best.size())) best = candidate
		}
		val buffer: GpuBuffer
		if (best != null && best.size() <= rounded * REUSE_SIZE_FACTOR) {
			available.remove(best)
			buffer = best
		} else {
			if (best != null) {
				available.remove(best)
				best.close()
			}
			buffer = RenderSystem.getDevice().createBuffer({ "$label ${rounded / 1024}KiB" }, usage, rounded.toLong())
		}
		usedThisFrame.add(buffer)
		return buffer
	}

	fun endFrame() {
		if (usedThisFrame.isEmpty()) return
		val device = RenderSystem.tryGetDevice() ?: return
		pendingRecycle.add(PendingRecycle(usedThisFrame.toList(), device.createCommandEncoder().createFence()))
		usedThisFrame.clear()
	}

	private fun recyclePending() {
		pendingRecycle.removeIf { pending ->
			val recycled = pending.tryRecycle()
			if (recycled != null) {
				available.addAll(recycled)
				true
			} else {
				false
			}
		}
	}

	override fun close() {
		available.forEach(GpuBuffer::close)
		usedThisFrame.forEach(GpuBuffer::close)
		pendingRecycle.forEach(PendingRecycle::close)
		available.clear()
		usedThisFrame.clear()
		pendingRecycle.clear()
	}

	private class PendingRecycle(private val buffers: List<GpuBuffer>, private val fence: GpuFence) : AutoCloseable {
		fun tryRecycle(): List<GpuBuffer>? {
			return if (fence.awaitCompletion(0L)) {
				fence.close()
				buffers
			} else {
				null
			}
		}

		override fun close() {
			fence.close()
			buffers.forEach(GpuBuffer::close)
		}
	}

	companion object {
		private const val REUSE_SIZE_FACTOR = 4
	}
}
