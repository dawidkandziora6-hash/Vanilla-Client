package dev.vanillaclient.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Item icons used all over the menus (vanilla "pictures"). */
public final class Icons {
	private static final Map<Item, ItemStack> CACHE = new HashMap<>();
	private static List<Item> categoryIcons;

	private Icons() {}

	/**
	 * Cached item stack for drawing. Before you join a world, Minecraft 26.3 can't
	 * create item stacks yet ("components not bound") – then this returns EMPTY,
	 * which simply draws nothing.
	 */
	public static ItemStack stack(Item item) {
		ItemStack cached = CACHE.get(item);
		if (cached != null) return cached;
		try {
			ItemStack s = new ItemStack(item);
			CACHE.put(item, s);
			return s;
		} catch (RuntimeException notReady) {
			return ItemStack.EMPTY;
		}
	}

	/** true once item icons can be drawn (a world has been loaded). */
	public static boolean ready() {
		return !stack(Items.STICK).isEmpty();
	}

	/** Icons a task category can have. */
	public static List<Item> categoryIcons() {
		if (categoryIcons == null) {
			categoryIcons = List.of(
					Items.WHEAT, Items.BRICKS, Items.IRON_PICKAXE, Items.CHEST,
					Items.REDSTONE, Items.OAK_SAPLING, Items.IRON_SWORD, Items.DIAMOND,
					Items.ENDER_PEARL, Items.MAP, Items.COMPASS, Items.HAY_BLOCK,
					Items.HOPPER, Items.EMERALD, Items.BEACON, Items.NETHER_STAR,
					Items.FISHING_ROD, Items.CRAFTING_TABLE, Items.TNT, Items.GOLDEN_CARROT,
					Items.OBSIDIAN, Items.BOOKSHELF, Items.LANTERN, Items.SPYGLASS);
		}
		return categoryIcons;
	}

	public static ItemStack categoryIcon(int index) {
		List<Item> l = categoryIcons();
		return stack(l.get(Math.floorMod(index, l.size())));
	}

	/** Item icon at half size (8×8), fits next to a line of text. */
	public static void small(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5f, 0.5f);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	/** Item icon at any size. */
	public static void scaled(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}
}
