package dev.vanillaclient.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Our hitboxes are added while Minecraft collects its debug shapes. */
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
	@Inject(method = "emitGizmos", at = @At("HEAD"))
	private void vc$hitboxes(Frustum frustum, double x, double y, double z, float partial, CallbackInfo ci) {
		dev.vanillaclient.pvp.Hitboxes.emit(partial);
	}
}
