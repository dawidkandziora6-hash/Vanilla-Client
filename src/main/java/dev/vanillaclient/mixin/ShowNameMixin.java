package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Your own name tag in third person (F5) – vanilla hides it. Only on your screen. */
@Mixin(LivingEntityRenderer.class)
public class ShowNameMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
	private void vc$ownName(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = Minecraft.getInstance();
		Config.Ranks r = Config.get().ranks;
		if (entity == mc.player && r.ownNameTag && !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui
				&& !entity.isInvisible()) {
			cir.setReturnValue(true);
		}
	}
}
