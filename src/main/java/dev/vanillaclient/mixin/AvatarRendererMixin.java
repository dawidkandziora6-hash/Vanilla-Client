package dev.vanillaclient.mixin;

import dev.vanillaclient.cosmetics.WingsLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the cosmetic wings layer to every player renderer. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>", at = @At("TAIL"))
	private void vc$addWings(CallbackInfo ci) {
		((LivingEntityRendererInvoker) (Object) this).vc$addLayer(new WingsLayer((RenderLayerParent) (Object) this));
	}
}
