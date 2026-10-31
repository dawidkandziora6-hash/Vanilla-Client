package dev.vanillaclient.gui;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.ui.Ui;
import dev.vanillaclient.map.Waypoints;
import dev.vanillaclient.map.WorldMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Map of everything you explored. Drag to move, scroll to zoom, right-click to
 * place a waypoint, click a waypoint to edit it.
 */
public final class WorldMapScreen extends BaseScreen {
	private static float zoom = -1f;
	private static final int PANEL = 116;

	private double camX, camZ;
	private boolean dragging, follow = true, moved;
	private double lastX, lastY, downX, downY;
	private float shownZoom;
	private int listScroll;

	// waypoint editor popup
	private Config.Waypoint editing;
	private boolean editingNew;
	private EditBox nameBox;
	private Config.Waypoint draggingWp;
	private boolean wpMoved;
	private static final int ED_W = 222, ED_H = 150, ICON_COLS = 12;
	private static final int ROW_H = 22;

	public WorldMapScreen(Screen parent) {
		super(Component.literal("World Map"), parent);
		var p = net.minecraft.client.Minecraft.getInstance().player;
		if (p != null) {
			camX = p.getX();
			camZ = p.getZ();
			WorldMap.loadAround(p.getBlockX(), p.getBlockZ());
		}
		for (int[] t : WorldMap.savedTiles()) WorldMap.tile(t[0], t[1], false);
	}

	private int mapLeft() {
		return 8;
	}

	private int mapRight() {
		return this.width - 8 - PANEL - 6;
	}

	private int mapTop() {
		return 30;
	}

	private int mapBottom() {
		return this.height - 32;
	}

	private int mcx() {
		return (mapLeft() + mapRight()) / 2;
	}

	private int mcy() {
		return (mapTop() + mapBottom()) / 2;
	}

	@Override
	protected void init() {
		super.init();
		if (zoom < 0) {
			// first open: fit the whole loaded area
			int rd = this.minecraft.options.renderDistance().get();
			float fit = (mapBottom() - mapTop()) / 2f / (rd * 16f + 8f);
			zoom = Config.clamp(fit, 0.125f, 4f);
		}
		shownZoom = zoom;
		int y = this.height - 26;
		int w = 96;
		int x0 = this.width / 2 - (w * 4 + 12) / 2;
		action(x0, y, w, "⌖ Center", () -> follow = true);
		action(x0 + (w + 4), y, w, "+ Waypoint here", () -> {
			var p = this.minecraft.player;
			if (p == null) return;
			Config.Waypoint wp = Waypoints.add("Waypoint " + (Waypoints.here().size() + 1), p.getBlockX(), p.getBlockY(), p.getBlockZ());
			if (wp != null) openEditor(wp, true);
		});
		action(x0 + 2 * (w + 4), y, w, Config.get().worldMap.enabled ? "§aRecording" : "§7Paused", () -> {
			Config.get().worldMap.enabled = !Config.get().worldMap.enabled;
			Config.save();
			rebuildWidgets();
		});
		action(x0 + 3 * (w + 4), y, w, "✔ Done", this::onClose);

		if (editing != null) editorWidgets();
	}

	// ------------------------------------------------------------ waypoint editor

	private int edX() {
		return this.width / 2 - ED_W / 2;
	}

	private int edY() {
		return Math.max(4, this.height / 2 - ED_H / 2);
	}

	private void openEditor(Config.Waypoint w, boolean isNew) {
		editing = w;
		editingNew = isNew;
		rebuildWidgets();
	}

