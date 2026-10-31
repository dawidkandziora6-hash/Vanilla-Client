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
		PlayerSkin skin = cir.getReturnValue();
		if (skin == null) {
			return;
		}
		ClientAsset.Texture cape, wings;
		if (self == Minecraft.getInstance().player) {
			cape = Cosmetics.capeTexture();
			wings = null; // wings are their own 3D model now (WingsLayer)
		} else {
			// other Vanilla Client players (server sync or ranks.json)
			dev.vanillaclient.net.ClientSync.Remote r = dev.vanillaclient.net.ClientSync.of(self);
			if (r == null || !dev.vanillaclient.config.Config.get().ranks.showOthers) return;
			cape = Cosmetics.assetOf(Cosmetics.Kind.CAPE, r.cape());
			wings = null;
		}
		if (cape == null && wings == null) {
			return;
		}
		ClientAsset.Texture newCape = cape != null ? cape : skin.cape();
		// Vanilla would draw the elytra with the cape texture – our capes only fill the cape
		// area, so with "Vanilla" wings the normal elytra texture is used instead.
		ClientAsset.Texture newElytra = wings != null ? wings : (cape != null ? Cosmetics.VANILLA_ELYTRA : skin.elytra());
		cir.setReturnValue(PlayerSkin.insecure(skin.body(), newCape, newElytra, skin.model()));
	}
}
