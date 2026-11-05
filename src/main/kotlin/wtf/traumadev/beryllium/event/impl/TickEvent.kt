package wtf.traumadev.beryllium.event.impl

import wtf.traumadev.beryllium.event.Event

class TickEvent(val phase: Phase) : Event {
	enum class Phase {
		START,
		END,
	}
}
