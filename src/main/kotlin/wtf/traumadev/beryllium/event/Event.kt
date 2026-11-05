package wtf.traumadev.beryllium.event

interface Event

interface Cancellable : Event {
	var isCancelled: Boolean
}

abstract class CancellableEvent : Cancellable {
	override var isCancelled: Boolean = false
}
