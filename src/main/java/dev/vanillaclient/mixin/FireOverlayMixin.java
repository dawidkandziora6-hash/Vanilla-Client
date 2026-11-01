package dev.vanillaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.config.Config;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low fire: moves the burning overlay down so you can still see. */
@Mixin(ScreenEffectRenderer.class)
public class FireOverlayMixin {
	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void vc$lowFireStart(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		pose.pushPose();
		pose.translate(0f, -Config.get().pvp.lowFire, 0f);
	}

	@Inject(method = "submitFire", at = @At("TAIL"))
	private static void vc$lowFireEnd(PoseStack pose, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
		pose.popPose();
	}
}
