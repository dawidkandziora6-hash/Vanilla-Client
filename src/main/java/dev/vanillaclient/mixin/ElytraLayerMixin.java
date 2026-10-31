package dev.vanillaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.cosmetics.WingsLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** When cosmetic wings are shown with an elytra on, the normal elytra is hidden (the wings replace it). */
@Mixin(net.minecraft.client.renderer.entity.layers.WingsLayer.class)
public class ElytraLayerMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
			at = @At("HEAD"), cancellable = true)
	private void vc$hideElytra(PoseStack pose, SubmitNodeCollector c, int light, HumanoidRenderState s, float y, float x, CallbackInfo ci) {
		if (s instanceof AvatarRenderState a && a.chestEquipment.is(net.minecraft.world.item.Items.ELYTRA) && WingsLayer.showsWings(a)) ci.cancel();
	}
}
