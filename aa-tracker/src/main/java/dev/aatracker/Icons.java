package dev.aatracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Item icons at any size. */
final class Icons {
	private Icons() {}

	static ItemStack stack(Item item) {
		return new ItemStack(item);
	}

	static void scaled(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}
}
