package dev.vanillaclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vanillaclient.config.Config;
import dev.vanillaclient.net.ClientSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Draws cosmetic wings on players (you, and other Vanilla Client players). Hidden while wearing an elytra. */
public final class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static WingsModel model;

	public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	private static Identifier textureFor(AvatarRenderState s) {
		Minecraft mc = Minecraft.getInstance();
		Entity e = mc.level == null ? null : mc.level.getEntity(s.id);
		boolean self = mc.player != null && (e == mc.player || s.id == mc.player.getId());
		String id;
		if (self || e == null) {
			// your own player (also the wardrobe preview)
			id = Cosmetics.effectiveId(Cosmetics.Kind.WINGS);
		} else if (e instanceof Player p && Config.get().ranks.showOthers) {
			ClientSync.Remote r = ClientSync.of(p);
			id = r == null ? null : r.wings();
		} else {
			id = null;
		}
		Cosmetics.Tex t = Cosmetics.texture(Cosmetics.Kind.WINGS, id);
		return t == null ? null : t.id();
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState s, float yRot, float xRot) {
		if (s.isInvisibleToPlayer || s.chestEquipment.is(Items.ELYTRA)) return;
		Identifier tex = textureFor(s);
		if (tex == null) return;
		if (model == null) model = new WingsModel();
		float time = (System.currentTimeMillis() % 3_600_000L) / 1000f + (s.id % 97) * 0.37f;
		float flap = s.isFallFlying ? 1f : Math.min(0.6f, s.walkAnimationSpeed * 0.8f);
		float fold = s.isCrouching ? 1f : 0f;
		pose.pushPose();
		getParentModel().body.translateAndRotate(pose);
		collector.submitModel(model, new WingsModel.Pose(time, flap, fold), pose, RenderTypes.entityCutout(tex),
				light, OverlayTexture.NO_OVERLAY, s.outlineColor);
		pose.popPose();
	}
}
