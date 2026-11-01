package dev.vanillaclient.pvp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/** One click for more FPS: changes Minecraft's own video settings (and can undo it). */
public final class Performance {
	private Performance() {}

	private record Saved(OptionInstance<Object> option, Object value) {}

	private static final List<Saved> BACKUP = new ArrayList<>();

	@SuppressWarnings("unchecked")
	private static <T> void set(OptionInstance<T> o, T value) {
		OptionInstance<Object> raw = (OptionInstance<Object>) (OptionInstance<?>) o;
		if (!value.equals(o.get())) {
			BACKUP.add(new Saved(raw, o.get()));
			o.set(value);
		}
	}

	public static boolean canUndo() {
		return !BACKUP.isEmpty();
	}

	/** Returns how many settings were changed. */
	public static int boost() {
		Options o = Minecraft.getInstance().options;
		int before = BACKUP.size();
		set(o.cloudStatus(), CloudStatus.OFF);
		set(o.particles(), ParticleStatus.DECREASED);
		set(o.entityShadows(), false);
		set(o.biomeBlendRadius(), 0);
		set(o.enableVsync(), false);
		set(o.framerateLimit(), 260);
		set(o.chunkSectionFadeInTime(), 0.0);
		set(o.menuBackgroundBlurriness(), 0);
		set(o.improvedTransparency(), false);
		set(o.vignette(), false);
		set(o.entityDistanceScaling(), 0.75);
		dev.vanillaclient.config.Config.get().performance.cullEntities = true;
		o.save();
		return BACKUP.size() - before;
	}

	public static void undo() {
		for (int i = BACKUP.size() - 1; i >= 0; i--) BACKUP.get(i).option().set(BACKUP.get(i).value());
		BACKUP.clear();
		Minecraft.getInstance().options.save();
	}

	public static void save() {
		Minecraft.getInstance().options.save();
	}
}
