package dev.vanillaclient.crosshair;

import dev.vanillaclient.config.Config;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
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
			if (!mc.options.getCameraType().isFirstPerson() && !c.thirdPerson) {
				return; // like vanilla: no crosshair in third person
			}
			int cx = g.guiWidth() / 2;
			int cy = g.guiHeight() / 2;
			int scale = Math.max(1, Math.round(c.scale));
			draw(g, c.size, c.pixels, c.color, c.outline, c.outlineColor, cx, cy, scale);
			if (c.attackIndicator) {
				drawAttackIndicator(g, mc, cx, cy, c, scale);
			} else {
				drawVanillaAttackIndicator(g, mc, cx, cy);
			}
		};
	}

	public static void draw(GuiGraphicsExtractor g, Config.Crosshair c, int cx, int cy, int scale) {
		draw(g, c.size, c.pixels, c.color, c.outline, c.outlineColor, cx, cy, scale);
	}

	/**
	 * Draws the pattern centered on (cx, cy) with whole-pixel scaling, so every
	 * pixel stays crisp and the same size. The outline is drawn exactly once per
	 * outline pixel (no darker overlaps with transparent colors).
	 */
	public static void draw(GuiGraphicsExtractor g, int n, String p, int color, boolean outline, int outlineColor, int cx, int cy, int scale) {
		if (p == null || p.length() != n * n) return;
		int ox = cx - (n * scale) / 2;
		int oy = cy - (n * scale) / 2;
		if (outline) {
			for (int y = -1; y <= n; y++) {
				for (int x = -1; x <= n; x++) {
					if (on(p, n, x, y)) continue;
					if (on(p, n, x - 1, y) || on(p, n, x + 1, y) || on(p, n, x, y - 1) || on(p, n, x, y + 1)) {
						g.fill(ox + x * scale, oy + y * scale, ox + (x + 1) * scale, oy + (y + 1) * scale, outlineColor);
					}
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
				int start = x;
				while (x < n && p.charAt(y * n + x) == '1') x++;
				g.fill(ox + start * scale, oy + y * scale, ox + x * scale, oy + (y + 1) * scale, color);
			}
		}
	}

	private static boolean on(String p, int n, int x, int y) {
		return x >= 0 && y >= 0 && x < n && y < n && p.charAt(y * n + x) == '1';
	}

	private static final net.minecraft.resources.Identifier IND_FULL = net.minecraft.resources.Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
	private static final net.minecraft.resources.Identifier IND_BG = net.minecraft.resources.Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
	private static final net.minecraft.resources.Identifier IND_PROGRESS = net.minecraft.resources.Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");

	/** The normal Minecraft attack indicator (only if it is set to "Crosshair" in the options). */
	private static void drawVanillaAttackIndicator(GuiGraphicsExtractor g, Minecraft mc, int cx, int cy) {
		if (mc.options.attackIndicator().get() != net.minecraft.client.AttackIndicatorStatus.CROSSHAIR) return;
		float f = mc.player.getAttackStrengthScale(0f);
		boolean full = false;
		if (mc.crosshairPickEntity instanceof net.minecraft.world.entity.LivingEntity target && f >= 1f) {
			full = mc.player.getCurrentItemAttackStrengthDelay() > 5f && target.isAlive();
		}
		int y = cy - 7 + 16;
		int x = cx - 8;
		var pipe = net.minecraft.client.renderer.RenderPipelines.CROSSHAIR;
		if (full) {
			g.blitSprite(pipe, IND_FULL, x, y, 16, 16);
		} else if (f < 1f) {
			int l = (int) (f * 17f);
			g.blitSprite(pipe, IND_BG, x, y, 16, 4);
			g.blitSprite(pipe, IND_PROGRESS, 16, 4, 0, 0, x, y, l, 4);
		}
	}

	private static void drawAttackIndicator(GuiGraphicsExtractor g, Minecraft mc, int cx, int cy, Config.Crosshair c, int scale) {
		float strength = mc.player.getAttackStrengthScale(0f);
		if (strength >= 1f) return;
		int half = (c.size * scale) / 2 + 3;
		int w = 16;
		int x = cx - w / 2;
		int y = cy + half;
		g.fill(x - 1, y - 1, x + w + 1, y + 3, 0x80000000);
		g.fill(x, y, x + Math.round(w * strength), y + 2, (c.color & 0x00FFFFFF) | 0xFF000000);
	}
}
