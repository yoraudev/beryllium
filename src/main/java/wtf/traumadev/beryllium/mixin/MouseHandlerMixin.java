package wtf.traumadev.beryllium.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.traumadev.beryllium.event.EventBus;
import wtf.traumadev.beryllium.event.impl.MouseClickEvent;
import wtf.traumadev.beryllium.event.impl.MouseMoveEvent;
import wtf.traumadev.beryllium.event.impl.MouseScrollEvent;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onMove", at = @At("HEAD"))
	private void beryllium$onMove(final long windowHandle, final double xpos, final double ypos, final double xrel, final double yrel, final CallbackInfo ci) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (windowHandle != minecraft.getWindow().handle() || !minecraft.isWindowActive()) {
			return;
		}
		EventBus.INSTANCE.post(new MouseMoveEvent(
			(float) MouseHandler.getScaledXPos(minecraft.getWindow(), xpos),
			(float) MouseHandler.getScaledYPos(minecraft.getWindow(), ypos)
		));
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void beryllium$onButton(final long windowHandle, final MouseButtonInfo buttonInfo, final int action, final CallbackInfo ci) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (windowHandle != minecraft.getWindow().handle() || !minecraft.isWindowActive()) {
			return;
		}
		final MouseHandler self = (MouseHandler) (Object) this;
		final MouseClickEvent event = new MouseClickEvent(
			(float) self.getScaledXPos(minecraft.getWindow()),
			(float) self.getScaledYPos(minecraft.getWindow()),
			buttonInfo.button(),
			action,
			buttonInfo.modifiers()
		);
		if (EventBus.INSTANCE.post(event)) {
			ci.cancel();
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void beryllium$onScroll(final long windowHandle, final double xoffset, final double yoffset, final CallbackInfo ci) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (windowHandle != minecraft.getWindow().handle() || !minecraft.isWindowActive()) {
			return;
		}
		final MouseHandler self = (MouseHandler) (Object) this;
		final MouseScrollEvent event = new MouseScrollEvent(
			(float) self.getScaledXPos(minecraft.getWindow()),
			(float) self.getScaledYPos(minecraft.getWindow()),
			xoffset,
			yoffset
		);
		if (EventBus.INSTANCE.post(event)) {
			ci.cancel();
		}
	}
}
