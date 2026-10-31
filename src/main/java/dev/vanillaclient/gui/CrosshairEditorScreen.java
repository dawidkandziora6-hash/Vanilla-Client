package dev.vanillaclient.gui;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.config.CrosshairPresets;
import dev.vanillaclient.crosshair.CrosshairRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Pixel paint tool for the crosshair: pencil, eraser, line and fill,
 * four mirror modes, undo (Ctrl+Z) and a live preview at real size.
 */
public final class CrosshairEditorScreen extends BaseScreen {
	private enum Tool {
		PENCIL("✎ Pencil"), ERASER("⌫ Eraser"), LINE("╱ Line"), FILL("▣ Fill");

		final String label;

		Tool(String label) {
			this.label = label;
		}
	}

	private enum Mirror {
		OFF("Off"), H("↔"), V("↕"), BOTH("✥ Both");

		final String label;

		Mirror(String label) {
			this.label = label;
		}
	}

	private static Tool tool = Tool.PENCIL;
	private static Mirror mirror = Mirror.BOTH;
	private static int presetIndex;

	private final Config.Crosshair c = Config.get().crosshair;
	private char[] pixels;
	private final Deque<String> undo = new ArrayDeque<>();
	private boolean painting;
	private char paintValue;
	private int lineStartX = -1, lineStartY = -1;
	private int lastMouseX, lastMouseY;
	private long changedAt;

	public CrosshairEditorScreen(Screen parent) {
		super(Component.literal("Paint Crosshair"), parent);
		pixels = c.pixels.toCharArray();
	}

	// ------------------------------------------------------------ layout

	private int cell() {
		int avail = Math.min(Math.min(this.height - 80, this.width / 2 - 30), 220);
		return Math.max(3, avail / c.size);
	}

	private int gridSize() {
		return cell() * c.size;
	}

	private int gridX() {
		return this.width / 2 - 14 - gridSize();
	}

	private int gridY() {
		return 34 + Math.max(0, (this.height - 80 - gridSize()) / 2);
	}

	private int panelX() {
		return this.width / 2 + 6;
	}

	// ------------------------------------------------------------ widgets

