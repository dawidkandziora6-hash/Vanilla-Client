package dev.vanillaclient.visual;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Custom enchant glint: the glint textures are recolored (any color or a slow rainbow).
 * Only changes how it looks on your screen.
 */
public final class Glint {
	private Glint() {}

	private static final Identifier[] IDS = {
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
			Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png")};
	private static final DynamicTexture[] TEX = new DynamicTexture[2];
	private static final int[][] ORIGINAL = new int[2][];
	private static final int[][] SIZE = new int[2][];
	private static String applied = "";
	private static int ticks;

	private static String key() {
		Config.Glint g = Config.get().glint;
		return g.enabled + "/" + g.color + "/" + g.rainbow + "/" + g.strength;
	}

	public static void tick(Minecraft mc) {
		if (++ticks % 2 != 0) return;
		Config.Glint g = Config.get().glint;
		try {
			// a resource reload puts the vanilla texture back -> take over again
			boolean lost = false;
			for (int i = 0; i < 2; i++) {
				AbstractTexture cur = mc.getTextureManager().getTexture(IDS[i]);
				if (TEX[i] != null && cur != TEX[i]) {
					lost = true;
					TEX[i] = null;
					ORIGINAL[i] = null;
				}
			}
			if (!g.enabled && TEX[0] == null) return; // untouched vanilla
			String k = key();
			if (!lost && k.equals(applied) && !(g.enabled && g.rainbow)) return;
			applied = k;
			for (int i = 0; i < 2; i++) paint(mc, i, g);
		} catch (RuntimeException e) {
			Config.LOG.debug("Glint could not be colored", e);
		}
	}

	private static void paint(Minecraft mc, int i, Config.Glint g) {
		if (ORIGINAL[i] == null) {
			try (InputStream in = mc.getResourceManager().getResource(IDS[i]).orElseThrow().open(); NativeImage img = NativeImage.read(in)) {
				int w = img.getWidth(), h = img.getHeight();
				int[] px = new int[w * h];
				for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = img.getPixel(x, y);
				ORIGINAL[i] = px;
				SIZE[i] = new int[] {w, h};
			} catch (Exception e) {
				Config.LOG.warn("Glint texture could not be read", e);
				return;
			}
		}
		int w = SIZE[i][0], h = SIZE[i][1];
		if (TEX[i] == null) {
			TEX[i] = new DynamicTexture(() -> "vanillaclient glint", new NativeImage(w, h, false));
			mc.getTextureManager().register(IDS[i], TEX[i]);
		}
		NativeImage out = TEX[i].getPixels();
		if (out == null) return;
		int col = g.color;
		if (g.rainbow) col = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 6000L) / 6000f, 0.75f, 1f);
		float cr = ((col >> 16) & 0xFF) / 255f, cg = ((col >> 8) & 0xFF) / 255f, cb = (col & 0xFF) / 255f;
		int[] src = ORIGINAL[i];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = src[y * w + x];
				if (!g.enabled) {
					out.setPixel(x, y, p);
					continue;
				}
				int a = (p >>> 24) & 0xFF;
				float v = Math.max((p >> 16) & 0xFF, Math.max((p >> 8) & 0xFF, p & 0xFF)) / 255f * g.strength;
				int r = Math.min(255, Math.round(cr * v * 255)), gg = Math.min(255, Math.round(cg * v * 255)), b = Math.min(255, Math.round(cb * v * 255));
				out.setPixel(x, y, (a << 24) | (r << 16) | (gg << 8) | b);
			}
		}
		TEX[i].upload();
	}
}
