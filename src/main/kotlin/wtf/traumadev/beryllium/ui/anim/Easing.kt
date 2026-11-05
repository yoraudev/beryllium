package wtf.traumadev.beryllium.ui.anim

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

fun interface Easing {
	fun transform(t: Float): Float
}

object Easings {
	val linear = Easing { t -> t }

	val easeInQuad = Easing { t -> t * t }

	val easeOutQuad = Easing { t -> 1f - (1f - t) * (1f - t) }

	val easeInOutQuad = Easing { t ->
		if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2f) / 2f
	}

	val easeInCubic = Easing { t -> t * t * t }

	val easeOutCubic = Easing { t -> 1f - (1f - t).pow(3f) }

	val easeInOutCubic = Easing { t ->
		if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3f) / 2f
	}

	val easeOutBack = Easing { t ->
		val overshoot = 1.70158f
		val c3 = overshoot + 1f
		1f + c3 * (t - 1f).pow(3f) + overshoot * (t - 1f).pow(2f)
	}

	val easeOutElastic = Easing { t ->
		val c4 = (2f * PI / 3f).toFloat()
		when {
			t <= 0f -> 0f
			t >= 1f -> 1f
			else -> 2f.pow(-10f * t) * sin((t * 10f - 0.75f) * c4) + 1f
		}
	}

	val easeOutBounce = Easing { t ->
		val n1 = 7.5625f
		val d1 = 2.75f
		when {
			t < 1f / d1 -> n1 * t * t
			t < 2f / d1 -> n1 * (t - 1.5f / d1) * (t - 1.5f / d1) + 0.75f
			t < 2.5f / d1 -> n1 * (t - 2.25f / d1) * (t - 2.25f / d1) + 0.9375f
			else -> n1 * (t - 2.625f / d1) * (t - 2.625f / d1) + 0.984375f
		}
	}
}
