package dev.vanillaclient.features;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.mixin.ContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Chest helpers: "Sort" puts the chest in order (stacks merged, same items together),
 * "Store" moves everything from your inventory into the chest that already has that item.
 * Uses normal clicks – the same as doing it by hand, just faster.
 */
public final class ChestTools {
	private ChestTools() {}

	public static boolean supported(Screen s) {
		return s instanceof ContainerScreen || s instanceof ShulkerBoxScreen;
	}

	public static void addButtons(Screen screen, java.util.List<net.minecraft.client.gui.components.AbstractWidget> widgets) {
		if (!Config.get().chestButtons || !supported(screen)) return;
		ContainerScreenAccessor acc = (ContainerScreenAccessor) screen;
		int x = acc.vc$left() + acc.vc$width() + 3;
		int y = acc.vc$top() + 4;
		AbstractContainerScreen<?> cs = (AbstractContainerScreen<?>) screen;
		widgets.add(Button.builder(Component.literal("⇅ Sort"), b -> sort(cs.getMenu()))
				.bounds(x, y, 54, 16).tooltip(Tooltip.create(Component.literal("Sort this chest: merge stacks, group items"))).build());
		widgets.add(Button.builder(Component.literal("⇩ Store"), b -> store(cs.getMenu()))
				.bounds(x, y + 18, 54, 16).tooltip(Tooltip.create(Component.literal("Move items from your inventory that this chest already has"))).build());
	}

	/** Called by the sort key while a chest is open. */
	public static void sortOpen() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() instanceof AbstractContainerScreen<?> cs && supported(cs)) sort(cs.getMenu());
	}

	private static int chestSize(AbstractContainerMenu menu) {
		return menu.slots.size() - 36;
	}

	private static void click(AbstractContainerMenu menu, int slot, int button, ContainerInput input) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.player == null) return;
		mc.gameMode.handleContainerInput(menu.containerId, slot, button, input, mc.player);
	}

	private static boolean same(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b);
	}

	private static String key(ItemStack s) {
		Item it = s.getItem();
		int group = it instanceof BlockItem ? 0 : s.isStackable() ? 1 : 2;
		return group + ":" + BuiltInRegistries.ITEM.getKey(it);
	}

	public static void sort(AbstractContainerMenu menu) {
		int n = chestSize(menu);
		if (n <= 0 || !menu.getCarried().isEmpty()) return;
		// 1) merge partial stacks
		for (int i = 0; i < n; i++) {
			ItemStack a = menu.getSlot(i).getItem();
			if (a.isEmpty() || a.getCount() >= a.getMaxStackSize()) continue;
			for (int j = i + 1; j < n; j++) {
				ItemStack b = menu.getSlot(j).getItem();
				if (b.isEmpty() || !same(a, b)) continue;
				click(menu, j, 0, ContainerInput.PICKUP);
				click(menu, i, 0, ContainerInput.PICKUP);
				if (!menu.getCarried().isEmpty()) click(menu, j, 0, ContainerInput.PICKUP);
				a = menu.getSlot(i).getItem();
				if (a.getCount() >= a.getMaxStackSize()) break;
			}
		}
		// 2) order: blocks, then stackable items, then tools; bigger stacks first
		List<ItemStack> wanted = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (!s.isEmpty()) wanted.add(s.copy());
		}
		wanted.sort(Comparator.comparing(ChestTools::key).thenComparing(s -> -s.getCount()));
		for (int target = 0; target < wanted.size(); target++) {
			ItemStack want = wanted.get(target);
			ItemStack cur = menu.getSlot(target).getItem();
			if (same(cur, want) && cur.getCount() == want.getCount()) continue;
			int from = -1;
			for (int j = target + 1; j < n; j++) {
				ItemStack s = menu.getSlot(j).getItem();
				if (same(s, want) && s.getCount() == want.getCount()) {
					from = j;
					break;
				}
			}
			if (from < 0) continue;
			// swap target <-> from with the cursor
			click(menu, from, 0, ContainerInput.PICKUP);
			click(menu, target, 0, ContainerInput.PICKUP);
			if (!menu.getCarried().isEmpty()) click(menu, from, 0, ContainerInput.PICKUP);
			if (!menu.getCarried().isEmpty()) return; // something unexpected – stop safely
		}
	}

	/** Shift-clicks every inventory stack whose item is already in the chest. */
	public static void store(AbstractContainerMenu menu) {
		int n = chestSize(menu);
		if (n <= 0 || !menu.getCarried().isEmpty()) return;
		Set<Item> inChest = new HashSet<>();
		for (int i = 0; i < n; i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (!s.isEmpty()) inChest.add(s.getItem());
		}
		for (int i = n; i < menu.slots.size(); i++) {
			ItemStack s = menu.getSlot(i).getItem();
			if (!s.isEmpty() && inChest.contains(s.getItem())) click(menu, i, 0, ContainerInput.QUICK_MOVE);
		}
	}
}
