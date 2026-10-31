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

	private Waypoints() {}

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
