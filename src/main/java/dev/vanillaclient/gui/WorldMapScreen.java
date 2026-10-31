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
		return this.width / 2 - 90;
	}

	private int edY() {
		return this.height / 2 - 50;
	}

	private void openEditor(Config.Waypoint w, boolean isNew) {
		editing = w;
		editingNew = isNew;
		rebuildWidgets();
	}

	private void editorWidgets() {
		int x = edX(), y = edY();
		nameBox = new EditBox(this.font, x + 8, y + 22, 164, 18, Component.literal("Name"));
		nameBox.setMaxLength(32);
		nameBox.setValue(editing.name);
		nameBox.setResponder(v -> editing.name = v);
		addRenderableWidget(nameBox);
		setFocused(nameBox);
		for (int i = 0; i < Waypoints.COLORS.length; i++) {
			final int c = Waypoints.COLORS[i];
			Button b = action(x + 8 + i * 21, y + 46, 19, "", () -> {
				editing.color = c;
				Config.save();
			});
			b.setMessage(Component.literal(editing.color == c ? "§l✔" : ""));
		}
		action(x + 8, y + 70, 52, editing.visible ? "§aShown" : "§7Hidden", () -> {
			editing.visible = !editing.visible;
			Config.save();
			rebuildWidgets();
		});
		action(x + 64, y + 70, 52, "§cDelete", () -> {
			Config.get().waypoints.remove(editing);
			Config.save();
			editing = null;
			rebuildWidgets();
		});
		action(x + 120, y + 70, 52, "Save", () -> {
			if (editing.name.isBlank()) editing.name = "Waypoint";
			Config.save();
			editing = null;
			rebuildWidgets();
		});
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
		g.centeredText(this.font, "§7Drag · scroll to zoom · right-click to set a waypoint", this.width / 2, 19, 0xFFFFFFFF);

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
			pin(g, mx, my + bob, w.color | 0xFF000000, w == hoverWp, w.visible);
			if (w == hoverWp || shownZoom >= 0.5f) label(g, w.name, mx, Math.round(my - 18 + bob), w.color | 0xFF000000);
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
		if (editing != null) drawEditor(g);
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
		g.fill(-5, -5, 5, 5, 0xFF000000);
		g.fill(-6, -4, 6, 4, 0xFF000000);
		g.fill(-4, -6, 4, 6, 0xFF000000);
		try {
			var tex = p.getSkin().body().texturePath();
			g.blit(RenderPipelines.GUI_TEXTURED, tex, -4, -4, 8f, 8f, 8, 8, 8, 8, 64, 64);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, -4, -4, 40f, 8f, 8, 8, 8, 8, 64, 64);
		} catch (RuntimeException e) {
			g.fill(-4, -4, 4, 4, 0xFFFFFFFF);
		}
		g.pose().popMatrix();
	}

	private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = mapRight() + 6, y = mapTop(), w = PANEL;
		g.text(this.font, "§7WAYPOINTS", x + 5, y + 5, 0xFFFFFFFF, false);
		List<Config.Waypoint> list = Waypoints.here();
		if (list.isEmpty()) {
			g.text(this.font, "§8Right-click the map", x + 5, y + 20, 0xFFFFFFFF, false);
			g.text(this.font, "§8or press B in game", x + 5, y + 30, 0xFFFFFFFF, false);
		}
		int rows = (mapBottom() - y - 18) / 14;
		listScroll = Math.max(0, Math.min(listScroll, Math.max(0, list.size() - rows)));
		var p = this.minecraft.player;
		for (int i = 0; i < rows && i + listScroll < list.size(); i++) {
			Config.Waypoint wp = list.get(i + listScroll);
			int ry = y + 16 + i * 14;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 14;
			if (hover) g.fill(x + 2, ry, x + w - 2, ry + 13, Theme.ROW_HOVER);
			g.fill(x + 5, ry + 3, x + 11, ry + 9, wp.color | 0xFF000000);
			g.outline(x + 4, ry + 2, 8, 8, 0xFF000000);
			String dist = "";
			if (p != null) dist = Math.round(Math.sqrt(Math.pow(wp.x - p.getX(), 2) + Math.pow(wp.z - p.getZ(), 2))) + "m";
			int dw = this.font.width(dist);
			String name = this.font.plainSubstrByWidth(wp.name, w - 22 - dw - 4);
			g.text(this.font, (wp.visible ? "§f" : "§8") + name, x + 15, ry + 3, 0xFFFFFFFF, false);
			g.text(this.font, "§7" + dist, x + w - 5 - dw, ry + 3, 0xFFFFFFFF, false);
		}
	}

	private void drawEditor(GuiGraphicsExtractor g) {
		int x = edX(), y = edY();
		g.fill(0, 0, this.width, this.height, 0x60000000);
		Theme.panel(g, x, y, 180, 96);
		g.fill(x, y, x + 180, y + 2, editing.color | 0xFF000000);
		g.text(this.font, (editingNew ? "§lNew waypoint" : "§lEdit waypoint"), x + 8, y + 8, 0xFFFFFFFF, false);
		String pos = "§7" + editing.x + " " + editing.y + " " + editing.z;
		g.text(this.font, pos, x + 172 - this.font.width(pos), y + 8, 0xFFFFFFFF, false);
		for (int i = 0; i < Waypoints.COLORS.length; i++) {
			int bx = x + 8 + i * 21;
			g.fill(bx + 3, y + 49, bx + 16, y + 63, Waypoints.COLORS[i]);
		}
	}

	// ------------------------------------------------------------ input

	private Config.Waypoint waypointAt(double mx, double my) {
		for (Config.Waypoint w : Waypoints.here()) {
			float x = sx(w.x + 0.5), y = sy(w.z + 0.5);
			if (Math.abs(mx - x) <= 6 && my <= y && my >= y - 14) return w;
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
		if (editing != null) return true; // modal
		double mx = event.x(), my = event.y();
		// list
		if (mx >= mapRight() + 6 && mx < mapRight() + 6 + PANEL && my >= mapTop() + 16) {
			int i = (int) ((my - mapTop() - 16) / 14) + listScroll;
			List<Config.Waypoint> list = Waypoints.here();
			if (i >= 0 && i < list.size()) {
				Config.Waypoint w = list.get(i);
				Ui.click();
				if (event.button() == Mouse.RIGHT || doubleClick) {
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
				Ui.click();
				openEditor(w, false);
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
			if (editing.name.isBlank()) editing.name = "Waypoint";
			Config.save();
			editing = null;
			rebuildWidgets();
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
