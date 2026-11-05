package wtf.traumadev.beryllium.event.impl

import wtf.traumadev.beryllium.event.CancellableEvent

class KeyPressEvent(val key: Int, val action: Int, val mods: Int) : CancellableEvent()
