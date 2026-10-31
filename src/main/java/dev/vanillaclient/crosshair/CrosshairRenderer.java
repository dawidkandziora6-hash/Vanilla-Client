package dev.vanillaclient.crosshair;

import dev.vanillaclient.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Replaces the vanilla crosshair with your painted one – only when enabled. */
public final class CrosshairRenderer {
	private CrosshairRenderer() {}

	public static HudElement wrap(HudElement vanilla) {
		return (g, delta) -> {
			Config.Crosshair c = Config.get().crosshair;
			Minecraft mc = Minecraft.getInstance();
			if (!c.enabled || mc.player == null) {
				vanilla.extractRenderState(g, delta);
				return;
			}
			if (!mc.options.getCameraType().isFirstPerson()) {
				return; // Like vanilla: no crosshair in third person.
			}
			int cx = g.guiWidth() / 2;
			int cy = g.guiHeight() / 2;
			draw(g, c, cx, cy, c.scale);
			if (c.attackIndicator) {
				drawAttackIndicator(g, mc, cx, cy, c);
			}
		};
	}

	/** Draws the pattern centered on (cx, cy). */
	public static void draw(GuiGraphicsExtractor g, Config.Crosshair c, int cx, int cy, float scale) {
		int n = c.size;
		String p = c.pixels;
		if (p == null || p.length() != n * n) return;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		int ox = -(n / 2);
		int oy = -(n / 2);
		if (c.outline) {
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					if (p.charAt(y * n + x) != '1') continue;
					g.fill(ox + x - 1, oy + y - 1, ox + x + 2, oy + y + 2, c.outlineColor);
				}
			}
		}
		for (int y = 0; y < n; y++) {
			int x = 0;
			while (x < n) {
				if (p.charAt(y * n + x) != '1') {
					x++;
					continue;
				}
				// Merge neighbouring pixels of a row into one rectangle.
				int start = x;
				while (x < n && p.charAt(y * n + x) == '1') x++;
				g.fill(ox + start, oy + y, ox + x, oy + y + 1, c.color);
			}
		}
		g.pose().popMatrix();
	}

	private static void drawAttackIndicator(GuiGraphicsExtractor g, Minecraft mc, int cx, int cy, Config.Crosshair c) {
		float strength = mc.player.getAttackStrengthScale(0f);
		if (strength >= 1f) return;
		int half = Math.round((c.size / 2f + 2f) * c.scale);
		int w = 16;
		int x = cx - w / 2;
		int y = cy + half + 2;
		g.fill(x, y, x + w, y + 2, 0x80000000);
		g.fill(x, y, x + Math.round(w * strength), y + 2, (c.color & 0x00FFFFFF) | 0xFF000000);
	}
}
