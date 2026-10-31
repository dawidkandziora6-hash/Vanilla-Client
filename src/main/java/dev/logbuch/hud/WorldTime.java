package dev.logbuch.hud;

import dev.logbuch.mixin.ClientClockManagerAccessor;
import dev.logbuch.mixin.ClockInstanceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.clock.WorldClocks;

/**
 * Liest die Überwelt-Uhr nur aus (nichts wird verändert). Im Nether/End läuft die
 * Überwelt-Uhr nicht immer mit – dann wird der zuletzt gesehene Wert gezeigt.
 */
public final class WorldTime {
	private static long lastTicks = -1;
	private static ClientLevel lastLevel;

	private WorldTime() {}

	/** Gesamt-Ticks der Überwelt-Uhr oder -1, wenn (noch) unbekannt. */
	public static long totalTicks() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			lastLevel = null;
			return lastTicks;
		}
		try {
			var clocks = ((ClientClockManagerAccessor) level.clockManager()).logbuch$getClocks();
			var holder = level.registryAccess().get(WorldClocks.OVERWORLD);
			if (holder.isPresent()) {
				Object clock = clocks.get(holder.get());
				if (clock instanceof ClockInstanceAccessor acc) {
					lastTicks = acc.logbuch$getTotalTicks();
					lastLevel = level;
				}
			}
		} catch (RuntimeException e) {
			// Fällt still auf den letzten Wert zurück.
		}
		return lastTicks;
	}

	public static long day(boolean countFromOne) {
		long t = totalTicks();
		if (t < 0) return countFromOne ? 1 : 0;
		return t / 24000L + (countFromOne ? 1 : 0);
	}

	/** Uhrzeit wie auf einer Uhr: Tick 0 = 06:00. */
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
