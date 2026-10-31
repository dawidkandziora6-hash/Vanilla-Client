package dev.vanillaclient.mixin;

import dev.vanillaclient.cosmetics.Cosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cosmetics only for your own player and only on your screen: cape and elytra
 * textures are swapped. Game logic stays vanilla: the cape disappears when you
 * wear an elytra, and wings only show when an elytra is really equipped.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void vc$cosmetics(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (self != Minecraft.getInstance().player) {
			return;
		}
		PlayerSkin skin = cir.getReturnValue();
		if (skin == null) {
			return;
		}
		ClientAsset.Texture cape = Cosmetics.capeTexture();
		ClientAsset.Texture wings = Cosmetics.wingsTexture();
		if (cape == null && wings == null) {
			return;
		}
		ClientAsset.Texture newCape = cape != null ? cape : skin.cape();
		// Without a wings design, vanilla also uses the cape texture for the elytra.
		ClientAsset.Texture newElytra = wings != null ? wings : (cape != null ? null : skin.elytra());
		cir.setReturnValue(PlayerSkin.insecure(skin.body(), newCape, newElytra, skin.model()));
	}
}
