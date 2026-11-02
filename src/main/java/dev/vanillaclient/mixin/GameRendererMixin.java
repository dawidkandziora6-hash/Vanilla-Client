package dev.vanillaclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.config.Config;
import dev.vanillaclient.zoom.Zoom;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Zoom: only scales the projection matrix (pure display). Your FOV setting,
 * reach and game mechanics stay untouched.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@ModifyVariable(method = "renderLevel", at = @At("STORE"), ordinal = 0)
	private Matrix4f vc$zoomProjection(Matrix4f projection) {
		float factor = Zoom.frameFactor();
		if (factor > 1.0001f) {
			projection.scale(factor, factor, 1.0f);
		}
		return projection;
	}

	@WrapOperation(
			method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
	private void vc$hideHandWhileZooming(FirstPersonHandsAndItemsRenderer renderer, float partialTick, PoseStack pose,
			SubmitNodeCollector collector, PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, Operation<Void> original) {
		float k = Config.get().zoom.hideHand ? Zoom.handHidden() : 0f;
		if (k >= 0.999f) return; // fully zoomed: no hand
		if (k <= 0.001f) {
			original.call(renderer, partialTick, pose, collector, player, hands);
			return;
		}
		// while zooming in/out the hand slides smoothly down / back up instead of popping
		pose.pushPose();
		pose.translate(0f, -k * k * 0.9f, 0f);
		original.call(renderer, partialTick, pose, collector, player, hands);
		pose.popPose();
	}
}
