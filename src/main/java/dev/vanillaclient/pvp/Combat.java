package dev.vanillaclient.pvp;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Remembers your hits: reach of the last hit, combo and the current target. Only reads, never changes anything. */
public final class Combat {
	private Combat() {}

	public static double lastReach = -1;
	public static long lastHitAt;
	public static int combo;
	private static int comboTarget = -1;
	private static int lastHurtTime;
	/** Entity id of what you hit last, and when. */
	public static int targetId = -1;
	public static long targetAt;

	public static void init() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) onHit(entity);
			return InteractionResult.PASS;
		});
	}

	private static void onHit(Entity target) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		long now = System.currentTimeMillis();
		Vec3 eye = mc.player.getEyePosition();
		AABB box = target.getBoundingBox();
		double dx = Math.max(box.minX - eye.x, Math.max(0, eye.x - box.maxX));
		double dy = Math.max(box.minY - eye.y, Math.max(0, eye.y - box.maxY));
		double dz = Math.max(box.minZ - eye.z, Math.max(0, eye.z - box.maxZ));
		lastReach = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (target.getId() == comboTarget && now - lastHitAt < 2500) combo++;
		else combo = 1;
		comboTarget = target.getId();
		lastHitAt = now;
		if (target instanceof LivingEntity) {
			targetId = target.getId();
			targetAt = now;
		}
	}

	/** Every client tick: getting hit or waiting too long ends the combo. */
	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			combo = 0;
			return;
		}
		int ht = mc.player.hurtTime;
		if (ht > lastHurtTime) combo = 0;
		lastHurtTime = ht;
		if (System.currentTimeMillis() - lastHitAt > 3000) combo = 0;
	}

	/** The entity shown in the target HUD, or null. */
	public static LivingEntity target(float seconds) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || targetId < 0 || System.currentTimeMillis() - targetAt > seconds * 1000f) return null;
		return mc.level.getEntity(targetId) instanceof LivingEntity le && le.isAlive() ? le : null;
	}
}
