package dev.logbuch.gui;

import dev.logbuch.config.CrosshairPresets;
import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.crosshair.CrosshairRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Pixel-Malprogramm fürs Fadenkreuz. Linksklick malt, Rechtsklick radiert,
 * Spiegeln malt symmetrisch in alle vier Viertel.
 */
public final class CrosshairEditorScreen extends LbScreen {
	private static boolean mirror = true;
	private static int presetIndex;

	private final LogbuchConfig.Crosshair c = LogbuchConfig.get().crosshair;
	private char[] pixels;
	private int paintValue = -1; // -1 = nicht am Malen, sonst '0' oder '1'

	public CrosshairEditorScreen(Screen parent) {
		super(Component.literal("Fadenkreuz malen"), parent);
		pixels = c.pixels.toCharArray();
	}

	private int cell() {
		int avail = Math.min(Math.min(this.height - 90, this.width / 2 - 20), 200);
		return Math.max(4, Math.min(14, avail / c.size));
	}

	private int gridX() {
		return this.width / 2 - 8 - c.size * cell();
	}

	private int gridY() {
		return 34;
	}

	@Override
	protected void init() {
		super.init();
		int x = this.width / 2 + 8;
		int y = 34;
		int w = 110;
		action(x, y, 53, "Größe −", () -> resize(c.size - 2)).active = c.size > 3;
		action(x + 57, y, 53, "Größe +", () -> resize(c.size + 2)).active = c.size < 31;
		action(x, y + 24, w, "Vorlage: " + CrosshairPresets.NAMES.get(presetIndex), () -> {
			presetIndex = (presetIndex + 1) % CrosshairPresets.NAMES.size();
			pixels = CrosshairPresets.build(CrosshairPresets.NAMES.get(presetIndex), c.size).toCharArray();
			store();
			rebuildWidgets();
		});
		toggle(x, y + 48, w, "Spiegeln", () -> mirror, v -> mirror = v);
		action(x, y + 72, 53, "Leeren", () -> {
			pixels = CrosshairPresets.empty(c.size).toCharArray();
			store();
		});
		action(x + 57, y + 72, 53, "Umkehren", () -> {
			for (int i = 0; i < pixels.length; i++) pixels[i] = pixels[i] == '1' ? '0' : '1';
			store();
		});
		color(x, y + 96, w, "Farbe", () -> c.color, v -> c.color = v);
		toggle(x, y + 120, w, "Benutzen", () -> c.enabled, v -> c.enabled = v);
		action(this.width / 2 - 100, this.height - 28, 200, "Fertig", this::onClose);
	}

	private void resize(int newSize) {
		newSize = Math.max(3, Math.min(31, newSize));
		char[] next = CrosshairPresets.empty(newSize).toCharArray();
		int off = (newSize - c.size) / 2;
		for (int y = 0; y < c.size; y++) {
			for (int x = 0; x < c.size; x++) {
				int nx = x + off;
				int ny = y + off;
				if (nx >= 0 && ny >= 0 && nx < newSize && ny < newSize) next[ny * newSize + nx] = pixels[y * c.size + x];
			}
		}
		c.size = newSize;
		pixels = next;
		store();
		rebuildWidgets();
	}

	private void store() {
		c.pixels = new String(pixels);
		LogbuchConfig.save();
	}

	private int[] cellAt(double mx, double my) {
		int s = cell();
		int gx = (int) Math.floor((mx - gridX()) / s);
		int gy = (int) Math.floor((my - gridY()) / s);
		if (gx < 0 || gy < 0 || gx >= c.size || gy >= c.size) return null;
		return new int[] {gx, gy};
	}

	private void paint(int x, int y) {
		int n = c.size;
		char v = (char) paintValue;
		set(x, y, v);
		if (mirror) {
			set(n - 1 - x, y, v);
			set(x, n - 1 - y, v);
			set(n - 1 - x, n - 1 - y, v);
		}
	}

	private void set(int x, int y, char v) {
		pixels[y * c.size + x] = v;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int[] at = cellAt(event.x(), event.y());
		if (at != null && (event.button() == 0 || event.button() == 1)) {
			paintValue = event.button() == 0 ? '1' : '0';
			paint(at[0], at[1]);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (paintValue >= 0) {
			int[] at = cellAt(event.x(), event.y());
			if (at != null) paint(at[0], at[1]);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (paintValue >= 0) {
			paintValue = -1;
			store();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		int s = cell();
		int gx = gridX();
		int gy = gridY();
		int n = c.size;
		int size = n * s;
		g.fill(gx - 1, gy - 1, gx + size + 1, gy + size + 1, 0xFF000000);
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				boolean on = pixels[y * n + x] == '1';
				int checker = ((x + y) & 1) == 0 ? 0xFF2A2A2A : 0xFF333333;
				int px = gx + x * s;
				int py = gy + y * s;
				g.fill(px, py, px + s, py + s, on ? (c.color | 0xFF000000) : checker);
			}
		}
		// Mittellinien zur Orientierung
		int mid = gx + (n / 2) * s;
		g.fill(mid, gy, mid + 1, gy + size, 0x40FFFFFF);
		g.fill(mid + s - 1, gy, mid + s, gy + size, 0x40FFFFFF);
		int midY = gy + (n / 2) * s;
		g.fill(gx, midY, gx + size, midY + 1, 0x40FFFFFF);
		g.fill(gx, midY + s - 1, gx + size, midY + s, 0x40FFFFFF);
		int[] at = cellAt(mouseX, mouseY);
		if (at != null) {
			int px = gx + at[0] * s;
			int py = gy + at[1] * s;
			g.fill(px, py, px + s, py + 1, 0xFFFFFFFF);
			g.fill(px, py + s - 1, px + s, py + s, 0xFFFFFFFF);
			g.fill(px, py, px + 1, py + s, 0xFFFFFFFF);
			g.fill(px + s - 1, py, px + s, py + s, 0xFFFFFFFF);
		}

		// Vorschau in echter Größe vor Himmel und Gras
		int vx = this.width / 2 + 8;
		int vy = 34 + 148;
		int vw = 110;
		int vh = Math.max(30, Math.min(60, this.height - 40 - vy));
		g.fill(vx, vy, vx + vw, vy + vh, 0xFF7BA4E0);
		g.fill(vx, vy + vh / 2, vx + vw, vy + vh, 0xFF5E8C3A);
		LogbuchConfig.Crosshair preview = new LogbuchConfig.Crosshair();
		preview.size = n;
		preview.pixels = new String(pixels);
		preview.color = c.color;
		preview.outline = c.outline;
		preview.outlineColor = c.outlineColor;
		CrosshairRenderer.draw(g, preview, vx + vw / 2, vy + vh / 2, c.scale);
		g.text(this.font, "§7Links malen · Rechts radieren", gx, gy + size + 6, 0xFFFFFFFF, false);
	}
}
