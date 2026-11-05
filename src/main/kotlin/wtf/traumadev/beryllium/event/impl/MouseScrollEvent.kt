package wtf.traumadev.beryllium.event.impl

import wtf.traumadev.beryllium.event.CancellableEvent

class MouseScrollEvent(val x: Float, val y: Float, val xOffset: Double, val yOffset: Double) : CancellableEvent()