	private void editorWidgets() {
		int x = edX(), y = edY();
		nameBox = new EditBox(this.font, x + 8, y + 20, ED_W - 16, 16, Component.literal("Name"));
		nameBox.setMaxLength(32);
		nameBox.setValue(editing.name);
		nameBox.setResponder(v -> editing.name = v);
		addRenderableWidget(nameBox);
		setFocused(nameBox);
		int bw = (ED_W - 16 - 3 * 12 - 8) / 3;
		int[] vals = {editing.x, editing.y, editing.z};
		for (int i = 0; i < 3; i++) {
			final int axis = i;
			EditBox b = new EditBox(this.font, x + 8 + 12 + i * (bw + 16), y + 40, bw, 14, Component.literal("XYZ".substring(i, i + 1)));
			b.setMaxLength(9);
			b.setValue(String.valueOf(vals[i]));
			b.setResponder(v -> {
				try {
					int n = Integer.parseInt(v.trim());
					if (axis == 0) editing.x = n;
					else if (axis == 1) editing.y = n;
					else editing.z = n;
				} catch (NumberFormatException ignored) {
					// typing in progress
				}
			});
			addRenderableWidget(b);
		}
		int cw = 18;
		for (int i = 0; i < Waypoints.COLORS.length; i++) {
			final int c = Waypoints.COLORS[i];
			Button b = action(x + 8 + i * (cw + 2), y + 104, cw, "", () -> {
				editing.color = c;
				Config.save();
				rebuildWidgets();
			});
			b.setMessage(Component.literal((editing.color | 0xFF000000) == c ? "§l✔" : ""));
		}
		action(x + 8 + 8 * (cw + 2) + 2, y + 104, ED_W - 18 - 8 * (cw + 2), "✎ Custom", () -> {
			Config.Waypoint w = editing;
			this.minecraft.setScreenAndShow(new ColorPickerScreen(this, "Waypoint color", w.color, c -> {
				w.color = c | 0xFF000000;
				Config.save();
			}));
		});
		int b3 = (ED_W - 16 - 8) / 3;
		action(x + 8, y + 126, b3, editing.visible ? "§a◉ Shown" : "§7○ Hidden", () -> {
			editing.visible = !editing.visible;
			Config.save();
			rebuildWidgets();
		});
		action(x + 12 + b3, y + 126, b3, "§c✖ Delete", () -> {
			Config.get().waypoints.remove(editing);
			Config.save();
			editing = null;
			rebuildWidgets();
		});
		action(x + 16 + 2 * b3, y + 126, b3, "✔ Save", this::closeEditor);
	}

	private void closeEditor() {
		if (editing.name.isBlank()) editing.name = "Waypoint";
		Config.save();
		editing = null;
		rebuildWidgets();
	}

	private int iconCellX(int i) {
		return edX() + 8 + (i % ICON_COLS) * 17 + 1;
	}

	private int iconCellY(int i) {
		return edY() + 62 + (i / ICON_COLS) * 18;
	}

	// ------------------------------------------------------------ frame

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		var p = this.minecraft.player;
		if (dragging) {
			if (Math.abs(mouseX - downX) + Math.abs(mouseY - downY) > 3) moved = true;
			camX -= (mouseX - lastX) / shownZoom;
			camZ -= (mouseY - lastY) / shownZoom;
			lastX = mouseX;
			lastY = mouseY;
			if (moved) follow = false;
		}
		if (draggingWp != null) {
			if (Math.abs(mouseX - downX) + Math.abs(mouseY - downY) > 3) wpMoved = true;
			if (wpMoved) {
				draggingWp.x = (int) Math.floor(camX + (mouseX - mcx()) / shownZoom);
				draggingWp.z = (int) Math.floor(camZ + (mouseY - mcy()) / shownZoom);
			}
		}
		if (follow && p != null) {
			camX += (p.getX() - camX) * 0.3;
			camZ += (p.getZ() - camZ) * 0.3;
		}
		shownZoom += (zoom - shownZoom) * 0.3f;
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	private float sx(double wx) {
		return (float) (mcx() + (wx - camX) * shownZoom);
	}

