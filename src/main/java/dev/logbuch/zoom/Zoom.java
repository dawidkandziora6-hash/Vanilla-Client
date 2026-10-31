package dev.logbuch.zoom;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Zoom (Standard-Taste C). Rein optisch: vergrößert nur das Bild, ändert weder
 * Reichweite noch FOV-Einstellung. Kinokamera wird beim Loslassen zurückgesetzt.
 */
public final class Zoom {
	private static KeyMapping key;
	private static float current = 1f;
	private static float sessionLevel = -1f;
	private static long lastFrame;
	private static boolean cinematicApplied;
	private static boolean cinematicBefore;

	private Zoom() {}

	public static void init(KeyMapping zoomKey) {
		key = zoomKey;
	}

	public static boolean held() {
		Minecraft mc = Minecraft.getInstance();
		return key != null && LogbuchConfig.get().zoom.enabled && mc.player != null
				&& mc.gui.screen() == null && key.isDown();
	}

	private static float target() {
		LogbuchConfig.Zoom z = LogbuchConfig.get().zoom;
		if (!held()) return 1f;
		return sessionLevel > 0 ? sessionLevel : z.level;
	}

	/** Pro Bild aufgerufen (aus dem GameRenderer-Mixin). */
	public static float frameFactor() {
		LogbuchConfig.Zoom z = LogbuchConfig.get().zoom;
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		float t = target();
		if (z.smoothTransition) {
			// Exponentielles Annähern, unabhängig von der FPS.
			float k = 1f - (float) Math.exp(-dt * 14f);
			current += (t - current) * k;
			if (Math.abs(current - t) < 0.002f) current = t;
		} else {
			current = t;
		}
		updateCinematic();
		return current;
	}

	private static void updateCinematic() {
		Minecraft mc = Minecraft.getInstance();
		boolean want = held() && LogbuchConfig.get().zoom.cinematicCamera;
		if (want && !cinematicApplied) {
			cinematicBefore = mc.options.smoothCamera;
			mc.options.smoothCamera = true;
			cinematicApplied = true;
		} else if (!want && cinematicApplied) {
			mc.options.smoothCamera = cinematicBefore;
			cinematicApplied = false;
		}
		if (!held() && LogbuchConfig.get().zoom.resetLevelOnRelease) {
			sessionLevel = -1f;
		}
	}

	/** Mausrad beim Zoomen: true = Scroll wurde verbraucht. */
	public static boolean onScroll(double vertical) {
		LogbuchConfig.Zoom z = LogbuchConfig.get().zoom;
		if (!z.scrollToAdjust || !held() || vertical == 0) return false;
		float base = sessionLevel > 0 ? sessionLevel : z.level;
		float next = (float) (base * Math.pow(1.15, Math.signum(vertical)));
		next = LogbuchConfig.clamp(next, 1.5f, 50f);
		if (z.resetLevelOnRelease) {
			sessionLevel = next;
		} else {
			z.level = next;
			LogbuchConfig.save();
		}
		return true;
	}

	public static float currentFactor() {
		return current;
	}
}
