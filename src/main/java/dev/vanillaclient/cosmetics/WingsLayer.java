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
	/** Smoothed animation per player: phase, strength, fold, last time. */
	private static final long START = System.nanoTime();
	private static final java.util.Map<Integer, float[]> ANIM = new java.util.HashMap<>();
	private static long lastCleanup;

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

	/** True if wings should be drawn for this player state (also used to hide the vanilla elytra). */
	public static boolean showsWings(AvatarRenderState s) {
		if (s.isInvisibleToPlayer || textureFor(s) == null) return false;
		boolean elytra = s.chestEquipment.is(Items.ELYTRA);
		return Config.get().cosmetics.wingsOnlyWithElytra ? elytra : !elytra || Config.get().cosmetics.wingsReplaceElytra;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState s, float yRot, float xRot) {
		if (!showsWings(s)) return;
		Identifier tex = textureFor(s);
		if (tex == null) return;
		if (model == null) model = new WingsModel();
		// seconds since start as float stays precise (nanoTime/1e9 as float would step in ~8 ms chunks)
		float now = (float) ((System.nanoTime() - START) / 1e9);
		float[] a = ANIM.computeIfAbsent(s.id, k -> new float[] {k * 0.7f, 0f, 0f, 0f});
		float dt = a[3] == 0 ? 0f : Math.max(0f, Math.min(0.1f, now - a[3]));
		a[3] = now;
		// targets: gentle idle, a bit more while walking, full flaps while gliding
		float target = s.isFallFlying ? 1f : Math.min(0.35f, s.walkAnimationSpeed * 0.5f);
		a[1] += (target - a[1]) * Math.min(1f, dt * 2.5f);
		a[2] += ((s.isCrouching ? 1f : 0f) - a[2]) * Math.min(1f, dt * 6f);
		a[0] += dt * (1.4f + a[1] * 5.5f); // phase only ever moves forward: no jumps, no jitter
		if (a[0] > 1000f) a[0] -= (float) (Math.PI * 2 * 159);
		if (System.currentTimeMillis() - lastCleanup > 30_000) {
			lastCleanup = System.currentTimeMillis();
			ANIM.entrySet().removeIf(e -> now - e.getValue()[3] > 30f);
		}
		pose.pushPose();
		getParentModel().body.translateAndRotate(pose);
		collector.submitModel(model, new WingsModel.Pose(a[0], a[1], a[2]), pose, RenderTypes.entityCutout(tex),
				light, OverlayTexture.NO_OVERLAY, s.outlineColor);
		pose.popPose();
	}
}
