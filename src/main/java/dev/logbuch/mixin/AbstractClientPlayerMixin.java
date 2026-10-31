package dev.logbuch.mixin;

import dev.logbuch.cosmetics.Cosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kosmetik nur für den eigenen Spieler und nur auf deinem Bildschirm:
 * Umhang- und Elytra-Textur werden ersetzt. Die Spiel-Logik bleibt vanilla:
 * der Umhang verschwindet, sobald du eine Elytra trägst, und die Flügel
 * erscheinen nur, wenn wirklich eine Elytra angelegt ist.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void logbuch$cosmetics(CallbackInfoReturnable<PlayerSkin> cir) {
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
		// Ohne eigenes Flügel-Design nutzt Vanilla die Umhang-Textur auch für die Elytra.
		ClientAsset.Texture newElytra = wings != null ? wings : (cape != null ? null : skin.elytra());
		cir.setReturnValue(PlayerSkin.insecure(skin.body(), newCape, newElytra, skin.model()));
	}
}
