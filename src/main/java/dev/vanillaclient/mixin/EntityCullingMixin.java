package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Performance: entities far away are not drawn at all. */
@Mixin(EntityRenderDispatcher.class)
public class EntityCullingMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void vc$cull(E entity, Frustum frustum, double x, double y, double z, float partial, CallbackInfoReturnable<Boolean> cir) {
		Config.Performance p = Config.get().performance;
		if (!p.cullEntities) return;
		if (entity instanceof Player && p.alwaysPlayers) return;
		int d = entity instanceof ItemEntity ? p.itemDistance
				: entity instanceof net.minecraft.world.entity.decoration.ArmorStand || entity instanceof net.minecraft.world.entity.decoration.HangingEntity ? p.decorDistance
				: p.entityDistance;
		if (entity.distanceToSqr(x, y, z) > (double) d * d) cir.setReturnValue(false);
	}
}
