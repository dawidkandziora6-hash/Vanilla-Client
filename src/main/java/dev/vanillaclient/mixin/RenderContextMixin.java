package dev.vanillaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.net.ClientSync;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers which player is being drawn, so a shield in their hand shows their design. */
@Mixin(LivingEntityRenderer.class)
public class RenderContextMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"))
	private void vc$begin(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam, CallbackInfo ci) {
		ClientSync.RENDERING.set(state instanceof AvatarRenderState a ? a.id : -1);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("RETURN"))
	private void vc$end(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam, CallbackInfo ci) {
		ClientSync.RENDERING.set(-1);
	}
}
