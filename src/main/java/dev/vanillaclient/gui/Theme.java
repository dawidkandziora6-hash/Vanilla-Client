package dev.vanillaclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers so every menu shares the same clean, vanilla look. */
public final class Theme {
	public static final int TEXT = 0xFFFFFFFF;
	public static final int MUTED = 0xFFA0A0A0;
	public static final int DIM = 0xFF707070;
	public static final int ACCENT = 0xFFFFD34D;
	public static final int GOOD = 0xFF55FF55;
	public static final int WARN = 0xFFFFAA00;
	public static final int BAD = 0xFFFF5555;
	public static final int PANEL = 0xA0000000;
	public static final int PANEL_LINE = 0x30FFFFFF;
	public static final int ROW_HOVER = 0x20FFFFFF;
	public static final int ROW_SELECTED = 0x40FFFFFF;

	private static final long START = System.nanoTime();

	private Theme() {}

	/** "Vanilla look": menus use Minecraft's own buttons, sliders and colors, no extra effects. */
	public static boolean vanilla() {
		return dev.vanillaclient.config.Config.get().vanillaMenus;
	}

	/** Seconds since the game started (for animations). */
	public static float time() {
		return (System.nanoTime() - START) / 1_000_000_000f;
	}

	/** 0..1..0 wave. */
	public static float pulse(float speed) {
		return 0.5f + 0.5f * (float) Math.sin(time() * speed);
	}

	/** Ease-out over [0,1]. */
	public static float easeOut(float t) {
		t = Math.max(0f, Math.min(1f, t));
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	public static int alpha(int argb, float a) {
		int base = (argb >>> 24) & 0xFF;
		return ((int) (base * Math.max(0f, Math.min(1f, a))) << 24) | (argb & 0x00FFFFFF);
	}

	public static int lerpColor(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
				| ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	/** Dark translucent panel with a thin light edge (like vanilla lists). */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, PANEL);
		g.outline(x, y, w, h, PANEL_LINE);
	}

	/** Horizontal separator line with softer ends. */
	public static void separator(GuiGraphicsExtractor g, int x, int y, int w) {
		g.fill(x, y, x + 3, y + 1, 0x18FFFFFF);
		g.fill(x + 3, y, x + w - 3, y + 1, 0x40FFFFFF);
		g.fill(x + w - 3, y, x + w, y + 1, 0x18FFFFFF);
	}

	public static void progressBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int color) {
		g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
		int fw = Math.round(w * Math.max(0f, Math.min(1f, frac)));
		if (fw > 0) {
			g.fill(x, y, x + fw, y + h, color);
			g.fill(x, y, x + fw, y + 1, alpha(0xFFFFFFFF, 0.25f));
		}
	}

	/** 8×8 checkbox in pixel style. */
	public static void checkbox(GuiGraphicsExtractor g, int x, int y, boolean done, int color) {
		int frame = (color & 0x00FFFFFF) | 0xC0000000;
		g.outline(x, y, 8, 8, frame);
		if (done) {
			int ok = GOOD;
			g.fill(x + 2, y + 4, x + 3, y + 6, ok);
			g.fill(x + 3, y + 5, x + 4, y + 7, ok);
			g.fill(x + 4, y + 4, x + 5, y + 6, ok);
			g.fill(x + 5, y + 2, x + 6, y + 5, ok);
		}
	}

	/** Colored pill with text (e.g. counters, priority). */
	public static void badge(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, String text, int x, int y, int bg) {
		int w = font.width(text) + 6;
		g.fill(x, y, x + w, y + 10, bg);
		g.text(font, text, x + 3, y + 1, TEXT, false);
	}

	public static int priorityColor(int p) {
		return p == 2 ? BAD : p == 1 ? WARN : 0xFF6A6A6A;
	}
}
