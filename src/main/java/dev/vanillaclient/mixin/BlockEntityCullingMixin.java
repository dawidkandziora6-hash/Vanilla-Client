package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Performance: chests, signs, banners, heads … far away are not drawn. */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityCullingMixin {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity, S extends BlockEntityRenderState> void vc$cull(E be, float partial, ModelFeatureRenderer.CrumblingOverlay overlay,
			boolean flag, CallbackInfoReturnable<S> cir) {
		Config.Performance p = Config.get().performance;
		if (!p.cullBlockEntities) return;
		var pl = Minecraft.getInstance().player;
		if (pl == null) return;
		double d = p.blockEntityDistance;
		if (be.getBlockPos().distToCenterSqr(pl.position()) > d * d) cir.setReturnValue(null);
	}
}