	@Override
	protected void init() {
		super.init();
		int x = panelX();
		int y = 34;
		int w = 150;
		int half = (w - 4) / 2;
		// tools
		int tw = (w - 6) / 4;
		for (Tool t : Tool.values()) {
			Button b = action(x + t.ordinal() * (tw + 2), y, tw, t.label.substring(0, 1), () -> {
				tool = t;
				lineStartX = -1;
				rebuildWidgets();
			});
			b.active = t != tool;
			b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(t.label)));
		}
		y += 22;
		cycle(x, y, half, "", java.util.List.of(Mirror.values()), () -> mirror, v -> mirror = v, v -> "Mirror " + v.label);
		action(x + half + 4, y, half, CrosshairPresets.NAMES.get(presetIndex), () -> {
			presetIndex = (presetIndex + 1) % CrosshairPresets.NAMES.size();
			push();
			pixels = CrosshairPresets.build(CrosshairPresets.NAMES.get(presetIndex), c.size).toCharArray();
			store();
			rebuildWidgets();
		}).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Preset (click for the next one)")));
		y += 22;
		action(x, y, half, "Grid −", () -> resize(c.size - 2)).active = c.size > 3;
		action(x + half + 4, y, half, "Grid +", () -> resize(c.size + 2)).active = c.size < 31;
		y += 22;
		action(x, y, half, "↶ Undo", this::undo).active = !undo.isEmpty();
		action(x + half + 4, y, half, "Invert", () -> {
			push();
			for (int i = 0; i < pixels.length; i++) pixels[i] = pixels[i] == '1' ? '0' : '1';
			store();
		});
		y += 22;
		action(x, y, half, "Clear", () -> {
			push();
			pixels = CrosshairPresets.empty(c.size).toCharArray();
			store();
		});
		color(x + half + 4, y, half, "Color", () -> c.color, v -> c.color = v);
		y += 22;
		toggle(x, y, half, "Outline", () -> c.outline, v -> c.outline = v);
		color(x + half + 4, y, half, "Edge", () -> c.outlineColor, v -> c.outlineColor = v);
		y += 22;
		slider(x, y, w, 1, 5, 1, () -> c.scale, v -> c.scale = (float) v, v -> "In-game size: " + (int) v + "×");
		y += 22;
		toggle(x, y, w, "Use custom crosshair", () -> c.enabled, v -> c.enabled = v);
		action(this.width / 2 - 100, this.height - 26, 200, "✔ Done", this::onClose);
	}

	private void resize(int newSize) {
		newSize = Math.max(3, Math.min(31, newSize));
		push();
		char[] next = CrosshairPresets.empty(newSize).toCharArray();
		int off = (newSize - c.size) / 2;
		for (int y = 0; y < c.size; y++) {
			for (int x = 0; x < c.size; x++) {
				int nx = x + off, ny = y + off;
				if (nx >= 0 && ny >= 0 && nx < newSize && ny < newSize) next[ny * newSize + nx] = pixels[y * c.size + x];
			}
		}
		c.size = newSize;
		pixels = next;
		store();
		rebuildWidgets();
	}

	// ------------------------------------------------------------ editing

	private void push() {
		undo.push(c.size + ":" + new String(pixels));
		while (undo.size() > 50) undo.removeLast();
	}

	private void undo() {
		if (undo.isEmpty()) return;
		String s = undo.pop();
		int i = s.indexOf(':');
		c.size = Integer.parseInt(s.substring(0, i));
		pixels = s.substring(i + 1).toCharArray();
		store();
		rebuildWidgets();
	}

	private void store() {
		c.pixels = new String(pixels);
		changedAt = System.currentTimeMillis();
		Config.save();
	}

	private int[] cellAt(double mx, double my) {
		int s = cell();
		int gx = (int) Math.floor((mx - gridX()) / s);
		int gy = (int) Math.floor((my - gridY()) / s);
		if (gx < 0 || gy < 0 || gx >= c.size || gy >= c.size) return null;
		return new int[] {gx, gy};
	}

	private void set(int x, int y, char v) {
		int n = c.size;
		if (x < 0 || y < 0 || x >= n || y >= n) return;
		pixels[y * n + x] = v;
		boolean h = mirror == Mirror.H || mirror == Mirror.BOTH;
		boolean vv = mirror == Mirror.V || mirror == Mirror.BOTH;
		if (h) pixels[y * n + (n - 1 - x)] = v;
		if (vv) pixels[(n - 1 - y) * n + x] = v;
		if (h && vv) pixels[(n - 1 - y) * n + (n - 1 - x)] = v;
	}

	private void line(int x0, int y0, int x1, int y1, char v) {
		int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		while (true) {
			set(x0, y0, v);
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				err += dy;
				x0 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y0 += sy;
			}
		}
	}

	private void fill(int x, int y, char v) {
		int n = c.size;
		char target = pixels[y * n + x];
		if (target == v) return;
		Deque<int[]> q = new ArrayDeque<>();
		q.add(new int[] {x, y});
		while (!q.isEmpty()) {
			int[] p = q.poll();
			if (p[0] < 0 || p[1] < 0 || p[0] >= n || p[1] >= n || pixels[p[1] * n + p[0]] != target) continue;
			set(p[0], p[1], v);
			q.add(new int[] {p[0] + 1, p[1]});
			q.add(new int[] {p[0] - 1, p[1]});
			q.add(new int[] {p[0], p[1] + 1});
			q.add(new int[] {p[0], p[1] - 1});
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int[] at = cellAt(event.x(), event.y());
		if (at != null && (event.button() == Mouse.LEFT || event.button() == Mouse.RIGHT)) {
			boolean erase = event.button() == Mouse.RIGHT || tool == Tool.ERASER;
			paintValue = erase ? '0' : '1';
			push();
			switch (tool) {
				case FILL -> {
					fill(at[0], at[1], paintValue);
					store();
				}
				case LINE -> {
					lineStartX = at[0];
					lineStartY = at[1];
					painting = true;
				}
				default -> {
					set(at[0], at[1], paintValue);
					painting = true;
					lastMouseX = at[0];
					lastMouseY = at[1];
				}
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (painting) {
			if (tool == Tool.LINE && lineStartX >= 0) {
				int[] at = cellAt(event.x(), event.y());
				if (at != null) line(lineStartX, lineStartY, at[0], at[1], paintValue);
				lineStartX = -1;
			}
			painting = false;
			store();
			rebuildWidgets();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_Z && event.hasControlDown()) {
			undo();
			return true;
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------ drawing

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		float in = intro();
		g.centeredText(this.font, "§l✎ Paint Crosshair", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7Left: paint · Right: erase · Ctrl+Z: undo", this.width / 2, 19, 0xFFFFFFFF);

		// keep painting while the mouse is held (pencil/eraser), interpolated
		int[] at = cellAt(mouseX, mouseY);
		if (painting && tool != Tool.LINE && at != null && (at[0] != lastMouseX || at[1] != lastMouseY)) {
			line(lastMouseX, lastMouseY, at[0], at[1], paintValue);
			lastMouseX = at[0];
			lastMouseY = at[1];
		}

		int s = cell(), gx = gridX(), gy = gridY(), n = c.size, size = gridSize();
		g.fill(gx - 2, gy - 2, gx + size + 2, gy + size + 2, 0xFF000000);
		g.outline(gx - 3, gy - 3, size + 6, size + 6, 0x60FFFFFF);
		int col = c.color | 0xFF000000;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				boolean on = pixels[y * n + x] == '1';
				int checker = ((x + y) & 1) == 0 ? 0xFF262626 : 0xFF303030;
				g.fill(gx + x * s, gy + y * s, gx + (x + 1) * s, gy + (y + 1) * s, on ? col : checker);
			}
		}
		// center cross
		int mid = gx + (n / 2) * s;
		g.fill(mid, gy, mid + 1, gy + size, 0x30FFFFFF);
		g.fill(mid + s - 1, gy, mid + s, gy + size, 0x30FFFFFF);
		int midY = gy + (n / 2) * s;
		g.fill(gx, midY, gx + size, midY + 1, 0x30FFFFFF);
		g.fill(gx, midY + s - 1, gx + size, midY + s, 0x30FFFFFF);
		// hover ghost incl. mirrored cells
		if (at != null) {
			int hx = at[0], hy = at[1];
			int ghost = Theme.alpha(0xFFFFFFFF, 0.5f + 0.3f * Theme.pulse(6f));
			ghost(g, hx, hy, ghost);
			if (mirror == Mirror.H || mirror == Mirror.BOTH) ghost(g, n - 1 - hx, hy, 0x60FFFFFF);
			if (mirror == Mirror.V || mirror == Mirror.BOTH) ghost(g, hx, n - 1 - hy, 0x60FFFFFF);
			if (mirror == Mirror.BOTH) ghost(g, n - 1 - hx, n - 1 - hy, 0x60FFFFFF);
			if (tool == Tool.LINE && painting && lineStartX >= 0) {
				g.text(this.font, "§7line " + (Math.abs(hx - lineStartX) + 1) + "×" + (Math.abs(hy - lineStartY) + 1), gx, gy + size + 6, 0xFFFFFFFF, false);
			}
		}
		g.text(this.font, "§7" + n + "×" + n + " · " + tool.label, gx, gy - 12, 0xFFFFFFFF, false);
		// small real-size preview next to the grid (always visible)
		int box = Math.max(28, n * Math.max(1, Math.round(c.scale)) + 10);
		int bx = gx - box - 10;
		if (bx > 4) {
			g.fill(bx, gy, bx + box, gy + box / 2, 0xFF7BA4E0);
			g.fill(bx, gy + box / 2, bx + box, gy + box, 0xFF5E8C3A);
			g.outline(bx, gy, box, box, 0x60FFFFFF);
			CrosshairRenderer.draw(g, n, new String(pixels), c.color, c.outline, c.outlineColor, bx + box / 2, gy + box / 2, Math.max(1, Math.round(c.scale)));
			g.text(this.font, "§7real size", bx, gy + box + 3, 0xFFFFFFFF, false);
		}

		// real-size preview over sky & grass
		int vx = panelX();
		int vy = 34 + 22 * 8 + 2;
		int vw = 150;
		int vh = Math.max(36, Math.min(70, this.height - 34 - vy));
		if (vy + 30 < this.height - 30) {
			g.fill(vx, vy, vx + vw, vy + vh, 0xFF7BA4E0);
			g.fill(vx, vy + vh / 2, vx + vw, vy + vh, 0xFF5E8C3A);
			g.fill(vx, vy + vh / 2, vx + vw, vy + vh / 2 + 2, 0xFF4A7A2C);
			g.outline(vx, vy, vw, vh, 0x60FFFFFF);
			String p = new String(pixels);
			int scale = Math.max(1, Math.round(c.scale));
			float flash = 1f - Math.min(1f, (System.currentTimeMillis() - changedAt) / 300f);
			CrosshairRenderer.draw(g, n, p, c.color, c.outline, c.outlineColor, vx + vw / 2, vy + vh / 2, scale);
			if (flash > 0) g.outline(vx, vy, vw, vh, Theme.alpha(Theme.ACCENT, flash));
			g.text(this.font, "§7preview " + scale + "×", vx + 3, vy + 3, 0xFFFFFFFF, false);
		}
	}

	private void ghost(GuiGraphicsExtractor g, int x, int y, int color) {
		int s = cell();
		int px = gridX() + x * s, py = gridY() + y * s;
		g.outline(px, py, s, s, color);
	}
}
