package dev.vanillaclient.features;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/** Remembers what was in your ender chest the last time you opened it (per world / server). */
public final class EnderChestCache {
	private EnderChestCache() {}

	private static final java.util.Map<String, List<ItemStack>> BY_WORLD = new java.util.HashMap<>();

	private static String world(Minecraft mc) {
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		var srv = mc.getCurrentServer();
		return srv == null ? "?" : "mp:" + srv.ip;
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) return;
		if (!(mc.gui.screen() instanceof ContainerScreen cs)) return;
		if (!(cs.getTitle().getContents() instanceof TranslatableContents tc) || !tc.getKey().equals("container.enderchest")) return;
		var menu = cs.getMenu();
		int n = Math.min(27, menu.slots.size() - 36);
		if (n <= 0) return;
		List<ItemStack> items = new ArrayList<>(n);
		for (int i = 0; i < n; i++) items.add(menu.getSlot(i).getItem().copy());
		BY_WORLD.put(world(mc), items);
	}

	/** null = not opened yet in this world. */
	public static List<ItemStack> items() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? null : BY_WORLD.get(world(mc));
	}
}
