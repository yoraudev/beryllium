package wtf.traumadev.beryllium.event

object EventBus {
	private val subscriptions = HashMap<Class<out Event>, MutableList<Subscription>>()

	inline fun <reified T : Event> subscribe(priority: Int = 0, noinline listener: (T) -> Unit): Subscription =
		subscribe(T::class.java, priority, listener)

	fun <T : Event> subscribe(type: Class<out T>, priority: Int = 0, listener: (T) -> Unit): Subscription =
		register(type, priority) { event ->
			@Suppress("UNCHECKED_CAST")
			listener(event as T)
		}

	fun post(event: Event): Boolean {
		val snapshot = subscriptions[event.javaClass] ?: return false
		for (subscription in snapshot.toList()) {
			if (!subscription.isActive) continue
			subscription.dispatch(event)
			if (event is Cancellable && event.isCancelled) break
		}
		return event is Cancellable && event.isCancelled
	}

	internal fun unregister(subscription: Subscription) {
		subscriptions[subscription.type]?.remove(subscription)
	}

	private fun register(type: Class<out Event>, priority: Int, action: (Event) -> Unit): Subscription {
		val subscription = Subscription(type, priority, action)
		val list = subscriptions.getOrPut(type) { mutableListOf() }
		val index = list.indexOfFirst { it.priority < priority }
		if (index == -1) list.add(subscription) else list.add(index, subscription)
		return subscription
	}
}
