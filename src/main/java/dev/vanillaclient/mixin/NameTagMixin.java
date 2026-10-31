package dev.vanillaclient.mixin;

import dev.vanillaclient.features.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client rank / play-time rank in front of player name tags (yours and other Vanilla Client players'). */
@Mixin(EntityRenderer.class)
public class NameTagMixin {
	@Inject(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
	private void vc$rank(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (!(entity instanceof Player p) || cir.getReturnValue() == null) return;
		cir.setReturnValue(NameTags.decorate(p, cir.getReturnValue()));
	}
}
