package wtf.traumadev.beryllium.ui.anim

import wtf.traumadev.beryllium.ui.View

fun View.fadeTo(target: Float, duration: Float = 0.15f, easing: Easing = Easings.easeOutQuad) {
	val animation = fadeAnimation ?: AnimatableFloat(alpha).also {
		fadeAnimation = it
		it.onChange = { value -> alpha = value }
	}
	animation.animateTo(target, duration, easing)
}

fun View.slideTo(targetX: Float? = null, targetY: Float? = null, duration: Float = 0.25f, easing: Easing = Easings.easeOutCubic) {
	if (targetX != null) {
		val animation = slideXAnimation ?: AnimatableFloat(x).also {
			slideXAnimation = it
			it.onChange = { value -> x = value }
		}
		animation.animateTo(targetX, duration, easing)
	}
	if (targetY != null) {
		val animation = slideYAnimation ?: AnimatableFloat(y).also {
			slideYAnimation = it
			it.onChange = { value -> y = value }
		}
		animation.animateTo(targetY, duration, easing)
	}
}
