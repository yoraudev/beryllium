package wtf.traumadev.beryllium.event.impl

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.level.CameraRenderState
import org.joml.Matrix4f
import wtf.traumadev.beryllium.event.Event
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class WorldRenderEvent(cameraState: CameraRenderState) : Event {
	val cameraX: Double = cameraState.pos.x
	val cameraY: Double = cameraState.pos.y
	val cameraZ: Double = cameraState.pos.z
	val viewRotation: Matrix4f = Matrix4f(cameraState.viewRotationMatrix)
	val projection: Matrix4f = Matrix4f(cameraState.projectionMatrix).mul(bobPose(cameraState))
	val cameraOrientation = cameraState.orientation

	private fun bobPose(cameraState: CameraRenderState): Matrix4f {
		val options = Minecraft.getInstance().options
		val pose = Matrix4f()
		applyBobHurt(cameraState, pose, options.damageTiltStrength().get().toFloat())
		if (options.bobView().get()) applyBobView(cameraState, pose)
		return pose
	}

	private fun applyBobHurt(cameraState: CameraRenderState, pose: Matrix4f, damageTiltStrength: Float) {
		val entity = cameraState.entityRenderState
		if (!entity.isLiving) return
		val hurt = entity.hurtTime
		if (entity.isDeadOrDying) {
			val duration = minOf(entity.deathTime, 20f)
			pose.rotateZ(radians(40f - 8000f / (duration + 200f)))
		}
		if (hurt < 0f) return
		val scaled = hurt / entity.hurtDuration
		val tilt = sin(scaled * scaled * scaled * scaled * Math.PI.toFloat())
		val dir = entity.hurtDir
		pose.rotateY(radians(-dir))
		pose.rotateZ(radians(-tilt * 14f * damageTiltStrength))
		pose.rotateY(radians(dir))
	}

	private fun applyBobView(cameraState: CameraRenderState, pose: Matrix4f) {
		val entity = cameraState.entityRenderState
		if (!entity.isPlayer) return
		val walk = entity.backwardsInterpolatedWalkDistance
		val bob = entity.bob
		pose.translate(sin(walk * Math.PI.toFloat()) * bob * 0.5f, -abs(cos(walk * Math.PI.toFloat()) * bob), 0f)
		pose.rotateZ(radians(sin(walk * Math.PI.toFloat()) * bob * 3f))
		pose.rotateX(radians(abs(cos(walk * Math.PI.toFloat() - 0.2f) * bob) * 5f))
	}

	private fun radians(degrees: Float): Float = degrees * (Math.PI.toFloat() / 180f)
}
