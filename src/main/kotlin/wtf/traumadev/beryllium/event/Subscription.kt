package wtf.traumadev.beryllium.event

class Subscription internal constructor(
	val type: Class<out Event>,
	val priority: Int,
	internal val action: (Event) -> Unit,
) {
	var isActive: Boolean = true
		private set

	fun unsubscribe() {
		isActive = false
		EventBus.unregister(this)
	}

	internal fun dispatch(event: Event) {
		action(event)
	}
}
