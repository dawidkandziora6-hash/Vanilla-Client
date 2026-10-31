package dev.vanillaclient.mixin;

import dev.vanillaclient.zoom.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While zooming, the scroll wheel changes the zoom level (instead of the hotbar slot). */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void vc$scrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (Zoom.onScroll(vertical)) {
			ci.cancel();
		}
	}
}
