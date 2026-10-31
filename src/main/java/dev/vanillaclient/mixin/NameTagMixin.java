package dev.vanillaclient.mixin;

import dev.vanillaclient.features.Ranks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rank symbol in front of your own name tag. */
@Mixin(EntityRenderer.class)
public class NameTagMixin {
	@Inject(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
	private void vc$rank(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (entity != Minecraft.getInstance().player || cir.getReturnValue() == null) return;
		cir.setReturnValue(Ranks.decorate(cir.getReturnValue()));
	}
}
