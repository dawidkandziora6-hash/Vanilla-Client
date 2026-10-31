package dev.vanillaclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.cosmetics.Cosmetics;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shield skin: plain shields are drawn with the chosen design instead of the
 * vanilla wood. Shields with a banner on them stay vanilla (the banner wins).
 * Drawn with cutout so shaped shields (round, kite, heater…) keep their outline.
 * Client-side only, no resource pack needed.
 */
@Mixin(ShieldSpecialRenderer.class)
public class ShieldSpecialRendererMixin {
	@Shadow
	@Final
	private ShieldModel model;

	@Unique
	private static dev.vanillaclient.cosmetics.RoundShieldModel vc$round;

	@Unique
	private static final UvMapping VC_IDENTITY = new UvMapping() {
		@Override
		public float getU(float u) {
			return u;
		}

		@Override
		public float getV(float v) {
			return v;
		}
	};

	@Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
			at = @At("HEAD"), cancellable = true)
	private void vc$shieldSkin(DataComponentMap data, PoseStack pose, SubmitNodeCollector collector,
			int light, int overlay, boolean foil, int outline, CallbackInfo ci) {
		Identifier tex;
		net.minecraft.world.entity.player.Player holder = dev.vanillaclient.net.ClientSync.renderingPlayer();
		if (holder != null && holder != net.minecraft.client.Minecraft.getInstance().player) {
			dev.vanillaclient.net.ClientSync.Remote r = dev.vanillaclient.net.ClientSync.of(holder);
			Cosmetics.Tex t = r == null || !dev.vanillaclient.config.Config.get().ranks.showOthers ? null : Cosmetics.texture(Cosmetics.Kind.SHIELD, r.shield());
			tex = t == null ? null : t.id();
		} else {
			tex = Cosmetics.shieldTexture();
		}
		if (tex == null) return;
		if (data != null) {
			BannerPatternLayers patterns = data.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
			if (!patterns.layers().isEmpty() || data.get(DataComponents.BASE_COLOR) != null) return;
		}
		boolean custom = tex.getPath().contains("_custom");
		if (vc$round == null) vc$round = new dev.vanillaclient.cosmetics.RoundShieldModel();
		collector.submitModel(custom ? this.model : vc$round, Unit.INSTANCE, pose,
				foil ? RenderTypes.armorCutoutNoCullGlint(tex) : RenderTypes.entityCutout(tex),
				light, overlay, -1, VC_IDENTITY, outline);
		ci.cancel();
	}
}
