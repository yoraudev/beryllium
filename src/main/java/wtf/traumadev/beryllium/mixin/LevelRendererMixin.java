package wtf.traumadev.beryllium.mixin;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.traumadev.beryllium.event.EventBus;
import wtf.traumadev.beryllium.event.impl.WorldRenderEvent;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void beryllium$onRenderTail(
		final GraphicsResourceAllocator resourceAllocator,
		final boolean renderOutline,
		final CameraRenderState cameraState,
		final GpuBufferSlice terrainFog,
		final Vector4f fogColor,
		final boolean shouldRenderSky,
		final boolean consistentDepthRequired,
		final CallbackInfo ci
	) {
		EventBus.INSTANCE.post(new WorldRenderEvent(cameraState));
	}
}
