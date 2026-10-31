package dev.vanillaclient.features;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;

/**
 * Play time and session time (used by the Session module and the rank page).
 */
public final class Ranks {
	private static int ticks;
	private static boolean requested;
	private static long sessionStart;
	private static int worldTicks;

	private Ranks() {}

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			requested = false;
			sessionStart = 0;
			return;
		}
		if (sessionStart == 0) sessionStart = System.currentTimeMillis();
		if (!requested) {
			requested = true;
			try {
				mc.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
			} catch (RuntimeException ignored) {
				// stats are optional
			}
		}
		if (mc.isPaused()) return;
		Config.Ranks r = Config.get().ranks;
		r.playTicks++;
		if (++ticks % 200 == 0) {
			try {
				worldTicks = mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
			} catch (RuntimeException ignored) {
				// not loaded yet
			}
		}
		if (ticks % 1200 == 0) Config.save();
	}

	public static long sessionMillis() {
		return sessionStart == 0 ? 0 : System.currentTimeMillis() - sessionStart;
	}

	public static double hours() {
		long t = Math.max(Config.get().ranks.playTicks, worldTicks);
		return t / 72000.0;
	}





	public static String formatHours(double h) {
		if (h < 1) return Math.round(h * 60) + " min";
		return String.format(java.util.Locale.ROOT, "%.1f h", h);
	}

}
