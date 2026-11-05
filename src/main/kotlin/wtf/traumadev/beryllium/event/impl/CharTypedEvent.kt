package wtf.traumadev.beryllium.event.impl

import wtf.traumadev.beryllium.event.CancellableEvent

class CharTypedEvent(val codepoint: Int) : CancellableEvent()
