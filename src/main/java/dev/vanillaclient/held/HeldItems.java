package dev.vanillaclient.held;

import java.util.List;
import java.util.function.Predicate;

import dev.vanillaclient.config.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Size settings for items in your own hands: per category, or per single item. */
public final class HeldItems {
	public record Category(String id, String name, Item icon, Predicate<ItemStack> test) {}

	/** Checked in this order – the first match wins. */
	public static final List<Category> CATEGORIES = List.of(
			new Category("shields", "Shields", Items.SHIELD, s -> s.is(Items.SHIELD)),
			new Category("totems", "Totems", Items.TOTEM_OF_UNDYING, s -> s.is(Items.TOTEM_OF_UNDYING)),
			new Category("swords", "Swords", Items.DIAMOND_SWORD, s -> s.is(ItemTags.SWORDS)),
			new Category("axes", "Axes", Items.DIAMOND_AXE, s -> s.is(ItemTags.AXES)),
			new Category("tools", "Pickaxes, shovels, hoes", Items.DIAMOND_PICKAXE,
					s -> s.is(ItemTags.PICKAXES) || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES)),
			new Category("ranged", "Bows, crossbows, tridents", Items.BOW,
					s -> s.is(Items.BOW) || s.is(Items.CROSSBOW) || s.is(Items.TRIDENT) || s.is(Items.MACE)),
			new Category("food", "Food", Items.COOKED_BEEF, s -> s.has(DataComponents.FOOD)),
			new Category("potions", "Potions", Items.POTION,
					s -> s.is(Items.POTION) || s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION)),
			new Category("blocks", "Blocks", Items.GRASS_BLOCK, s -> s.getItem() instanceof BlockItem),
			new Category("other", "Everything else", Items.STICK, s -> true));

	private HeldItems() {}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getPath();
	}

	public static Category categoryOf(ItemStack stack) {
		for (Category c : CATEGORIES) {
			try {
				if (c.test().test(stack)) return c;
			} catch (RuntimeException ignored) {
				// components not ready
			}
		}
		return CATEGORIES.get(CATEGORIES.size() - 1);
	}

	public static float scaleFor(ItemStack stack) {
		Config.HeldItems h = Config.get().heldItems;
		if (!h.enabled || stack == null || stack.isEmpty()) return 1f;
		Float own = h.items.get(id(stack.getItem()));
		if (own != null) return own;
		Float cat = h.categories.get(categoryOf(stack).id());
		return cat == null ? 1f : cat;
	}

	public static float category(String id) {
		Float v = Config.get().heldItems.categories.get(id);
		return v == null ? 1f : v;
	}

	public static void setCategory(String id, float v) {
		if (Math.abs(v - 1f) < 0.001f) Config.get().heldItems.categories.remove(id);
		else Config.get().heldItems.categories.put(id, v);
	}

	public static float totemPopScale() {
		Config.HeldItems h = Config.get().heldItems;
		return h.enabled ? h.totemPopScale : 1f;
	}
}
