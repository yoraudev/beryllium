package wtf.traumadev.beryllium.event.impl

import wtf.traumadev.beryllium.event.CancellableEvent

class MouseClickEvent(val x: Float, val y: Float, val button: Int, val action: Int, val mods: Int) : CancellableEvent()
