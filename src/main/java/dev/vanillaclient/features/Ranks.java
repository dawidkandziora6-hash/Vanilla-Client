package dev.vanillaclient.features;

import java.util.List;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;

/**
 * Ranks you earn by playing. Hours = the larger of your Vanilla Client play time
 * and the world's own "Time Played" statistic. Only visible to you.
 */
public final class Ranks {
	public record Rank(String id, String symbol, String name, int hours, int color) {
		public String plain() {
			return symbol + " " + name;
		}
	}

	public static final List<Rank> ALL = List.of(
			new Rank("newcomer", "◇", "Newcomer", 0, 0xFFB0B0B0),
			new Rank("wanderer", "◆", "Wanderer", 1, 0xFFFFFFFF),
			new Rank("explorer", "✦", "Explorer", 3, 0xFF6CFF6C),
			new Rank("adventurer", "✧", "Adventurer", 6, 0xFF55FFFF),
			new Rank("veteran", "★", "Veteran", 12, 0xFF5AA0FF),
			new Rank("elite", "✪", "Elite", 24, 0xFFB46CFF),
			new Rank("master", "❖", "Master", 50, 0xFFFFAA00),
			new Rank("legend", "♛", "Legend", 100, 0xFFFF5555),
			new Rank("mythic", "☀", "Mythic", 250, 0xFFFFE04A),
			new Rank("immortal", "♔", "Immortal", 500, 0xFFFF6CFF));

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

	public static Rank best() {
		double h = hours();
		Rank out = ALL.get(0);
		for (Rank r : ALL) if (h >= r.hours()) out = r;
		return out;
	}

	public static boolean unlocked(Rank r) {
		return hours() >= r.hours();
	}

	/** The rank you show: your choice if unlocked, else the best one. */
	public static Rank current() {
		String c = Config.get().ranks.chosen;
		if (c != null && !c.isEmpty()) {
			for (Rank r : ALL) if (r.id().equals(c) && unlocked(r)) return r;
		}
		return best();
	}

	public static Rank next() {
		double h = hours();
		for (Rank r : ALL) if (r.hours() > h) return r;
		return null;
	}

	public static String formatHours(double h) {
		if (h < 1) return Math.round(h * 60) + " min";
		return String.format(java.util.Locale.ROOT, "%.1f h", h);
	}

	/** Name tag text: rank symbol + your name. */
	public static Component decorate(Component name) {
		Config.Ranks cfg = Config.get().ranks;
		if (!cfg.enabled || !cfg.symbolInName) return name;
		Rank r = current();
		MutableComponent out = Component.literal(r.symbol() + " ").withColor(r.color() & 0xFFFFFF);
		return out.append(name);
	}
}
