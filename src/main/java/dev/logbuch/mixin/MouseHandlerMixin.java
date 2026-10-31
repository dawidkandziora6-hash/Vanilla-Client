package dev.logbuch.mixin;

import dev.logbuch.zoom.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mausrad passt beim Zoomen die Zoomstufe an (statt den Hotbar-Slot zu wechseln). */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void logbuch$scrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (Zoom.onScroll(vertical)) {
			ci.cancel();
		}
	}
}
