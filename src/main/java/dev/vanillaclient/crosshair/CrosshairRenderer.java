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
			draw(g, c, cx, cy, scale);
			if (c.attackIndicator) {
				drawAttackIndicator(g, mc, cx, cy, c, scale);
			} else {
				drawVanillaAttackIndicator(g, mc, cx, cy);
			}
		};
	}

	public static void draw(GuiGraphicsExtractor g, Config.Crosshair c, int cx, int cy, int scale) {
		Minecraft mc = Minecraft.getInstance();
		int color = c.color;
		if (c.dynamicColor && mc.crosshairPickEntity != null) color = c.targetColor;
		if (c.vanillaBlend) {
			drawBlend(g, c.size, c.pixels, cx, cy, scale);
			return;
		}
		int oc = (c.outlineColor & 0x00FFFFFF) | (Math.round(Math.max(0f, Math.min(1f, c.outlineOpacity)) * 255) << 24);
		draw(g, c.size, c.pixels, color, c.outline, oc, Math.max(1, Math.min(3, c.outlineThickness)), cx, cy, scale);
	}

	public static int outlineColor(Config.Crosshair c) {
		return (c.outlineColor & 0x00FFFFFF) | (Math.round(Math.max(0f, Math.min(1f, c.outlineOpacity)) * 255) << 24);
	}

	/** Old signature (editor preview). */
	public static void draw(GuiGraphicsExtractor g, int n, String p, int color, boolean outline, int outlineColor, int cx, int cy, int scale) {
		draw(g, n, p, color, outline, outlineColor, 1, cx, cy, scale);
	}

	// cached outline rectangles for the current pattern
	private static String olKey = "";
	private static int[] olRects = new int[0];

	/**
	 * Crisp crosshair. The outline is a clean ring of "thick" GUI pixels around the shape
	 * (like CS / Valorant): computed once, drawn without overlaps, so see-through
	 * outlines never get darker corners.
	 */
	public static void draw(GuiGraphicsExtractor g, int n, String p, int color, boolean outline, int outlineColor, int thick, int cx, int cy, int scale) {
		if (p == null || p.length() != n * n) return;
		int ox = cx - (n * scale) / 2;
		int oy = cy - (n * scale) / 2;
		if (outline) {
			String key = n + "|" + scale + "|" + thick + "|" + p;
			if (!key.equals(olKey)) {
				olKey = key;
				olRects = outlineRects(n, p, scale, thick);
			}
			for (int i = 0; i < olRects.length; i += 3) {
				g.fill(ox + olRects[i], oy + olRects[i + 1], ox + olRects[i + 2], oy + olRects[i + 1] + 1, outlineColor);
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

	/** Row runs (x1, y, x2) of the outline ring in GUI pixels, relative to the top-left of the shape. */
	private static int[] outlineRects(int n, String p, int scale, int t) {
		int size = n * scale + 2 * t;
		boolean[] on = new boolean[size * size];
		for (int y = 0; y < n * scale; y++) {
			for (int x = 0; x < n * scale; x++) {
				if (p.charAt((y / scale) * n + x / scale) == '1') on[(y + t) * size + x + t] = true;
			}
		}
		boolean[] h = new boolean[size * size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				boolean v = false;
				for (int d = -t; d <= t && !v; d++) {
					int xx = x + d;
					v = xx >= 0 && xx < size && on[y * size + xx];
				}
				h[y * size + x] = v;
			}
		}
		java.util.List<Integer> out = new java.util.ArrayList<>();
		for (int y = 0; y < size; y++) {
			int x = 0;
			while (x < size) {
				boolean ring = false;
				if (!on[y * size + x]) {
					for (int d = -t; d <= t && !ring; d++) {
						int yy = y + d;
						ring = yy >= 0 && yy < size && h[yy * size + x];
					}
				}
				if (!ring) {
					x++;
					continue;
				}
				int start = x;
				while (x < size && !on[y * size + x]) {
					boolean r = false;
					for (int d = -t; d <= t && !r; d++) {
						int yy = y + d;
						r = yy >= 0 && yy < size && h[yy * size + x];
					}
					if (!r) break;
					x++;
				}
				out.add(start - t);
				out.add(y - t);
				out.add(x - t);
			}
		}
		int[] arr = new int[out.size()];
		for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
		return arr;
	}

	// ------------------------------------------------------------ vanilla blending

	private static final net.minecraft.resources.Identifier BLEND_TEX = net.minecraft.resources.Identifier.fromNamespaceAndPath("vanillaclient", "crosshair_blend");
	private static String blendKey = "";
	private static net.minecraft.client.renderer.texture.DynamicTexture blendTex;

	/** Like the vanilla crosshair: inverts whatever is behind it, so it is always visible. */
	private static void drawBlend(GuiGraphicsExtractor g, int n, String p, int cx, int cy, int scale) {
		if (p == null || p.length() != n * n) return;
		String key = n + "|" + p;
		if (blendTex == null || !key.equals(blendKey)) {
			com.mojang.blaze3d.platform.NativeImage img = new com.mojang.blaze3d.platform.NativeImage(n, n, false);
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) img.setPixel(x, y, p.charAt(y * n + x) == '1' ? 0xFFFFFFFF : 0);
			}
			if (blendTex != null) Minecraft.getInstance().getTextureManager().release(BLEND_TEX);
			blendTex = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "vc crosshair", img);
			Minecraft.getInstance().getTextureManager().register(BLEND_TEX, blendTex);
			blendKey = key;
		}
		int ox = cx - (n * scale) / 2;
		int oy = cy - (n * scale) / 2;
		g.blit(net.minecraft.client.renderer.RenderPipelines.CROSSHAIR, BLEND_TEX, ox, oy, 0f, 0f, n * scale, n * scale, n, n, n, n);
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
