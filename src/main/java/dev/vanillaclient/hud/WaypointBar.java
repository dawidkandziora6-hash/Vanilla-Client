package dev.vanillaclient.hud;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Theme;
import dev.vanillaclient.map.Waypoints;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Waypoints like the vanilla locator bar: colored markers on the XP bar
 * (or a compass bar at the top) that show where your waypoints are.
 */
public final class WaypointBar {
	private static final String[] CARDINAL = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private WaypointBar() {}

	private static float wrap(float deg) {
		deg %= 360f;
		if (deg >= 180f) deg -= 360f;
		if (deg < -180f) deg += 360f;
		return deg;
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		Config.WaypointHud cfg = Config.get().waypointHud;
		if (p == null || cfg.mode == 0 || mc.gui.screen() instanceof dev.vanillaclient.gui.HudEditorScreen) return;
		List<Config.Waypoint> list = Waypoints.here();
		if (cfg.mode == 1 && list.isEmpty()) return;
		if (cfg.mode == 3) {
			topMarkers(g, mc, p, list, cfg);
			return;
		}
		Font font = mc.font;
		int sw = g.guiWidth(), sh = g.guiHeight();
		int cx = sw / 2;
		float yaw = p.getYRot();
		boolean top = cfg.mode == 2;
		int barY = top ? 6 : sh - 27;
		float fov = top ? 90f : 60f;
		int half = top ? 91 : 88;

		if (top) {
			g.fill(cx - 93, barY - 5, cx + 93, barY + 6, 0x80000000);
			g.outline(cx - 93, barY - 5, 186, 11, 0x40FFFFFF);
			for (int i = 0; i < 8; i++) {
				float rel = wrap(i * 45f - yaw);
				if (Math.abs(rel) > fov) continue;
				int x = cx + Math.round(rel / fov * half);
				boolean main = i % 2 == 0;
				String s = CARDINAL[i];
				int col = s.equals("N") ? 0xFFFF5555 : main ? 0xFFFFFFFF : 0xFFA0A0A0;
				g.centeredText(font, s, x, barY - 3, col);
			}
			for (int a = 0; a < 360; a += 15) {
				float rel = wrap(a - yaw);
				if (Math.abs(rel) > fov || a % 45 == 0) continue;
				int x = cx + Math.round(rel / fov * half);
				g.fill(x, barY + 2, x + 1, barY + 4, 0x60FFFFFF);
			}
			g.fill(cx, barY - 6, cx + 1, barY - 4, Theme.ACCENT);
		}

		Config.Waypoint focused = null;
		float best = 999f;
		for (Config.Waypoint w : list) {
			if (!w.visible) continue;
			double dx = w.x + 0.5 - p.getX();
			double dz = w.z + 0.5 - p.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (cfg.maxDistance > 0 && dist > cfg.maxDistance) continue;
			float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float rel = wrap(target - yaw);
			int col = w.color | 0xFF000000;
			if (Math.abs(rel) <= fov) {
				int x = cx + Math.round(rel / fov * half);
				int y = top ? barY + 1 : barY;
				// diamond marker with outline, a bit smaller when far away
				int r = dist < 64 ? 3 : 2;
				net.minecraft.world.item.ItemStack icon = dev.vanillaclient.map.Waypoints.iconStack(w);
				if (top && !icon.isEmpty()) {
					dev.vanillaclient.gui.Icons.scaled(g, icon, x - 4, y - 4, 0.5f);
				} else {
					diamond(g, x, y, r + 1, 0xFF000000);
					diamond(g, x, y, r, col);
				}
				double dy = w.y - p.getY();
				if (dy > 6) g.text(font, "▴", x - 2, y - r - 8, col, true);
				else if (dy < -6) g.text(font, "▾", x - 2, y + r, col, true);
				if (Math.abs(rel) < best) {
					best = Math.abs(rel);
					focused = w;
				}
			} else {
				// off to the side: small arrow at the end of the bar
				boolean right = rel > 0;
				int x = right ? cx + half + 3 : cx - half - 7;
				g.text(font, right ? "▸" : "◂", x, (top ? barY + 1 : barY) - 4, Theme.alpha(col, 0.8f), true);
			}
		}
		if (focused != null && best < 7f) {
			double dx = focused.x + 0.5 - p.getX(), dz = focused.z + 0.5 - p.getZ();
			int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
			String label = focused.name + (cfg.showDistance ? " §7" + dist + "m" : "");
			// XP-bar mode: above hearts, armor, hunger and air – never on top of them
			int ly = top ? barY + 9 : statusTop(p, sh) - 12;
			net.minecraft.world.item.ItemStack icon = dev.vanillaclient.map.Waypoints.iconStack(focused);
			int iw = icon.isEmpty() ? 0 : 10;
			int w = font.width(label) + 8 + iw;
			int lx = cx - w / 2;
			g.fill(lx, ly - 2, lx + w, ly + 9, 0xA0000000);
			g.fill(lx, ly + 9, lx + w, ly + 10, focused.color | 0xFF000000);
			if (iw > 0) dev.vanillaclient.gui.Icons.scaled(g, icon, lx + 3, ly - 1, 0.5f);
			g.text(font, label, lx + 4 + iw, ly, focused.color | 0xFF000000, true);
		}
	}

