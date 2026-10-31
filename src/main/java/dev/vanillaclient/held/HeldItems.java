package dev.vanillaclient.held;

import dev.vanillaclient.config.Config;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Size settings for items in your own hands. */
public final class HeldItems {
	private HeldItems() {}

	public static float scaleFor(ItemStack stack) {
		Config.HeldItems h = Config.get().heldItems;
		if (!h.enabled || stack == null || stack.isEmpty()) return 1f;
		if (stack.is(Items.SHIELD)) return h.shieldScale;
		if (stack.is(Items.TOTEM_OF_UNDYING)) return h.totemScale;
		return 1f;
	}

	public static float totemPopScale() {
		Config.HeldItems h = Config.get().heldItems;
		return h.enabled ? h.totemPopScale : 1f;
	}
}
