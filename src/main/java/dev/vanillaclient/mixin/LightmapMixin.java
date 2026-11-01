package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fullbright: lights everything like night vision, without the effect. */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {
	private static boolean vc$was;

	@Inject(method = "extract", at = @At("TAIL"))
	private void vc$fullbright(LightmapRenderState state, float partial, CallbackInfo ci) {
		Config.Pvp p = Config.get().pvp;
		if (p.fullbright) {
			if (state.nightVisionEffectIntensity < p.fullbrightStrength) {
				state.nightVisionEffectIntensity = p.fullbrightStrength;
				state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
			}
			state.needsUpdate = true;
		} else if (vc$was) {
			state.needsUpdate = true;
		}
		vc$was = p.fullbright;
	}
}
