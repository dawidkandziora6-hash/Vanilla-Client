package dev.vanillaclient.map;

import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Waypoints of the current world & dimension. */
public final class Waypoints {
	public static final int[] COLORS = {
			0xFFFFD34D, 0xFFFF5555, 0xFF55FF55, 0xFF55AAFF, 0xFFFF55FF, 0xFF55FFFF, 0xFFFFAA00, 0xFFFFFFFF};

	/** Icon choices for waypoints (item ids, "" = colored diamond). */
	public static final java.util.List<String[]> ICONS = java.util.List.of(
			new String[] {"", "Marker"}, new String[] {"campfire", "Home"}, new String[] {"straw_bed", "Bed"},
			new String[] {"crafting_table", "Base"}, new String[] {"chest", "Storage"}, new String[] {"bell", "Village"},
			new String[] {"obsidian", "Portal"}, new String[] {"ender_eye", "Stronghold"}, new String[] {"blaze_rod", "Fortress"},
			new String[] {"spawner", "Spawner"}, new String[] {"trial_spawner", "Trial"}, new String[] {"wheat", "Farm"},
			new String[] {"iron_pickaxe", "Mine"}, new String[] {"diamond", "Diamonds"}, new String[] {"enchanting_table", "Enchanting"},
			new String[] {"skeleton_skull", "Death"}, new String[] {"tnt", "Danger"}, new String[] {"heart_of_the_sea", "Treasure"},
			new String[] {"oak_sapling", "Nature"}, new String[] {"water_bucket", "Water"}, new String[] {"lava_bucket", "Lava"},
			new String[] {"elytra", "Elytra"}, new String[] {"beacon", "Beacon"}, new String[] {"nether_star", "Special"});

	private Waypoints() {}

	public static net.minecraft.world.item.ItemStack iconStack(Config.Waypoint w) {
		if (w == null || w.icon == null || w.icon.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
		return dev.vanillaclient.gui.Icons.stack(dev.vanillaclient.hud.InfoModules.Counter.item(w.icon));
	}

	public static List<Config.Waypoint> here() {
		List<Config.Waypoint> out = new ArrayList<>();
		String key = WorldMap.currentKey();
		if (key == null) return out;
		for (Config.Waypoint w : Config.get().waypoints) {
			if (key.equals(w.world)) out.add(w);
		}
		return out;
	}

	public static Config.Waypoint add(String name, int x, int y, int z) {
		String key = WorldMap.currentKey();
		if (key == null) return null;
		int n = Config.get().waypoints.size();
		Config.Waypoint w = new Config.Waypoint(name, x, y, z, key, COLORS[n % COLORS.length]);
		w.created = System.currentTimeMillis();
		Config.get().waypoints.add(w);
		Config.save();
		return w;
	}

	/** Key binding: waypoint at your feet. */
	public static void addHere() {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null) return;
		Config.Waypoint w = add("Waypoint " + (here().size() + 1), p.getBlockX(), p.getBlockY(), p.getBlockZ());
		if (w != null) p.sendSystemMessage(Component.literal("§a✔ §f" + w.name + " §7saved at " + w.x + " " + w.y + " " + w.z));
	}
}
