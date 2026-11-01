package dev.vanillaclient.pvp;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws hitboxes with Minecraft's own debug lines, but in your colors and only for what you choose. */
public final class Hitboxes {
	private Hitboxes() {}

	public static boolean shows(Entity e) {
		Config.Hitboxes h = Config.get().hitboxes;
		if (e instanceof Player) return h.players;
		if (e instanceof ItemEntity || e instanceof ExperienceOrb) return h.items;
		if (e instanceof Projectile) return h.projectiles;
		if (e instanceof LivingEntity) {
			MobCategory c = e.getType().getCategory();
			return c == MobCategory.MONSTER ? h.mobs : h.animals;
		}
		return h.others;
	}

	public static void emit(float partial) {
		Config.Hitboxes h = Config.get().hitboxes;
		Minecraft mc = Minecraft.getInstance();
		if (!h.enabled || mc.level == null || mc.player == null) return;
		Entity cam = mc.getCameraEntity();
		boolean firstPerson = mc.options.getCameraType().isFirstPerson();
		double max = (double) h.distance * h.distance;
		Entity target = mc.crosshairPickEntity;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e.isInvisible()) continue;
			if (e == cam && (firstPerson || !h.self)) continue;
			if (e != cam && !shows(e)) continue;
			if (cam != null && e.distanceToSqr(cam) > max) continue;
			Vec3 off = e.getPosition(partial).subtract(e.position());
			AABB box = e.getBoundingBox().move(off);
			int col = h.color;
			if (h.hurtHighlight && e instanceof LivingEntity le && le.hurtTime > 0) col = h.hurtColor;
			else if (h.targetHighlight && e == target) col = h.targetColor;
			GizmoStyle st = h.fill ? GizmoStyle.strokeAndFill(col, h.width, h.fillColor) : GizmoStyle.stroke(col, h.width);
			Gizmos.cuboid(box, st);
			if (e instanceof LivingEntity) {
				if (h.eyeLine) {
					double ey = box.minY + e.getEyeHeight();
					Gizmos.cuboid(new AABB(box.minX, ey - 0.01, box.minZ, box.maxX, ey + 0.01, box.maxZ), GizmoStyle.stroke(0xFFFF3030, h.width));
				}
				if (h.lookLine) {
					Vec3 eye = e.getEyePosition(partial);
					Gizmos.line(eye, eye.add(e.getViewVector(partial).scale(2.0)), 0xFF4060FF, h.width);
				}
			}
		}
	}
}
