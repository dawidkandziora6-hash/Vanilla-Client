package dev.vanillaclient.gui;

import dev.vanillaclient.mixin.InventoryScreenInvoker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Draws your own player in a menu, turned to any angle, with a little cape "wind". */
public final class PlayerPreview {
	public enum Gear { AS_IS, CAPE, WINGS }

	private PlayerPreview() {}

	public static void draw(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size,
			float yawDegrees, float tilt, LivingEntity entity, Gear gear) {
		EntityRenderState state = InventoryScreenInvoker.vc$extractRenderState(entity);
		if (state instanceof LivingEntityRenderState ls) {
			ls.bodyRot = 180f + yawDegrees;
			ls.yRot = 0f;
			ls.xRot = ls.pose == Pose.FALL_FLYING ? 0f : -tilt * 20f;
			ls.boundingBoxWidth /= ls.scale;
			ls.boundingBoxHeight /= ls.scale;
			ls.scale = 1f;
		}
		if (state instanceof AvatarRenderState av) {
			float t = Theme.time();
			av.showCape = true;
			av.capeLean = 10f + 6f * (float) Math.sin(t * 1.7);
			av.capeLean2 = 2f * (float) Math.sin(t * 1.1);
			av.capeFlap = 0f;
			if (gear == Gear.WINGS) av.chestEquipment = Icons.stack(Items.ELYTRA);
			else if (gear == Gear.CAPE) av.chestEquipment = ItemStack.EMPTY;
		}
		Vector3f translation = new Vector3f(0f, state.boundingBoxHeight / 2f + 0.0625f, 0f);
		Quaternionf camera = new Quaternionf().rotateX(tilt * 20f * 0.017453292f);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
		g.entity(state, size, translation, rotation, camera, x0, y0, x1, y1);
	}
}