	private float sy(double wz) {
		return (float) (mcy() + (wz - camZ) * shownZoom);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int l = mapLeft(), r = mapRight(), top = mapTop(), bottom = mapBottom();
		g.fill(l, top, r, bottom, 0xFF101010);
		g.enableScissor(l, top, r, bottom);
		// faint grid for unexplored space
		float z = shownZoom;
		int step = z >= 1 ? 16 : z >= 0.25f ? 64 : 256;
		double startX = Math.floor((camX - (mcx() - l) / z) / step) * step;
		for (double gx = startX; sx(gx) < r; gx += step) g.fill(Math.round(sx(gx)), top, Math.round(sx(gx)) + 1, bottom, 0x14FFFFFF);
		double startZ = Math.floor((camZ - (mcy() - top) / z) / step) * step;
		for (double gz = startZ; sy(gz) < bottom; gz += step) g.fill(l, Math.round(sy(gz)), r, Math.round(sy(gz)) + 1, 0x14FFFFFF);
		for (WorldMap.Tile t : WorldMap.tiles()) {
			float x = sx(t.tx * WorldMap.TILE), y = sy(t.tz * WorldMap.TILE);
			float size = WorldMap.TILE * z;
			if (x > r || y > bottom || x + size < l || y + size < top) continue;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(z, z);
			g.blit(RenderPipelines.GUI_TEXTURED, t.texture(), 0, 0, 0f, 0f, WorldMap.TILE, WorldMap.TILE, WorldMap.TILE, WorldMap.TILE);
			g.pose().popMatrix();
		}
		g.disableScissor();
		g.outline(l - 1, top - 1, r - l + 2, bottom - top + 2, 0x60FFFFFF);
		Theme.panel(g, r + 6, top, PANEL, bottom - top);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int l = mapLeft(), r = mapRight(), top = mapTop(), bottom = mapBottom();
		float in = intro();
		g.centeredText(this.font, "§l⌖ World Map", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7Drag · scroll to zoom · right-click: new waypoint · drag a waypoint to move it", this.width / 2, 19, 0xFFFFFFFF);

		g.enableScissor(l, top, r, bottom);
		// task markers
		if (Config.get().worldMap.showTasks) {
			for (Config.TodoItem t : Config.get().todos) {
				int[] c = parseCoords(t.coords);
				if (c == null) continue;
				int mx = Math.round(sx(c[0] + 0.5)), my = Math.round(sy(c[1] + 0.5));
				Config.Category cat = Config.get().category(t.category);
				Icons.scaled(g, cat == null ? Icons.stack(Items.BOOK) : Icons.categoryIcon(cat.icon), mx - 4, my - 4, 0.5f);
				if (Math.abs(mouseX - mx) < 6 && Math.abs(mouseY - my) < 6) label(g, "✎ " + t.text, mx, my - 14, 0xFFFFFFFF);
			}
		}
		// waypoints
		Config.Waypoint hoverWp = waypointAt(mouseX, mouseY);
		for (Config.Waypoint w : Waypoints.here()) {
			int mx = Math.round(sx(w.x + 0.5)), my = Math.round(sy(w.z + 0.5));
			float bob = (float) Math.sin(Theme.time() * 3 + w.x) * 1f;
			var icon = Waypoints.iconStack(w);
			if (!icon.isEmpty()) {
				float s = w == hoverWp ? 1f : 0.75f;
				g.fill(mx - 1, my - 2, mx + 2, my + 1, 0xFF000000);
				g.fill(mx, my - 1, mx + 1, my, w.color | 0xFF000000);
				Icons.scaled(g, icon, mx - 8 * s, my - 4 - 16 * s + bob, s);
			} else {
				pin(g, mx, my + bob, w.color | 0xFF000000, w == hoverWp, w.visible);
			}
			if (w == hoverWp || shownZoom >= 0.5f) label(g, w.name, mx, Math.round(my - 22 + bob), w.color | 0xFF000000);
		}
		// player
		var p = this.minecraft.player;
		if (p != null) drawPlayer(g, p, sx(p.getX()), sy(p.getZ()));
		g.disableScissor();

		// coordinates under the mouse + zoom
		if (mouseX >= l && mouseX < r && mouseY >= top && mouseY < bottom && editing == null) {
			int wx = (int) Math.floor(camX + (mouseX - mcx()) / shownZoom);
			int wz = (int) Math.floor(camZ + (mouseY - mcy()) / shownZoom);
			String s = "X " + wx + "  Z " + wz;
			g.fill(l + 3, bottom - 14, l + 7 + this.font.width(s), bottom - 3, 0xC0000000);
			g.text(this.font, s, l + 5, bottom - 12, 0xFFFFFFFF, false);
		}
		String zs = (shownZoom >= 1 ? Math.round(shownZoom) + ":1" : "1:" + Math.round(1 / shownZoom));
		g.text(this.font, "§7" + zs, r - 6 - this.font.width(zs), bottom - 12, 0xFFFFFFFF, false);
		if (WorldMap.tiles().isEmpty()) g.centeredText(this.font, "§7Walk around – the map draws itself", mcx(), mcy() + 24, 0xFFFFFFFF);

		drawList(g, mouseX, mouseY);
		if (editing != null) {
			drawEditor(g);
			for (int i = 0; i < Waypoints.ICONS.size(); i++) {
				int cx = iconCellX(i), cy = iconCellY(i);
				if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
					label(g, Waypoints.ICONS.get(i)[1], cx + 8, cy - 11, 0xFFFFFFFF);
				}
			}
		}
	}

