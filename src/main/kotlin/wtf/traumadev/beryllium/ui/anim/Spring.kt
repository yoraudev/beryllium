package wtf.traumadev.beryllium.ui.anim

import kotlin.math.abs

class SpringFloat(initial: Float, stiffness: Float = DEFAULT_STIFFNESS, damping: Float = DEFAULT_DAMPING) : Ticked {
	var stiffness: Float = stiffness
	var damping: Float = damping

	var value: Float = initial
		private set
	var target: Float = initial
		private set
	var velocity: Float = 0f
		private set

	var onChange: ((Float) -> Unit)? = null

	val isSettled: Boolean
		get() = abs(target - value) < SETTLE_EPSILON && abs(velocity) < SETTLE_EPSILON

	fun setTarget(newTarget: Float) {
		target = newTarget
		Animator.play(this)
	}

	fun snapTo(newTarget: Float) {
		target = newTarget
		value = newTarget
		velocity = 0f
		onChange?.invoke(value)
	}

	override fun update(deltaTime: Float) {
		val acceleration = stiffness * (target - value) - damping * velocity
		velocity += acceleration * deltaTime
		value += velocity * deltaTime
		if (isSettled) {
			value = target
			velocity = 0f
			onChange?.invoke(value)
			Animator.cancel(this)
			return
		}
		onChange?.invoke(value)
	}

	companion object {
		const val DEFAULT_STIFFNESS = 170f
		const val DEFAULT_DAMPING = 22f
		private const val SETTLE_EPSILON = 0.01f
	}
}
