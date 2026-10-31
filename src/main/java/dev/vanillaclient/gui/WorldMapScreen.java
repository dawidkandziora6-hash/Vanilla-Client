package dev.vanillaclient.gui;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.map.WorldMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Map of everything you explored. Drag to move, scroll to zoom,
 * tasks with coordinates show up as markers.
 */
public final class WorldMapScreen extends BaseScreen {
	private static float zoom = 1f;

	private double camX, camZ;
	private boolean dragging, follow = true;
	private double lastX, lastY;
	private float shownZoom;

	public WorldMapScreen(Screen parent) {
		super(Component.literal("World Map"), parent);
		var p = net.minecraft.client.Minecraft.getInstance().player;
		if (p != null) {
			camX = p.getX();
			camZ = p.getZ();
			WorldMap.loadAround(p.getBlockX(), p.getBlockZ());
		}
		for (int[] t : WorldMap.savedTiles()) WorldMap.tile(t[0], t[1], false);
		shownZoom = zoom;
	}

	@Override
	protected void init() {
		super.init();
		int y = this.height - 26;
		action(this.width / 2 - 154, y, 100, "⌖ Center on me", () -> follow = true);
		action(this.width / 2 - 50, y, 100, "Map: " + (Config.get().worldMap.enabled ? "§aRecording" : "§7Paused"), () -> {
			Config.get().worldMap.enabled = !Config.get().worldMap.enabled;
			Config.save();
			rebuildWidgets();
		});
		action(this.width / 2 + 54, y, 100, "✔ Done", this::onClose);
	}

	private int mapTop() {
		return 30;
	}

	private int mapBottom() {
		return this.height - 32;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		var p = this.minecraft.player;
		if (dragging) {
			camX -= (mouseX - lastX) / shownZoom;
			camZ -= (mouseY - lastY) / shownZoom;
			lastX = mouseX;
			lastY = mouseY;
			follow = false;
		}
		if (follow && p != null) {
			camX += (p.getX() - camX) * 0.3;
			camZ += (p.getZ() - camZ) * 0.3;
		}
		shownZoom += (zoom - shownZoom) * 0.25f;
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int top = mapTop(), bottom = mapBottom();
		g.fill(8, top, this.width - 8, bottom, 0xFF151515);
		g.enableScissor(8, top, this.width - 8, bottom);
		int cx = this.width / 2, cy = (top + bottom) / 2;
		float z = shownZoom;
		for (WorldMap.Tile t : WorldMap.tiles()) {
			float sx = (float) (cx + (t.tx * WorldMap.TILE - camX) * z);
			float sy = (float) (cy + (t.tz * WorldMap.TILE - camZ) * z);
			float size = WorldMap.TILE * z;
			if (sx > this.width || sy > bottom || sx + size < 0 || sy + size < top) continue;
			g.pose().pushMatrix();
			g.pose().translate(sx, sy);
			g.pose().scale(z, z);
			g.blit(RenderPipelines.GUI_TEXTURED, t.texture(), 0, 0, 0f, 0f, WorldMap.TILE, WorldMap.TILE, WorldMap.TILE, WorldMap.TILE);
			g.pose().popMatrix();
		}
		g.disableScissor();
		g.outline(8, top, this.width - 16, bottom - top, 0x60FFFFFF);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int top = mapTop(), bottom = mapBottom();
		int cx = this.width / 2, cy = (top + bottom) / 2;
		float z = shownZoom;
		g.enableScissor(8, top, this.width - 8, bottom);

		// task markers
		if (Config.get().worldMap.showTasks) {
			for (Config.TodoItem t : Config.get().todos) {
				int[] c = parseCoords(t.coords);
				if (c == null) continue;
				int mx = Math.round((float) (cx + (c[0] + 0.5 - camX) * z));
				int my = Math.round((float) (cy + (c[1] + 0.5 - camZ) * z));
				Config.Category cat = Config.get().category(t.category);
				float bob = (float) Math.sin(Theme.time() * 3 + c[0]) * 1.5f;
				g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000);
				g.fill(mx, my, mx + 1, my + 1, 0xFFFFFFFF);
				Icons.scaled(g, cat == null ? Icons.stack(Items.BOOK) : Icons.categoryIcon(cat.icon), mx - 6, my - 15 + bob, 0.75f);
				if (Math.abs(mouseX - mx) < 8 && Math.abs(mouseY - my + 8) < 10) {
					int w = this.font.width(t.text) + 6;
					g.fill(mx - w / 2, my - 28, mx + w / 2, my - 17, 0xE0101010);
					g.centeredText(this.font, t.text, mx, my - 26, 0xFFFFFFFF);
				}
			}
		}

		// player arrow
		var p = this.minecraft.player;
		if (p != null) {
			float px = (float) (cx + (p.getX() - camX) * z);
			float py = (float) (cy + (p.getZ() - camZ) * z);
			g.pose().pushMatrix();
			g.pose().translate(px, py);
			g.pose().rotate((float) Math.toRadians(p.getYRot() + 180f));
			float pulse = 1f + 0.15f * Theme.pulse(4f);
			g.pose().scale(pulse, pulse);
			g.fill(-1, -5, 2, 4, 0xFF000000);
			for (int i = 0; i < 4; i++) g.fill(-i - 1, -3 + i, i + 2, -2 + i, 0xFF000000);
			g.fill(0, -4, 1, 3, 0xFFFFFFFF);
			for (int i = 0; i < 3; i++) g.fill(-i, -3 + i, i + 1, -2 + i, 0xFFFFFFFF);
			g.pose().popMatrix();
		}
		g.disableScissor();

		// header
		float in = intro();
		g.centeredText(this.font, "§l⌖ World Map", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7Drag to move · Scroll to zoom · Grows while you explore", this.width / 2, 19, 0xFFFFFFFF);

		// coordinates under the mouse
		if (mouseY >= top && mouseY < bottom) {
			int wx = (int) Math.floor(camX + (mouseX - cx) / z);
			int wz = (int) Math.floor(camZ + (mouseY - cy) / z);
			String s = "X " + wx + "  Z " + wz;
			g.fill(12, bottom - 14, 16 + this.font.width(s), bottom - 3, 0xC0000000);
			g.text(this.font, s, 14, bottom - 12, 0xFFFFFFFF, false);
		}
		String zs = Math.round(zoom * 100) + "%";
		g.text(this.font, "§7" + zs, this.width - 14 - this.font.width(zs), bottom - 12, 0xFFFFFFFF, false);
		if (WorldMap.tiles().isEmpty()) {
			g.centeredText(this.font, "§7Walk around – the map draws itself", cx, cy + 20, 0xFFFFFFFF);
		}
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
		if (event.y() >= mapTop() && event.y() < mapBottom()) {
			dragging = true;
			lastX = event.x();
			lastY = event.y();
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
		float old = zoom;
		zoom = Config.clamp(zoom * (v > 0 ? 1.25f : 0.8f), 0.125f, 8f);
		// zoom towards the mouse
		int cx = this.width / 2, cy = (mapTop() + mapBottom()) / 2;
		double wx = camX + (mx - cx) / old;
		double wz = camZ + (my - cy) / old;
		camX = wx - (mx - cx) / zoom;
		camZ = wz - (my - cy) / zoom;
		shownZoom = zoom;
		follow = false;
		return true;
	}

	@Override
	public void onClose() {
		WorldMap.saveAll();
		super.onClose();
	}
}
