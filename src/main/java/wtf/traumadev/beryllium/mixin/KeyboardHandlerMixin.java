package wtf.traumadev.beryllium.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.traumadev.beryllium.event.EventBus;
import wtf.traumadev.beryllium.event.impl.CharTypedEvent;
import wtf.traumadev.beryllium.event.impl.KeyPressEvent;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void beryllium$onKeyPress(final long windowHandle, final int action, final KeyEvent event, final CallbackInfo ci) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (windowHandle != minecraft.getWindow().handle() || !minecraft.isWindowActive()) {
			return;
		}
		if (EventBus.INSTANCE.post(new KeyPressEvent(event.key(), action, event.modifiers()))) {
			ci.cancel();
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void beryllium$onCharTyped(final long windowHandle, final CharacterEvent event, final CallbackInfo ci) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (windowHandle != minecraft.getWindow().handle() || !minecraft.isWindowActive()) {
			return;
		}
		if (EventBus.INSTANCE.post(new CharTypedEvent(event.codepoint()))) {
			ci.cancel();
		}
	}
}
