package wtf.traumadev.beryllium.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.traumadev.beryllium.event.EventBus;
import wtf.traumadev.beryllium.event.impl.FrameEvent;
import wtf.traumadev.beryllium.render.Renderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void beryllium$onRenderStart(final CallbackInfo ci) {
		EventBus.INSTANCE.post(new FrameEvent(FrameEvent.Phase.START));
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"
		)
	)
	private void beryllium$onBeforeGuiRender(final CallbackInfo ci) {
		Renderer.INSTANCE.flushHud();
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void beryllium$onRenderEnd(final CallbackInfo ci) {
		EventBus.INSTANCE.post(new FrameEvent(FrameEvent.Phase.END));
	}
}
