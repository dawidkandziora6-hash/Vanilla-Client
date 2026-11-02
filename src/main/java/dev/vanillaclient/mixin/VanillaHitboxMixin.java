package dev.vanillaclient.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** F3+B hitboxes: when our style is on, ours replace Minecraft's (same switch, one set of boxes). */
@Mixin(EntityHitboxDebugRenderer.class)
public class VanillaHitboxMixin {
	@Inject(method = "emitGizmos", at = @At("HEAD"), cancellable = true)
	private void vc$ownStyle(double x, double y, double z, DebugValueAccess access, Frustum frustum, float partial, CallbackInfo ci) {
		if (dev.vanillaclient.pvp.Hitboxes.ownStyle()) ci.cancel();
	}
}
