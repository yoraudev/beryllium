package wtf.traumadev.beryllium.ui.anim

import wtf.traumadev.beryllium.event.EventBus
import wtf.traumadev.beryllium.event.impl.FrameEvent

internal interface Ticked {
	fun update(deltaTime: Float)
}

object Animator {
	private val items = ArrayList<Ticked>()
	private var lastFrameNanos = 0L

	init {
		EventBus.subscribe<FrameEvent>(priority = 250) { event ->
			if (event.phase == FrameEvent.Phase.START) tick()
		}
	}

	internal fun play(item: Ticked) {
		if (item !in items) items.add(item)
	}

	internal fun cancel(item: Ticked) {
		items.remove(item)
	}

	private fun tick() {
		val now = System.nanoTime()
		val deltaTime = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
		lastFrameNanos = now
		if (items.isEmpty()) return
		for (item in items.toList()) item.update(deltaTime)
	}
}
