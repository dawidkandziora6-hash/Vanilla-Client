package dev.vanillaclient.hud;

import dev.vanillaclient.mixin.ClientClockManagerAccessor;
import dev.vanillaclient.mixin.ClockInstanceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.clock.WorldClocks;

/**
 * Only reads the overworld clock (nothing is changed). In the Nether/End the last
 * known value is shown.
 */
public final class WorldTime {
	private static long lastTicks = -1;
	private static ClientLevel lastLevel;

	private WorldTime() {}

	/** Total ticks of the overworld clock, or -1 if not known (yet). */
	public static long totalTicks() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			lastLevel = null;
			return lastTicks;
		}
		try {
			var clocks = ((ClientClockManagerAccessor) level.clockManager()).vc$getClocks();
			var holder = level.registryAccess().get(WorldClocks.OVERWORLD);
			if (holder.isPresent()) {
				Object clock = clocks.get(holder.get());
				if (clock instanceof ClockInstanceAccessor acc) {
					lastTicks = acc.vc$getTotalTicks();
					lastLevel = level;
				}
			}
		} catch (RuntimeException e) {
			// Silently fall back to the last value.
		}
		return lastTicks;
	}

	public static long day(boolean countFromOne) {
		long t = totalTicks();
		if (t < 0) return countFromOne ? 1 : 0;
		return t / 24000L + (countFromOne ? 1 : 0);
	}

	/** Clock time: tick 0 = 06:00. */
	public static String clock() {
		long t = totalTicks();
		if (t < 0) return "--:--";
		long tod = Math.floorMod(t, 24000L);
		long hours = (tod / 1000L + 6L) % 24L;
		long minutes = (tod % 1000L) * 60L / 1000L;
		return String.format("%02d:%02d", hours, minutes);
	}

	public static boolean isNight() {
		long t = totalTicks();
		if (t < 0) return false;
		long tod = Math.floorMod(t, 24000L);
		return tod >= 12542 && tod <= 23460;
	}
}
