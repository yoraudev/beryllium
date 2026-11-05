package wtf.traumadev.beryllium.ui.anim

import wtf.traumadev.beryllium.render.Colors

enum class RepeatMode { NONE, LOOP, PING_PONG }

abstract class Animatable<T : Any>(initial: T) : Ticked {
	var value: T = initial
		private set

	var onChange: ((T) -> Unit)? = null

	private var from: T = initial
	private var target: T = initial
	private var elapsed: Float = 0f
	private var duration: Float = 0f
	private var delay: Float = 0f
	private var easing: Easing = Easings.linear
	private var repeatMode: RepeatMode = RepeatMode.NONE
	private var endCallback: (() -> Unit)? = null
	private var playing: Boolean = false

	val isPlaying: Boolean
		get() = playing

	fun animateTo(
		target: T,
		duration: Float,
		easing: Easing = Easings.easeOutCubic,
		delay: Float = 0f,
		repeatMode: RepeatMode = RepeatMode.NONE,
		onEnd: (() -> Unit)? = null
	) {
		from = value
		this.target = target
		this.duration = duration.coerceAtLeast(0f)
		this.delay = delay.coerceAtLeast(0f)
		this.easing = easing
		this.repeatMode = repeatMode
		endCallback = onEnd
		elapsed = 0f
		if (this.duration <= 0f && this.delay <= 0f) {
			playing = false
			setValue(target)
			endCallback?.invoke()
			return
		}
		playing = true
		Animator.play(this)
	}

	fun snapTo(target: T) {
		stop()
		setValue(target)
	}

	fun stop() {
		playing = false
		Animator.cancel(this)
	}

	protected abstract fun interpolate(from: T, to: T, t: Float): T

	private fun setValue(newValue: T) {
		value = newValue
		onChange?.invoke(newValue)
	}

	override fun update(deltaTime: Float) {
		if (!playing) return
		if (delay > 0f) {
			val remaining = delay - deltaTime
			if (remaining > 0f) {
				delay = remaining
				return
			}
			delay = 0f
		}
		elapsed += deltaTime
		if (duration <= 0f) {
			finish()
			return
		}
		var progress = elapsed / duration
		var done = false
		when (repeatMode) {
			RepeatMode.NONE -> {
				if (progress >= 1f) {
					progress = 1f
					done = true
				}
			}
			RepeatMode.LOOP -> progress %= 1f
			RepeatMode.PING_PONG -> {
				val cycle = progress % 2f
				progress = if (cycle > 1f) 2f - cycle else cycle
			}
		}
		setValue(interpolate(from, target, easing.transform(progress)))
		if (done) finish()
	}

	private fun finish() {
		playing = false
		Animator.cancel(this)
		if (repeatMode == RepeatMode.NONE) setValue(target)
		endCallback?.invoke()
	}
}

class AnimatableFloat(initial: Float) : Animatable<Float>(initial) {
	override fun interpolate(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}

class AnimatableColor(initial: Int) : Animatable<Int>(initial) {
	override fun interpolate(from: Int, to: Int, t: Float): Int = Colors.lerp(from, to, t)
}
