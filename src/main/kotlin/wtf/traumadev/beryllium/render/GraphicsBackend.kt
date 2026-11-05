package wtf.traumadev.beryllium.render

import com.mojang.blaze3d.systems.RenderSystem

enum class GraphicsBackend(val display: String) {
	OPENGL("OpenGL"),
	VULKAN("Vulkan"),
	UNKNOWN("Unknown");

	companion object {
		internal fun detect(): GraphicsBackend {
			val device = RenderSystem.tryGetDevice() ?: return UNKNOWN
			return when (device.deviceInfo.backendName) {
				"OpenGL" -> OPENGL
				"Vulkan" -> VULKAN
				else -> UNKNOWN
			}
		}
	}
}
