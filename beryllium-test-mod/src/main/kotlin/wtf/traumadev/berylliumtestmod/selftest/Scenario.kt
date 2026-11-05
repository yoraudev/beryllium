package wtf.traumadev.berylliumtestmod.selftest

import wtf.traumadev.beryllium.event.impl.WorldRenderEvent
import wtf.traumadev.beryllium.render.draw.Renderer2D

open class Scenario(
	val name: String,
	val warmupTicks: Int = 5,
	val setup: () -> Unit = {},
	val teardown: () -> Unit = {},
	val drawGui: (Renderer2D.() -> Unit)? = null,
	val drawWorld: ((WorldRenderEvent) -> Unit)? = null,
	val verify: Capture.() -> List<String> = { emptyList() }
) {
	val stepFailures = ArrayList<String>()
	private var ticks = 0

	open fun onTick(): Boolean {
		ticks++
		return ticks >= warmupTicks
	}
}