	private void label(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		int w = this.font.width(text) + 6;
		g.fill(x - w / 2, y - 1, x + w / 2, y + 9, 0xC0101010);
		g.centeredText(this.font, text, x, y, color);
	}

	/** A map pin: colored head with a dark outline and a little point. */
	private static void pin(GuiGraphicsExtractor g, int x, float yf, int color, boolean hover, boolean visible) {
		int y = Math.round(yf);
		int r = hover ? 4 : 3;
		int c = visible ? color : Theme.alpha(color, 0.45f);
		g.fill(x - r - 1, y - 2 * r - 4, x + r + 2, y - 3, 0xFF000000);
		g.fill(x - r, y - 2 * r - 3, x + r + 1, y - 4, c);
		g.fill(x - r + 1, y - 2 * r - 3, x, y - 2 * r - 2, 0x60FFFFFF);
		for (int i = 0; i < 3; i++) g.fill(x - 2 + i, y - 4 + i, x + 3 - i, y - 3 + i, 0xFF000000);
		g.fill(x, y - 3, x + 1, y, c);
	}

	/** You: your own face with a direction arrow and a soft view cone. */
	private void drawPlayer(GuiGraphicsExtractor g, net.minecraft.client.player.LocalPlayer p, float x, float y) {
		float yaw = p.getYRot();
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate((float) Math.toRadians(yaw + 180f));
		// view cone
		for (int i = 0; i < 16; i++) {
			int w = 2 + i;
			g.fill(-w / 2, -8 - i, w / 2 + 1, -7 - i, Theme.alpha(0xFFFFFFFF, 0.18f * (1f - i / 16f)));
		}
		// arrow in front of the head
		g.fill(-3, -10, 4, -9, 0xFF000000);
		for (int i = 0; i < 4; i++) g.fill(-3 + i, -10 - i, 4 - i, -9 - i, i == 3 ? 0xFF000000 : 0xFFFFFFFF);
		g.pose().popMatrix();

		float pulse = 1f + 0.08f * Theme.pulse(4f);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(pulse, pulse);
		g.fill(-3, 5, 5, 6, 0x50000000); // soft shadow only
		try {
			var tex = p.getSkin().body().texturePath();
			g.blit(RenderPipelines.GUI_TEXTURED, tex, -4, -4, 8f, 8f, 8, 8, 8, 8, 64, 64);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, -4, -4, 40f, 8f, 8, 8, 8, 8, 64, 64);
		} catch (RuntimeException e) {
			g.fill(-4, -4, 4, 4, 0xFFFFFFFF);
		}
		g.pose().popMatrix();
	}

	private List<Config.Waypoint> sortedList() {
		List<Config.Waypoint> list = new java.util.ArrayList<>(Waypoints.here());
		var p = this.minecraft.player;
		if (p != null) list.sort(java.util.Comparator.comparingDouble(w -> Math.pow(w.x - p.getX(), 2) + Math.pow(w.z - p.getZ(), 2)));
		return list;
	}

	private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = mapRight() + 6, y = mapTop(), w = PANEL;
		List<Config.Waypoint> list = sortedList();
		g.text(this.font, "§7WAYPOINTS §8" + list.size(), x + 5, y + 5, 0xFFFFFFFF, false);
		if (list.isEmpty()) {
			g.text(this.font, "§8Right-click the map", x + 5, y + 20, 0xFFFFFFFF, false);
			g.text(this.font, "§8or press B in game", x + 5, y + 30, 0xFFFFFFFF, false);
		}
		int rows = (mapBottom() - y - 18) / ROW_H;
		listScroll = Math.max(0, Math.min(listScroll, Math.max(0, list.size() - rows)));
		var p = this.minecraft.player;
		for (int i = 0; i < rows && i + listScroll < list.size(); i++) {
			Config.Waypoint wp = list.get(i + listScroll);
			int ry = y + 16 + i * ROW_H;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H - 1 && editing == null;
			int col = wp.color | 0xFF000000;
			g.fill(x + 2, ry, x + w - 2, ry + ROW_H - 1, hover ? 0x50FFFFFF : 0x28000000);
			g.fill(x + 2, ry, x + 4, ry + ROW_H - 1, wp.visible ? col : Theme.alpha(col, 0.35f));
			var icon = Waypoints.iconStack(wp);
			if (!icon.isEmpty()) {
				Icons.scaled(g, icon, x + 7, ry + 3, 1f);
			} else {
				g.fill(x + 10, ry + 6, x + 18, ry + 14, 0xFF000000);
				g.fill(x + 11, ry + 7, x + 17, ry + 13, col);
			}
			String dist = "", arrow = "";
			if (p != null) {
				double dx = wp.x + 0.5 - p.getX(), dz = wp.z + 0.5 - p.getZ();
				dist = Math.round(Math.sqrt(dx * dx + dz * dz)) + "m";
				float rel = (float) Math.toDegrees(Math.atan2(-dx, dz)) - p.getYRot();
				int d = Math.floorMod(Math.round(rel / 45f), 8);
				arrow = new String[] {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[d] + " ";
			}
			int dw = this.font.width(arrow + dist);
			String name = this.font.plainSubstrByWidth(wp.name, w - 30 - dw - 4);
			g.text(this.font, (wp.visible ? "§f" : "§8") + name, x + 26, ry + 3, 0xFFFFFFFF, false);
			g.text(this.font, "§7" + arrow + dist, x + w - 5 - dw, ry + 3, 0xFFFFFFFF, false);
			g.text(this.font, "§8" + wp.x + " " + wp.y + " " + wp.z, x + 26, ry + 12, 0xFFFFFFFF, false);
			// eye toggle
			boolean eyeHover = hover && mouseX >= x + w - 14 && mouseY >= ry + 11;
			g.text(this.font, wp.visible ? (eyeHover ? "§f◉" : "§7◉") : "§8○", x + w - 12, ry + 12, 0xFFFFFFFF, false);
		}
		if (list.size() > rows) g.text(this.font, "§8scroll for more", x + 5, mapBottom() - 10, 0xFFFFFFFF, false);
	}

	private void drawEditor(GuiGraphicsExtractor g) {
		int x = edX(), y = edY();
		g.fill(0, 0, this.width, this.height, 0x70000000);
		Theme.panel(g, x, y, ED_W, ED_H);
		g.fill(x, y, x + ED_W, y + 2, editing.color | 0xFF000000);
		g.text(this.font, (editingNew ? "§l✚ New waypoint" : "§l✎ Edit waypoint"), x + 8, y + 7, 0xFFFFFFFF, false);
		String world = editing.world.contains("/") ? editing.world.substring(editing.world.lastIndexOf('/') + 1) : "";
		g.text(this.font, "§8" + world, x + ED_W - 8 - this.font.width(world), y + 7, 0xFFFFFFFF, false);
		int bw = (ED_W - 16 - 3 * 12 - 8) / 3;
		for (int i = 0; i < 3; i++) g.text(this.font, "§7" + "XYZ".charAt(i), x + 10 + i * (bw + 16), y + 43, 0xFFFFFFFF, false);
		// icon grid
		for (int i = 0; i < Waypoints.ICONS.size(); i++) {
			int cx = iconCellX(i), cy = iconCellY(i);
			String id = Waypoints.ICONS.get(i)[0];
			boolean sel = id.equals(editing.icon);
			g.fill(cx - 1, cy - 1, cx + 17, cy + 17, sel ? Theme.ACCENT : 0x30FFFFFF);
			g.fill(cx, cy, cx + 16, cy + 16, 0xFF202020);
			if (id.isEmpty()) {
				g.fill(cx + 5, cy + 5, cx + 11, cy + 11, editing.color | 0xFF000000);
			} else {
				g.item(Icons.stack(dev.vanillaclient.hud.InfoModules.Counter.item(id)), cx, cy);
			}
		}
		for (int i = 0; i < Waypoints.COLORS.length; i++) {
			int bx = x + 8 + i * 20;
			g.fill(bx + 3, y + 107, bx + 15, y + 117, Waypoints.COLORS[i]);
		}
	}

	// ------------------------------------------------------------ input

	private Config.Waypoint waypointAt(double mx, double my) {
		for (Config.Waypoint w : Waypoints.here()) {
			float x = sx(w.x + 0.5), y = sy(w.z + 0.5);
			if (Math.abs(mx - x) <= 7 && my <= y + 2 && my >= y - 18) return w;
		}
		return null;
	}

	static int[] parseCoords(String s) {
		if (s == null || s.isBlank()) return null;
		String[] parts = s.trim().replace(",", " ").split("\\s+");
		try {
			if (parts.length >= 3) return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[2])};
			if (parts.length == 2) return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
		} catch (NumberFormatException ignored) {
			// not coordinates
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x(), my = event.y();
		if (editing != null) {
			for (int i = 0; i < Waypoints.ICONS.size(); i++) {
				int cx = iconCellX(i), cy = iconCellY(i);
				if (mx >= cx && mx < cx + 16 && my >= cy && my < cy + 16) {
					Ui.click();
					editing.icon = Waypoints.ICONS.get(i)[0];
					Config.save();
					return true;
				}
			}
			return true; // modal
		}
		// list
		if (mx >= mapRight() + 6 && mx < mapRight() + 6 + PANEL && my >= mapTop() + 16) {
			int i = (int) ((my - mapTop() - 16) / ROW_H) + listScroll;
			List<Config.Waypoint> list = sortedList();
			if (i >= 0 && i < list.size()) {
				Config.Waypoint w = list.get(i);
				Ui.click();
				int ry = mapTop() + 16 + (i - listScroll) * ROW_H;
				if (mx >= mapRight() + 6 + PANEL - 14 && my >= ry + 11) {
					w.visible = !w.visible;
					Config.save();
				} else if (event.button() == Mouse.RIGHT || doubleClick) {
					openEditor(w, false);
				} else {
					camX = w.x + 0.5;
					camZ = w.z + 0.5;
					follow = false;
				}
				return true;
			}
		}
		if (mx >= mapLeft() && mx < mapRight() && my >= mapTop() && my < mapBottom()) {
			Config.Waypoint w = waypointAt(mx, my);
			if (w != null && event.button() == Mouse.LEFT) {
				// click = edit, drag = move it
				draggingWp = w;
				wpMoved = false;
				downX = mx;
				downY = my;
				return true;
			}
			if (event.button() == Mouse.RIGHT) {
				int wx = (int) Math.floor(camX + (mx - mcx()) / shownZoom);
				int wz = (int) Math.floor(camZ + (my - mcy()) / shownZoom);
				int wy = this.minecraft.player != null ? this.minecraft.player.getBlockY() : 64;
				Config.Waypoint nw = Waypoints.add("Waypoint " + (Waypoints.here().size() + 1), wx, wy, wz);
				if (nw != null) {
					Ui.click();
					openEditor(nw, true);
				}
				return true;
			}
			dragging = true;
			moved = false;
			lastX = downX = mx;
			lastY = downY = my;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		if (draggingWp != null) {
			Config.Waypoint w = draggingWp;
			draggingWp = null;
			if (wpMoved) {
				Config.save();
			} else {
				Ui.click();
				openEditor(w, false);
			}
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (editing != null) return true;
		if (mx >= mapRight()) {
			listScroll = Math.max(0, listScroll + (v > 0 ? -1 : 1));
			return true;
		}
		float old = zoom;
		zoom = Config.clamp(zoom * (v > 0 ? 1.25f : 0.8f), 0.0625f, 8f);
		double wx = camX + (mx - mcx()) / old;
		double wz = camZ + (my - mcy()) / old;
		camX = wx - (mx - mcx()) / zoom;
		camZ = wz - (my - mcy()) / zoom;
		shownZoom = zoom;
		follow = false;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editing != null && event.isConfirmation()) {
			closeEditor();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (editing != null) {
			Config.save();
			editing = null;
			rebuildWidgets();
			return;
		}
		WorldMap.saveAll();
		super.onClose();
	}
}
