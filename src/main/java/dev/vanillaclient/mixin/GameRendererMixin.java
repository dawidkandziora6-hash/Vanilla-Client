package dev.vanillaclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
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

	@WrapWithCondition(
			method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
	private boolean vc$hideHandWhileZooming(FirstPersonHandsAndItemsRenderer renderer, float partialTick, PoseStack pose,
			SubmitNodeCollector collector, PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands) {
		return !(Config.get().zoom.hideHand && Zoom.currentFactor() > 1.05f);
	}
}
