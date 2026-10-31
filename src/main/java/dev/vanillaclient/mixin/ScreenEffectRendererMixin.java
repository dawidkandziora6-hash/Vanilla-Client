package dev.vanillaclient.mixin;

import dev.vanillaclient.held.HeldItems;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Size of the big totem animation that plays when a totem saves you. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	@ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 0.8f))
	private float vc$totemPopScale(float original) {
		return original * HeldItems.totemPopScale();
	}
}
