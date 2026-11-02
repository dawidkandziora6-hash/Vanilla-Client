package dev.vanillaclient.zoom;

import dev.vanillaclient.config.Config;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Zoom (default key C). Purely visual: only magnifies the image, changes neither
 * reach nor your FOV setting. Cinematic camera is restored on release.
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
		return key != null && Config.get().zoom.enabled && mc.player != null
				&& mc.gui.screen() == null && key.isDown();
	}

	private static float target() {
		Config.Zoom z = Config.get().zoom;
		if (!held()) return 1f;
		return sessionLevel > 0 ? sessionLevel : z.level;
	}

	/** Called every frame (from the GameRenderer mixin). */
	public static float frameFactor() {
		Config.Zoom z = Config.get().zoom;
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		float t = target();
		if (z.smoothTransition) {
			// Exponential approach, independent of FPS.
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
		boolean want = held() && Config.get().zoom.cinematicCamera;
		if (want && !cinematicApplied) {
			cinematicBefore = mc.options.smoothCamera;
			mc.options.smoothCamera = true;
			cinematicApplied = true;
		} else if (!want && cinematicApplied) {
			mc.options.smoothCamera = cinematicBefore;
			cinematicApplied = false;
		}
		if (!held() && Config.get().zoom.resetLevelOnRelease) {
			sessionLevel = -1f;
		}
	}

	/** Scroll while zooming: true = scroll was used. */
	public static boolean onScroll(double vertical) {
		Config.Zoom z = Config.get().zoom;
		if (!z.scrollToAdjust || !held() || vertical == 0) return false;
		float base = sessionLevel > 0 ? sessionLevel : z.level;
		float next = (float) (base * Math.pow(1.15, Math.signum(vertical)));
		next = Config.clamp(next, 1.5f, 50f);
		if (z.resetLevelOnRelease) {
			sessionLevel = next;
		} else {
			z.level = next;
			Config.save();
		}
		return true;
	}

	public static float currentFactor() {
		return current;
	}

	private static float lastLevel = 4f;

	/** 0 = hand fully visible, 1 = fully away (follows the zoom animation). */
	public static float handHidden() {
		if (held()) lastLevel = Math.max(1.5f, target());
		float span = Math.max(0.5f, lastLevel - 1f);
		return Config.clamp((current - 1f) / span, 0f, 1f);
	}
}
