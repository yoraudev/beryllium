package wtf.traumadev.beryllium.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.traumadev.beryllium.event.EventBus;
import wtf.traumadev.beryllium.event.impl.TickEvent;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void beryllium$onTickStart(final CallbackInfo ci) {
		EventBus.INSTANCE.post(new TickEvent(TickEvent.Phase.START));
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void beryllium$onTickEnd(final CallbackInfo ci) {
		EventBus.INSTANCE.post(new TickEvent(TickEvent.Phase.END));
	}
}
