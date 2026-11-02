package dev.vanillaclient.cosmetics;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** "Show without a shield item": your shield design sits in your free off hand. */
public final class FakeShield {
	private FakeShield() {}

	private static final ItemStack SHIELD = new ItemStack(Items.SHIELD);

	/** Which arm gets the fake shield, or null. */
	public static HumanoidArm armFor(LivingEntity e) {
		Minecraft mc = Minecraft.getInstance();
		if (e != mc.player || !Config.get().cosmetics.shieldWithoutItem) return null;
		String id = Cosmetics.effectiveId(Cosmetics.Kind.SHIELD);
		if (id == null || id.equals(Cosmetics.NONE)) return null;
		HumanoidArm off = e.getMainArm().getOpposite();
		if (!e.getItemHeldByArm(off).isEmpty()) return null;
		return off;
	}

	public static ItemStack stack() {
		return SHIELD;
	}
}