	/**
	 * Markers at the top edge, exactly above the direction of each waypoint
	 * (no bar, no compass). Turn around and they slide into view.
	 */
	private static void topMarkers(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, List<Config.Waypoint> list, Config.WaypointHud cfg) {
		Font font = mc.font;
		int sw = g.guiWidth(), sh = g.guiHeight();
		int cx = sw / 2;
		double vfov = Math.toRadians(mc.options.fov().get());
		double zoom = Math.max(1.0, dev.vanillaclient.zoom.Zoom.currentFactor());
		double tanH = Math.tan(vfov / 2) * sw / (double) sh / zoom;
		float yaw = p.getYRot();
		java.util.List<Object[]> shown = new java.util.ArrayList<>();
		for (Config.Waypoint w : list) {
			if (!w.visible) continue;
			double dx = w.x + 0.5 - p.getX(), dz = w.z + 0.5 - p.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (cfg.maxDistance > 0 && dist > cfg.maxDistance) continue;
			float rel = wrap((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
			shown.add(new Object[] {w, dist, rel});
		}
		shown.sort((a, b) -> Double.compare((double) b[1], (double) a[1])); // far ones first, near ones on top
		for (Object[] o : shown) {
			Config.Waypoint w = (Config.Waypoint) o[0];
			double dist = (double) o[1];
			float rel = (float) o[2];
			int col = w.color | 0xFF000000;
			double sxD = Math.abs(rel) < 89 ? cx + Math.tan(Math.toRadians(rel)) / tanH * (sw / 2.0) : (rel > 0 ? sw * 4 : -sw * 4);
			boolean off = sxD < 10 || sxD > sw - 10;
			int x = (int) Math.round(Math.max(10, Math.min(sw - 10, sxD)));
			int y = 6;
			if (off) {
				// at the screen edge: small arrow pointing where to turn
				g.text(font, sxD < 10 ? "◂" : "▸", sxD < 10 ? 2 : sw - 8, y, Theme.alpha(col, 0.85f), true);
				continue;
			}
			net.minecraft.world.item.ItemStack icon = Waypoints.iconStack(w);
			if (!icon.isEmpty()) {
				g.fill(x - 6, y - 2, x + 6, y + 10, 0x70000000);
				g.fill(x - 6, y + 10, x + 6, y + 11, col);
				dev.vanillaclient.gui.Icons.scaled(g, icon, x - 5, y - 1, 0.625f);
			} else {
				diamond(g, x, y + 4, 5, 0xFF000000);
				diamond(g, x, y + 4, 4, col);
			}
			double dy = w.y - p.getY();
			String arrow = dy > 6 ? " ▴" : dy < -6 ? " ▾" : "";
			boolean focus = Math.abs(x - cx) < 30;
			String dtext = (cfg.showDistance ? (int) Math.round(dist) + "m" : "") + arrow;
			if (focus || cfg.topNames) {
				String label = w.name + (dtext.isEmpty() ? "" : " §7" + dtext);
				int lw = font.width(label);
				g.fill(x - lw / 2 - 3, y + 13, x + lw / 2 + 3, y + 24, 0x90000000);
				g.text(font, label, x - lw / 2, y + 15, col, true);
			} else if (!dtext.isEmpty()) {
				g.pose().pushMatrix();
				g.pose().translate(x, y + 14);
				g.pose().scale(0.75f, 0.75f);
				g.centeredText(font, "§7" + dtext, 0, 0, 0xFFFFFFFF);
				g.pose().popMatrix();
			}
		}
	}

	/** Top edge of the vanilla status bars (hearts / armor on the left, food / air on the right). */
	private static int statusTop(LocalPlayer p, int sh) {
		if (p.isCreative() || p.isSpectator()) return sh - 32;
		float max = p.getMaxHealth() + p.getAbsorptionAmount();
		int rows = Math.max(1, (int) Math.ceil(max / 20f));
		int spacing = Math.max(10 - (rows - 2), 3);
		int left = sh - 39 - (rows - 1) * spacing - (p.getArmorValue() > 0 ? 10 : 0);
		int right = sh - 39 - (p.getAirSupply() < p.getMaxAirSupply() || p.isUnderWater() ? 10 : 0);
		if (p.getVehicle() instanceof net.minecraft.world.entity.LivingEntity) right -= 10;
		return Math.min(left, right);
	}

	private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int w = r - Math.abs(i);
			g.fill(x - w, y + i, x + w + 1, y + i + 1, color);
		}
	}
}
